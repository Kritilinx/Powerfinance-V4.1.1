package com.example.receipt.data

import com.example.receipt.data.model.ReceiptCartItem
import com.example.receipt.data.model.ReceiptItemSnapshot
import com.example.receipt.data.model.ReceiptProduct
import com.example.receipt.data.model.ReceiptRecord
import com.example.receipt.data.model.ReceiptStoreSettings
import com.example.receipt.data.model.ReceiptWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptRepository(private val database: ReceiptDatabase) {

    private val productDao = database.receiptProductDao()
    private val recordDao = database.receiptRecordDao()

    val allProducts: Flow<List<ReceiptProduct>> = productDao.getAllProducts()
    val allReceipts: Flow<List<ReceiptWithItems>> = recordDao.getAllReceiptsWithItems()

    fun searchProducts(query: String): Flow<List<ReceiptProduct>> =
        productDao.searchProducts(query)

    // --- Product Master Management ---

    suspend fun addProduct(sku: String, name: String, price: Double): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val cleanSku = sku.trim().uppercase()
            val cleanName = name.trim()
            if (cleanSku.isBlank()) return@withContext Result.failure(IllegalArgumentException("SKU tidak boleh kosong"))
            if (cleanName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Nama produk tidak boleh kosong"))
            if (price < 0) return@withContext Result.failure(IllegalArgumentException("Harga satuan tidak boleh negatif"))

            val existing = productDao.getProductBySku(cleanSku)
            if (existing != null) {
                return@withContext Result.failure(IllegalArgumentException("SKU '$cleanSku' sudah terdaftar! Gunakan SKU lain."))
            }

            val id = productDao.insertProduct(
                ReceiptProduct(
                    sku = cleanSku,
                    name = cleanName,
                    unitPrice = price
                )
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(id: Long, sku: String, name: String, price: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanSku = sku.trim().uppercase()
            val cleanName = name.trim()
            if (cleanSku.isBlank()) return@withContext Result.failure(IllegalArgumentException("SKU tidak boleh kosong"))
            if (cleanName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Nama produk tidak boleh kosong"))
            if (price < 0) return@withContext Result.failure(IllegalArgumentException("Harga satuan tidak boleh negatif"))

            val existingWithSku = productDao.getProductBySku(cleanSku)
            if (existingWithSku != null && existingWithSku.id != id) {
                return@withContext Result.failure(IllegalArgumentException("SKU '$cleanSku' sudah digunakan oleh produk lain!"))
            }

            val current = productDao.getProductById(id)
                ?: return@withContext Result.failure(IllegalArgumentException("Produk tidak ditemukan"))

            productDao.updateProduct(
                current.copy(
                    sku = cleanSku,
                    name = cleanName,
                    unitPrice = price
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(product: ReceiptProduct): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            productDao.deleteProduct(product)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- CSV Import & Export ---

    fun getSampleCsvTemplate(): String {
        return """
SKU,Nama Produk,Harga Satuan
BNR-280,Banner Flexi Frontlite 280gsm,25000
BNR-380,Banner Flexi Korea 380gsm,45000
X-BNR-60,X-Banner 60x160cm Komplit Rangka,65000
ROLL-UP-80,Roll Up Banner 80x200cm Aluminium,185000
STK-VINYL,Stiker Vinyl A3+ Kiss Cut,18000
        """.trimIndent()
    }

    suspend fun exportProductsToCsv(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("SKU,Nama Produk,Harga Satuan\n")
        val cursor = database.openHelper.readableDatabase.query(
            "SELECT sku, name, unitPrice FROM receipt_products ORDER BY name ASC"
        )
        while (cursor.moveToNext()) {
            val sku = cursor.getString(0)
            val name = cursor.getString(1).replace(",", ";")
            val price = cursor.getDouble(2)
            sb.append("\"$sku\",\"$name\",$price\n")
        }
        cursor.close()
        sb.toString()
    }

    suspend fun importProductsFromCsv(csvText: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val lines = csvText.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return@withContext Result.failure(IllegalArgumentException("File CSV kosong"))

            var importedCount = 0
            for ((index, line) in lines.withIndex()) {
                if (index == 0 && (line.contains("SKU", ignoreCase = true) || line.contains("Harga", ignoreCase = true))) {
                    continue // Skip header row
                }

                val delimiter = if (line.contains(";")) ";" else ","
                val parts = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
                if (parts.size >= 3) {
                    val sku = parts[0].uppercase()
                    val name = parts[1]
                    val price = parts[2].toDoubleOrNull() ?: 0.0

                    if (sku.isNotBlank() && name.isNotBlank() && price >= 0) {
                        val existing = productDao.getProductBySku(sku)
                        if (existing != null) {
                            productDao.updateProduct(existing.copy(name = name, unitPrice = price))
                        } else {
                            productDao.insertProduct(ReceiptProduct(sku = sku, name = name, unitPrice = price))
                        }
                        importedCount++
                    }
                }
            }
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Sequential Receipt Numbering ---

    suspend fun getNextReceiptNumber(
        prefix: String = "STRUK-",
        startNumber: Int = 1
    ): String = withContext(Dispatchers.IO) {
        val latest = recordDao.getLatestReceipt()
        if (latest == null) {
            return@withContext String.format(Locale.US, "%s%04d", prefix, startNumber)
        }

        val lastNumStr = latest.receiptNumber
        // Extract digits at the end
        val regex = Regex("""\d+$""")
        val match = regex.find(lastNumStr)
        if (match != null) {
            val digits = match.value
            val nextVal = (digits.toLongOrNull() ?: 0L) + 1
            val prefixPart = lastNumStr.substring(0, match.range.first)
            val padding = digits.length.coerceAtLeast(4)
            return@withContext String.format(Locale.US, "%s%0${padding}d", prefixPart, nextVal)
        } else {
            return@withContext String.format(Locale.US, "%s%04d", prefix, startNumber)
        }
    }

    suspend fun isReceiptNumberExists(number: String): Boolean = withContext(Dispatchers.IO) {
        recordDao.getReceiptByNumber(number.trim()) != null
    }

    // --- Generate & Save Receipt ---

    suspend fun generateAndSaveReceipt(
        receiptNumber: String,
        storeSettings: ReceiptStoreSettings,
        paymentMethod: String,
        items: List<ReceiptCartItem>,
        notes: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val cleanNumber = receiptNumber.trim()
            if (cleanNumber.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Nomor struk tidak boleh kosong"))
            }

            if (recordDao.getReceiptByNumber(cleanNumber) != null) {
                return@withContext Result.failure(IllegalArgumentException("Nomor struk '$cleanNumber' sudah digunakan! Gunakan nomor lain."))
            }

            if (items.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Pilih minimal 1 produk untuk membuat struk"))
            }

            val now = System.currentTimeMillis()
            val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
            val formattedDate = dateTimeFormat.format(Date(now))
            val printDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))

            val totalAmount = items.sumOf { it.subtotal }
            val totalQuantity = items.sumOf { it.quantity }
            val totalItemTypes = items.size

            val receiptRecord = ReceiptRecord(
                receiptNumber = cleanNumber,
                dateEpochMs = now,
                formattedDateTime = formattedDate,
                storeName = storeSettings.storeName,
                storeAddress = storeSettings.address,
                storePhone = storeSettings.phone,
                paymentMethod = paymentMethod,
                totalAmount = totalAmount,
                totalItemTypes = totalItemTypes,
                totalQuantity = totalQuantity,
                footerMessage = storeSettings.footerNote1.trim().ifEmpty { "Terima kasih atas kunjungan Anda!" },
                printedAtFormatted = printDateFormat.format(Date(now))
            )

            val receiptId = recordDao.insertReceipt(receiptRecord)

            val snapshotItems = items.map { item ->
                ReceiptItemSnapshot(
                    receiptId = receiptId,
                    sku = item.sku,
                    productName = item.productName,
                    unitPrice = item.customPrice,
                    quantity = item.quantity,
                    subtotal = item.subtotal
                )
            }

            recordDao.insertReceiptItems(snapshotItems)
            Result.success(receiptId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReceipt(receiptId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            recordDao.deleteReceiptById(receiptId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- WhatsApp Text Formatter ---

    fun formatReceiptForWhatsApp(receipt: ReceiptRecord, items: List<ReceiptItemSnapshot>): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        nf.maximumFractionDigits = 0

        val sb = StringBuilder()
        sb.append("*${receipt.storeName.uppercase()}*\n")
        sb.append("${receipt.storeAddress}\n")
        sb.append("Telp: ${receipt.storePhone}\n")
        sb.append("================================\n")
        sb.append("No. Struk : *${receipt.receiptNumber}*\n")
        sb.append("Waktu     : ${receipt.formattedDateTime}\n")
        sb.append("Status    : *${receipt.paymentMethod}*\n")
        sb.append("================================\n")

        items.forEach { item ->
            sb.append("*${item.productName}*\n")
            sb.append("  ${item.quantity} x ${nf.format(item.unitPrice).replace("Rp", "Rp ")} = *${nf.format(item.subtotal).replace("Rp", "Rp ")}*\n")
        }

        sb.append("--------------------------------\n")
        sb.append("Jumlah Jenis : ${receipt.totalItemTypes} produk\n")
        sb.append("Total Item   : ${receipt.totalQuantity} pcs\n")
        sb.append("*TOTAL       : ${nf.format(receipt.totalAmount).replace("Rp", "Rp ")}*\n")
        sb.append("================================\n")
        sb.append("${receipt.footerMessage}\n")
        sb.append("Waktu Cetak : ${receipt.printedAtFormatted}\n")

        return sb.toString()
    }

    // --- Export & Import All Saved Receipts ---

    suspend fun exportAllReceiptsJson(): String = withContext(Dispatchers.IO) {
        val all = recordDao.getAllReceiptsWithItemsSync()
        val root = JSONObject()
        root.put("appName", "Diprint Accounting - Struk")
        root.put("version", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID")).format(Date()))
        root.put("totalReceipts", all.size)

        val receiptsArray = JSONArray()
        for (item in all) {
            val rObj = JSONObject()
            val r = item.receipt
            rObj.put("receiptNumber", r.receiptNumber)
            rObj.put("dateEpochMs", r.dateEpochMs)
            rObj.put("formattedDateTime", r.formattedDateTime)
            rObj.put("storeName", r.storeName)
            rObj.put("storeAddress", r.storeAddress)
            rObj.put("storePhone", r.storePhone)
            rObj.put("paymentMethod", r.paymentMethod)
            rObj.put("totalAmount", r.totalAmount)
            rObj.put("totalItemTypes", r.totalItemTypes)
            rObj.put("totalQuantity", r.totalQuantity)
            rObj.put("footerMessage", r.footerMessage)
            rObj.put("printedAtFormatted", r.printedAtFormatted)

            val itemsArray = JSONArray()
            for (it in item.items) {
                val itObj = JSONObject()
                itObj.put("sku", it.sku)
                itObj.put("productName", it.productName)
                itObj.put("unitPrice", it.unitPrice)
                itObj.put("quantity", it.quantity)
                itObj.put("subtotal", it.subtotal)
                itemsArray.put(itObj)
            }
            rObj.put("items", itemsArray)
            receiptsArray.put(rObj)
        }
        root.put("receipts", receiptsArray)
        root.toString(2)
    }

    suspend fun importReceiptsFromJson(jsonText: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonText)
            val receiptsArray = root.optJSONArray("receipts")
                ?: return@withContext Result.failure(IllegalArgumentException("Format data tidak memiliki array 'receipts'"))

            var count = 0
            for (i in 0 until receiptsArray.length()) {
                val rObj = receiptsArray.getJSONObject(i)
                val receiptNumber = rObj.getString("receiptNumber")
                val dateEpochMs = rObj.optLong("dateEpochMs", System.currentTimeMillis())
                val formattedDateTime = rObj.optString("formattedDateTime", "")
                val storeName = rObj.optString("storeName", "Diprint Indonesia")
                val storeAddress = rObj.optString("storeAddress", "")
                val storePhone = rObj.optString("storePhone", "")
                val paymentMethod = rObj.optString("paymentMethod", "Lunas")
                val totalAmount = rObj.optDouble("totalAmount", 0.0)
                val totalItemTypes = rObj.optInt("totalItemTypes", 0)
                val totalQuantity = rObj.optInt("totalQuantity", 0)
                val footerMessage = rObj.optString("footerMessage", "")
                val printedAtFormatted = rObj.optString("printedAtFormatted", "")

                // Replace if existing with same receipt number
                val existing = recordDao.getReceiptByNumber(receiptNumber)
                val receiptId = if (existing != null) {
                    recordDao.deleteReceiptById(existing.id)
                    recordDao.insertReceipt(
                        ReceiptRecord(
                            receiptNumber = receiptNumber,
                            dateEpochMs = dateEpochMs,
                            formattedDateTime = formattedDateTime,
                            storeName = storeName,
                            storeAddress = storeAddress,
                            storePhone = storePhone,
                            paymentMethod = paymentMethod,
                            totalAmount = totalAmount,
                            totalItemTypes = totalItemTypes,
                            totalQuantity = totalQuantity,
                            footerMessage = footerMessage,
                            printedAtFormatted = printedAtFormatted
                        )
                    )
                } else {
                    recordDao.insertReceipt(
                        ReceiptRecord(
                            receiptNumber = receiptNumber,
                            dateEpochMs = dateEpochMs,
                            formattedDateTime = formattedDateTime,
                            storeName = storeName,
                            storeAddress = storeAddress,
                            storePhone = storePhone,
                            paymentMethod = paymentMethod,
                            totalAmount = totalAmount,
                            totalItemTypes = totalItemTypes,
                            totalQuantity = totalQuantity,
                            footerMessage = footerMessage,
                            printedAtFormatted = printedAtFormatted
                        )
                    )
                }

                val itemsArray = rObj.optJSONArray("items")
                if (itemsArray != null) {
                    val snapshots = mutableListOf<ReceiptItemSnapshot>()
                    for (j in 0 until itemsArray.length()) {
                        val itObj = itemsArray.getJSONObject(j)
                        snapshots.add(
                            ReceiptItemSnapshot(
                                receiptId = receiptId,
                                sku = itObj.optString("sku", ""),
                                productName = itObj.optString("productName", ""),
                                unitPrice = itObj.optDouble("unitPrice", 0.0),
                                quantity = itObj.optInt("quantity", 1),
                                subtotal = itObj.optDouble("subtotal", 0.0)
                            )
                        )
                    }
                    recordDao.insertReceiptItems(snapshots)
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
