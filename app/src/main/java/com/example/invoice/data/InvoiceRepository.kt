package com.example.invoice.data

import android.content.Context
import com.example.invoice.data.dao.InvoiceDao
import com.example.invoice.data.model.InvoiceDraftItem
import com.example.invoice.data.model.InvoiceItemRecord
import com.example.invoice.data.model.InvoiceRecord
import com.example.invoice.data.model.InvoiceWithItems
import com.example.receipt.data.ReceiptDatabase
import com.example.receipt.data.model.ReceiptProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InvoiceRepository(
    private val context: Context,
    private val invoiceDao: InvoiceDao,
    private val coroutineScope: CoroutineScope
) {
    // Read-only access to products master from Receipt Database
    private val receiptProductDao = ReceiptDatabase.getDatabase(context, coroutineScope).receiptProductDao()

    private val prefs = context.getSharedPreferences("invoice_preferences", Context.MODE_PRIVATE)

    fun getLastSavedComments(defaultComments: String): String {
        return prefs.getString("last_invoice_comments", defaultComments) ?: defaultComments
    }

    fun saveLastComments(comments: String) {
        val trimmed = comments.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("last_invoice_comments", trimmed).apply()
        }
    }

    fun getAllInvoices(): Flow<List<InvoiceWithItems>> = invoiceDao.getAllInvoices()

    fun searchInvoices(query: String): Flow<List<InvoiceWithItems>> {
        return if (query.isBlank()) {
            invoiceDao.getAllInvoices()
        } else {
            invoiceDao.searchInvoices(query.trim())
        }
    }

    /**
     * Strictly READ-ONLY flow of catalog products from the receipt database.
     * Guaranteed never to modify receipt_products from the invoice module.
     */
    fun getAvailableCatalogProducts(): Flow<List<ReceiptProduct>> = receiptProductDao.getAllProducts()

    /**
     * Generate next sequential invoice number in the format:
     * INV/NDP/<Year>/<Month>/<Day>/<6-digit-sequence>
     * e.g. INV/NDP/2026/11/6/002513
     */
    suspend fun getNextInvoiceNumber(): String = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val latest = invoiceDao.getLatestInvoice()
        val nextSeq = if (latest != null) {
            val trailingDigits = Regex("""(\d{4,8})$""").find(latest.invoiceNumber)?.value?.toIntOrNull()
            if (trailingDigits != null) trailingDigits + 1 else 2513
        } else {
            2513 // Matches user's exact sample baseline: INV/NDP/2026/11/6/002513
        }

        val seqFormatted = String.format("%06d", nextSeq)
        "INV/NDP/$year/$month/$day/$seqFormatted"
    }

    suspend fun isInvoiceNumberExists(number: String): Boolean = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceByNumber(number.trim()) != null
    }

    suspend fun saveInvoice(
        invoiceNumber: String,
        dateFormatted: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        draftItems: List<InvoiceDraftItem>,
        discount: Double,
        tax: Double,
        shippingAndHandling: Double,
        other: Double,
        comments: String,
        pdfFilePath: String? = null
    ): InvoiceWithItems = withContext(Dispatchers.IO) {
        val trimmedNumber = invoiceNumber.trim()
        if (trimmedNumber.isBlank()) {
            throw IllegalArgumentException("Nomor invoice tidak boleh kosong.")
        }
        if (invoiceDao.getInvoiceByNumber(trimmedNumber) != null) {
            throw IllegalStateException("Nomor invoice '$trimmedNumber' sudah pernah digunakan! Harap gunakan nomor yang berbeda.")
        }
        if (draftItems.isEmpty()) {
            throw IllegalArgumentException("Invoice harus memiliki minimal satu baris item produk.")
        }

        val subtotal = draftItems.sumOf { it.quantity * it.unitPrice }
        val itemsTax = draftItems.sumOf { it.tax }
        val effectiveTax = if (tax > 0.0) tax else itemsTax
        val total = maxOf(0.0, subtotal - discount + effectiveTax + shippingAndHandling + other)

        val invoiceRecord = InvoiceRecord(
            invoiceNumber = trimmedNumber,
            dateEpochMs = System.currentTimeMillis(),
            dateFormatted = dateFormatted.ifBlank {
                val cal = Calendar.getInstance()
                "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
            },
            customerName = customerName.trim().ifBlank { "Pelanggan Umum" },
            customerPhone = customerPhone.trim(),
            customerAddress = customerAddress.trim(),
            subtotal = subtotal,
            discount = discount,
            tax = effectiveTax,
            shippingAndHandling = shippingAndHandling,
            other = other,
            totalAmount = total,
            comments = comments.trim(),
            pdfFilePath = pdfFilePath
        )

        val invoiceId = invoiceDao.insertInvoice(invoiceRecord)

        val itemRecords = draftItems.mapIndexed { index, draft ->
            InvoiceItemRecord(
                invoiceId = invoiceId,
                itemNumber = index + 1,
                sku = draft.sku.ifBlank { "ITEM-${index + 1}" },
                description = draft.description.ifBlank { "Produk Percetakan #${index + 1}" },
                quantity = maxOf(1, draft.quantity),
                unitPrice = draft.unitPrice,
                tax = draft.tax,
                lineTotal = (maxOf(1, draft.quantity) * draft.unitPrice) + draft.tax
            )
        }

        invoiceDao.insertItems(itemRecords)

        saveLastComments(comments)

        InvoiceWithItems(
            invoice = invoiceRecord.copy(id = invoiceId),
            items = itemRecords
        )
    }

    suspend fun deleteInvoice(invoice: InvoiceRecord) = withContext(Dispatchers.IO) {
        invoiceDao.deleteInvoice(invoice)
    }

    // --- Export & Import All Saved Invoices ---

    suspend fun exportAllInvoicesJson(): String = withContext(Dispatchers.IO) {
        val all = invoiceDao.getAllInvoicesSync()
        val root = JSONObject()
        root.put("appName", "Diprint Accounting - Invoice")
        root.put("version", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID")).format(Date()))
        root.put("totalInvoices", all.size)

        val invoicesArray = JSONArray()
        for (item in all) {
            val iObj = JSONObject()
            val inv = item.invoice
            iObj.put("invoiceNumber", inv.invoiceNumber)
            iObj.put("dateEpochMs", inv.dateEpochMs)
            iObj.put("dateFormatted", inv.dateFormatted)
            iObj.put("customerName", inv.customerName)
            iObj.put("customerPhone", inv.customerPhone)
            iObj.put("customerAddress", inv.customerAddress)
            iObj.put("subtotal", inv.subtotal)
            iObj.put("discount", inv.discount)
            iObj.put("tax", inv.tax)
            iObj.put("shippingAndHandling", inv.shippingAndHandling)
            iObj.put("other", inv.other)
            iObj.put("totalAmount", inv.totalAmount)
            iObj.put("comments", inv.comments)
            iObj.put("createdAt", inv.createdAt)

            val itemsArray = JSONArray()
            for (line in item.items) {
                val itObj = JSONObject()
                itObj.put("itemNumber", line.itemNumber)
                itObj.put("sku", line.sku)
                itObj.put("description", line.description)
                itObj.put("quantity", line.quantity)
                itObj.put("unitPrice", line.unitPrice)
                itObj.put("tax", line.tax)
                itObj.put("lineTotal", line.lineTotal)
                itemsArray.put(itObj)
            }
            iObj.put("items", itemsArray)
            invoicesArray.put(iObj)
        }
        root.put("invoices", invoicesArray)
        root.toString(2)
    }

    suspend fun importInvoicesFromJson(jsonText: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonText.trim())
            val invoicesArray = root.optJSONArray("invoices")
                ?: return@withContext Result.failure(IllegalArgumentException("Format data tidak valid: atribut 'invoices' tidak ditemukan."))

            var count = 0
            for (i in 0 until invoicesArray.length()) {
                val iObj = invoicesArray.getJSONObject(i)
                val invoiceNumber = iObj.getString("invoiceNumber")
                val dateEpochMs = iObj.optLong("dateEpochMs", System.currentTimeMillis())
                val dateFormatted = iObj.optString("dateFormatted", "")
                val customerName = iObj.optString("customerName", "Pelanggan Umum")
                val customerPhone = iObj.optString("customerPhone", "")
                val customerAddress = iObj.optString("customerAddress", "")
                val subtotal = iObj.optDouble("subtotal", 0.0)
                val discount = iObj.optDouble("discount", 0.0)
                val tax = iObj.optDouble("tax", 0.0)
                val shippingAndHandling = iObj.optDouble("shippingAndHandling", 0.0)
                val other = iObj.optDouble("other", 0.0)
                val totalAmount = iObj.optDouble("totalAmount", 0.0)
                val comments = iObj.optString("comments", "")
                val createdAt = iObj.optLong("createdAt", System.currentTimeMillis())

                // Check existing invoice with same number
                val existing = invoiceDao.getInvoiceByNumber(invoiceNumber)
                val invoiceId = if (existing != null) {
                    invoiceDao.deleteItemsByInvoiceId(existing.id)
                    invoiceDao.insertInvoiceReplace(
                        existing.copy(
                            invoiceNumber = invoiceNumber,
                            dateEpochMs = dateEpochMs,
                            dateFormatted = dateFormatted,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            customerAddress = customerAddress,
                            subtotal = subtotal,
                            discount = discount,
                            tax = tax,
                            shippingAndHandling = shippingAndHandling,
                            other = other,
                            totalAmount = totalAmount,
                            comments = comments,
                            createdAt = createdAt
                        )
                    )
                } else {
                    invoiceDao.insertInvoiceReplace(
                        InvoiceRecord(
                            invoiceNumber = invoiceNumber,
                            dateEpochMs = dateEpochMs,
                            dateFormatted = dateFormatted,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            customerAddress = customerAddress,
                            subtotal = subtotal,
                            discount = discount,
                            tax = tax,
                            shippingAndHandling = shippingAndHandling,
                            other = other,
                            totalAmount = totalAmount,
                            comments = comments,
                            createdAt = createdAt
                        )
                    )
                }

                val itemsArray = iObj.optJSONArray("items")
                if (itemsArray != null) {
                    val itemRecords = mutableListOf<InvoiceItemRecord>()
                    for (j in 0 until itemsArray.length()) {
                        val itObj = itemsArray.getJSONObject(j)
                        itemRecords.add(
                            InvoiceItemRecord(
                                invoiceId = invoiceId,
                                itemNumber = itObj.optInt("itemNumber", j + 1),
                                sku = itObj.optString("sku", "ITEM-${j + 1}"),
                                description = itObj.optString("description", "Produk #${j + 1}"),
                                quantity = itObj.optInt("quantity", 1),
                                unitPrice = itObj.optDouble("unitPrice", 0.0),
                                tax = itObj.optDouble("tax", 0.0),
                                lineTotal = itObj.optDouble("lineTotal", 0.0)
                            )
                        )
                    }
                    invoiceDao.insertItems(itemRecords)
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
