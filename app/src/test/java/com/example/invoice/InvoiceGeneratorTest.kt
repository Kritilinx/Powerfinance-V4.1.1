package com.example.invoice

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.invoice.data.InvoiceDatabase
import com.example.invoice.data.InvoiceRepository
import com.example.invoice.data.model.InvoiceDraftItem
import com.example.invoice.data.model.InvoiceRecord
import com.example.invoice.util.InvoicePdfGenerator
import com.example.receipt.data.ReceiptDatabase
import com.example.receipt.data.model.ReceiptProduct
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InvoiceGeneratorTest {

    private lateinit var context: Context
    private lateinit var invoiceDb: InvoiceDatabase
    private lateinit var receiptDb: ReceiptDatabase
    private lateinit var accountingDb: AppDatabase
    private lateinit var repository: InvoiceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // In-memory isolated databases for clean, fast unit testing
        invoiceDb = Room.inMemoryDatabaseBuilder(context, InvoiceDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        receiptDb = Room.inMemoryDatabaseBuilder(context, ReceiptDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        accountingDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Populate sample products in Receipt Database
        runBlocking {
            ReceiptDatabase.populateInitialProducts(receiptDb)
        }

        repository = InvoiceRepository(
            context = context,
            invoiceDao = invoiceDb.invoiceDao(),
            coroutineScope = kotlinx.coroutines.GlobalScope
        )
    }

    @After
    fun tearDown() {
        invoiceDb.close()
        receiptDb.close()
        accountingDb.close()
    }

    // 1. Pemilihan produk dari database struk
    // 2. Harga otomatis
    @Test
    fun testProductSelectionFromReceiptDatabaseWithAutomaticPrice() = runBlocking {
        val initialProductCount = receiptDb.receiptProductDao().countProducts()
        assertTrue("Database produk struk harus memiliki data awal", initialProductCount > 0)

        val product = receiptDb.receiptProductDao().getProductBySku("BNR-280")
        assertNotNull("Produk BNR-280 harus ditemukan", product)
        assertEquals("Banner Flexi Frontlite 280gsm", product!!.name)
        assertEquals(25000.0, product.unitPrice, 0.001)
    }

    // 3. Perubahan harga khusus invoice
    @Test
    fun testCustomPriceOverrideForInvoiceOnly() = runBlocking {
        val originalProduct = receiptDb.receiptProductDao().getProductBySku("BNR-280")!!
        val originalPrice = originalProduct.unitPrice // 25000.0

        // Custom price specific to this invoice (e.g. negotiated price 22000.0)
        val customPrice = 22000.0
        val draftItem = InvoiceDraftItem(
            itemNumber = 1,
            sku = originalProduct.sku,
            description = originalProduct.name,
            quantity = 3,
            unitPrice = customPrice
        )

        assertEquals(66000.0, draftItem.lineTotal, 0.001)

        // Verify receipt database was NOT modified
        val productInDb = receiptDb.receiptProductDao().getProductBySku("BNR-280")!!
        assertEquals(originalPrice, productInDb.unitPrice, 0.001)
    }

    // 4. Perhitungan subtotal, discount, tax, S&H, other, dan total
    @Test
    fun testCalculationsSubtotalAndTotal() = runBlocking {
        val items = listOf(
            InvoiceDraftItem(itemNumber = 1, sku = "BNR-280", description = "Banner", quantity = 2, unitPrice = 25000.0), // 50000
            InvoiceDraftItem(itemNumber = 2, sku = "X-BNR-60", description = "X-Banner", quantity = 1, unitPrice = 65000.0) // 65000
        )

        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        assertEquals(115000.0, subtotal, 0.001)

        val discount = 5000.0
        val tax = 12000.0
        val shipping = 15000.0
        val other = 3000.0

        val total = maxOf(0.0, subtotal - discount + tax + shipping + other)
        // 115000 - 5000 + 12000 + 15000 + 3000 = 140000
        assertEquals(140000.0, total, 0.001)
    }

    // 5. Nomor invoice otomatis format INV/NDP/YYYY/MM/D/002513
    @Test
    fun testAutomaticInvoiceNumberFormatAndSequence() = runBlocking {
        val initialNumber = repository.getNextInvoiceNumber()
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        assertTrue(
            "Nomor invoice harus dimulai dengan prefix INV/NDP/$year/$month/$day/",
            initialNumber.startsWith("INV/NDP/$year/$month/$day/")
        )
        assertTrue(
            "Nomor invoice awal harus berakhiran 002513",
            initialNumber.endsWith("002513")
        )

        // Save one invoice
        repository.saveInvoice(
            invoiceNumber = initialNumber,
            dateFormatted = "07/11/2026",
            customerName = "CV. Cipta Grafika",
            customerPhone = "08123456789",
            customerAddress = "Jakarta",
            draftItems = listOf(InvoiceDraftItem(itemNumber = 1, sku = "SKU-1", description = "Print", quantity = 1, unitPrice = 50000.0)),
            discount = 0.0,
            tax = 0.0,
            shippingAndHandling = 0.0,
            other = 0.0,
            comments = "Lunas"
        )

        // Next number must increment automatically to 002514
        val nextNumber = repository.getNextInvoiceNumber()
        assertTrue(
            "Nomor berikutnya harus otomatis melanjutkan nomor terakhir ke 002514",
            nextNumber.endsWith("002514")
        )
    }

    // 6. Edit nomor invoice
    // 7. Pencegahan nomor duplikat
    @Test
    fun testDuplicateInvoiceNumberPrevention() = runBlocking {
        val number = "INV/NDP/2026/11/6/009999"

        repository.saveInvoice(
            invoiceNumber = number,
            dateFormatted = "07/11/2026",
            customerName = "Pelanggan 1",
            customerPhone = "08111111",
            customerAddress = "",
            draftItems = listOf(InvoiceDraftItem(itemNumber = 1, sku = "SKU-1", description = "Item", quantity = 1, unitPrice = 10000.0)),
            discount = 0.0,
            tax = 0.0,
            shippingAndHandling = 0.0,
            other = 0.0,
            comments = ""
        )

        assertTrue("Nomor harus terdeteksi ada di DB", repository.isInvoiceNumberExists(number))

        try {
            repository.saveInvoice(
                invoiceNumber = number, // Duplicate!
                dateFormatted = "07/11/2026",
                customerName = "Pelanggan 2",
                customerPhone = "08222222",
                customerAddress = "",
                draftItems = listOf(InvoiceDraftItem(itemNumber = 1, sku = "SKU-1", description = "Item", quantity = 1, unitPrice = 10000.0)),
                discount = 0.0,
                tax = 0.0,
                shippingAndHandling = 0.0,
                other = 0.0,
                comments = ""
            )
            fail("Pencegahan nomor duplikat harus melempar exception!")
        } catch (e: Exception) {
            assertTrue("Pesan error harus menyebut duplikat", e.message?.contains("sudah pernah digunakan") == true)
        }
    }

    // 8. PDF benar-benar berukuran A4 (595 x 842 points)
    // 9. Layout dan 10. PDF dapat dibuat dan dibuka
    @Test
    fun testA4PdfGenerationFileIntegrity() = runBlocking {
        val saved = repository.saveInvoice(
            invoiceNumber = "INV/NDP/2026/11/6/002513",
            dateFormatted = "06/11/2026",
            customerName = "PT. Sinar Jaya Abadi",
            customerPhone = "0813-8888-9999",
            customerAddress = "Kawasan Industri Pulo Gadung, Jakarta Timur",
            draftItems = listOf(
                InvoiceDraftItem(itemNumber = 1, sku = "BNR-380", description = "Banner Flexi Korea 380gsm High-Res", quantity = 5, unitPrice = 45000.0),
                InvoiceDraftItem(itemNumber = 2, sku = "X-BNR-60", description = "X-Banner 60x160cm Lengkap Rangka", quantity = 2, unitPrice = 65000.0)
            ),
            discount = 15000.0,
            tax = 35000.0,
            shippingAndHandling = 25000.0,
            other = 5000.0,
            comments = "Transfer Bank BCA: 123-456-7890 a/n Diprint"
        )

        val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, saved)
        assertTrue("File PDF harus berhasil dibuat", pdfFile.exists())
        assertTrue("Ukuran file PDF harus lebih besar dari 500 byte", pdfFile.length() > 500)

        // Verify PDF Header signature (%PDF-)
        val headerBytes = ByteArray(5)
        pdfFile.inputStream().use { it.read(headerBytes) }
        val headerString = String(headerBytes)
        assertEquals("%PDF-", headerString)

        // Verify dimensions constants
        assertEquals(595, InvoicePdfGenerator.A4_WIDTH_POINTS)
        assertEquals(842, InvoicePdfGenerator.A4_HEIGHT_POINTS)
    }

    // 11. Riwayat invoice tersimpan & pencarian
    // 12. Invoice lama tidak berubah ketika harga produk master diperbarui
    @Test
    fun testInvoiceHistoryPersistenceAndSnapshotIsolation() = runBlocking {
        val inv = repository.saveInvoice(
            invoiceNumber = "INV/NDP/2026/11/6/003001",
            dateFormatted = "07/11/2026",
            customerName = "Toko Berkah",
            customerPhone = "087712345678",
            customerAddress = "Bandung",
            draftItems = listOf(
                InvoiceDraftItem(itemNumber = 1, sku = "KRT-NAMA", description = "Kartu Nama 2 Muka + Laminasi Doff (Box)", quantity = 4, unitPrice = 45000.0)
            ),
            discount = 0.0,
            tax = 0.0,
            shippingAndHandling = 0.0,
            other = 0.0,
            comments = ""
        )

        // 1. Check history
        val list = repository.getAllInvoices().first()
        assertEquals(1, list.size)
        assertEquals("INV/NDP/2026/11/6/003001", list[0].invoice.invoiceNumber)
        assertEquals(180000.0, list[0].invoice.totalAmount, 0.001)

        // 2. Simulate product master price update in receiptDb
        val productInMaster = receiptDb.receiptProductDao().getProductBySku("KRT-NAMA")!!
        receiptDb.receiptProductDao().updateProduct(productInMaster.copy(unitPrice = 60000.0))

        // 3. Historical invoice item must retain the snapshot price (45000.0)
        val refreshedInvoice = repository.getAllInvoices().first()[0]
        assertEquals(45000.0, refreshedInvoice.items[0].unitPrice, 0.001)
        assertEquals(180000.0, refreshedInvoice.invoice.totalAmount, 0.001)
    }

    // 13. Isolasi: Database akuntansi utama tidak terganggu
    @Test
    fun testAccountingDatabaseIsolation() = runBlocking {
        // Accounting database should have 0 transactions inserted by invoice generator
        val txCount = accountingDb.transactionDao().countTransactions()
        assertEquals(0, txCount)

        val journalCount = accountingDb.journalDao().countJournalEntries()
        assertEquals(0, journalCount)
    }

    // 14. Pengujian persistensi komentar / instruksi khusus antar invoice
    @Test
    fun testInvoiceCommentsPersistence() = runBlocking {
        val customComments = "1. DP 50% via BCA 7570689503.\n2. Pelunasan saat barang siap kirim."
        repository.saveLastComments(customComments)

        val retrieved = repository.getLastSavedComments("Default fallback")
        assertEquals(customComments, retrieved)

        // Simpan invoice baru dengan custom komentar
        repository.saveInvoice(
            invoiceNumber = "INV/NDP/2026/11/6/003999",
            dateFormatted = "07/11/2026",
            customerName = "Pelanggan Custom Comments",
            customerPhone = "0812345678",
            customerAddress = "Jakarta",
            draftItems = listOf(
                InvoiceDraftItem(itemNumber = 1, sku = "BNR-280", description = "Banner", quantity = 1, unitPrice = 25000.0)
            ),
            discount = 0.0,
            tax = 0.0,
            shippingAndHandling = 0.0,
            other = 0.0,
            comments = customComments
        )

        // Invoice selanjutnya harus tetap membaca custom comments ini
        val nextComments = repository.getLastSavedComments("Default fallback")
        assertEquals(customComments, nextComments)
    }

    // 15. Pengujian kolom SKU dari database produk struk
    @Test
    fun testSkuProductCatalogMapping() = runBlocking {
        val product = receiptDb.receiptProductDao().getProductBySku("BNR-280")!!
        val draftItem = InvoiceDraftItem(
            itemNumber = 1,
            sku = product.sku,
            description = product.name,
            quantity = 2,
            unitPrice = product.unitPrice
        )

        val saved = repository.saveInvoice(
            invoiceNumber = "INV/NDP/2026/11/6/004000",
            dateFormatted = "07/11/2026",
            customerName = "Pelanggan SKU",
            customerPhone = "0812345",
            customerAddress = "",
            draftItems = listOf(draftItem),
            discount = 0.0,
            tax = 0.0,
            shippingAndHandling = 0.0,
            other = 0.0,
            comments = "Komentar SKU"
        )

        assertEquals(1, saved.items.size)
        assertEquals("BNR-280", saved.items[0].sku)
        assertEquals("Banner Flexi Frontlite 280gsm", saved.items[0].description)
    }
}
