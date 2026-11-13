package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.receipt.data.ReceiptDatabase
import com.example.receipt.data.ReceiptRepository
import com.example.receipt.data.model.ReceiptCartItem
import com.example.receipt.data.model.ReceiptStoreSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReceiptFeatureTest {

    private lateinit var receiptDb: ReceiptDatabase
    private lateinit var receiptRepo: ReceiptRepository

    private lateinit var accountingDb: AppDatabase

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Isolated Receipt Database
        receiptDb = Room.inMemoryDatabaseBuilder(context, ReceiptDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        receiptRepo = ReceiptRepository(receiptDb)
        ReceiptDatabase.populateInitialProducts(receiptDb)

        // 2. Accounting Database to verify isolation
        accountingDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.populateInitialAccounts(accountingDb)
    }

    @After
    fun tearDown() {
        receiptDb.close()
        accountingDb.close()
    }

    @Test
    fun testAddProductAndDuplicateSkuValidation() = runBlocking {
        // Add new unique product
        val result1 = receiptRepo.addProduct("TEST-001", "Cetak Stiker Hologram", 20000.0)
        assertTrue("Add product should succeed", result1.isSuccess)

        // Verify product in list
        val products = receiptRepo.allProducts.first()
        val found = products.find { it.sku == "TEST-001" }
        assertNotNull(found)
        assertEquals("Cetak Stiker Hologram", found?.name)
        assertEquals(20000.0, found?.unitPrice ?: 0.0, 0.01)

        // Attempt duplicate SKU
        val resultDuplicate = receiptRepo.addProduct("TEST-001", "Produk Duplikat", 30000.0)
        assertTrue("Duplicate SKU must fail", resultDuplicate.isFailure)
    }

    @Test
    fun testEditProductPrice() = runBlocking {
        val addRes = receiptRepo.addProduct("EDIT-01", "Undangan Pernikahan Softcover", 5000.0)
        val prodId = addRes.getOrThrow()

        // Update price
        val updateRes = receiptRepo.updateProduct(prodId, "EDIT-01", "Undangan Pernikahan Softcover", 6500.0)
        assertTrue(updateRes.isSuccess)

        val updated = receiptDb.receiptProductDao().getProductById(prodId)
        assertEquals(6500.0, updated?.unitPrice ?: 0.0, 0.01)
    }

    @Test
    fun testImportCsvAndTemplate() = runBlocking {
        val template = receiptRepo.getSampleCsvTemplate()
        assertTrue("Template should contain header", template.contains("SKU,Nama Produk,Harga Satuan"))

        val csvData = """
SKU,Nama Produk,Harga Satuan
IMP-01,Kalender Meja Dudukan Linen,35000
IMP-02,Buku Yasin Bludru Emas,25000
        """.trimIndent()

        val importRes = receiptRepo.importProductsFromCsv(csvData)
        assertTrue(importRes.isSuccess)
        assertEquals(2, importRes.getOrThrow())

        val p1 = receiptDb.receiptProductDao().getProductBySku("IMP-01")
        assertNotNull(p1)
        assertEquals(35000.0, p1?.unitPrice ?: 0.0, 0.01)
    }

    @Test
    fun testGenerateReceiptSingleProductAndTotals() = runBlocking {
        val storeSettings = ReceiptStoreSettings(
            storeName = "Diprint Indonesia",
            address = "Jl. Grafika No. 10",
            phone = "0812-3456-7890"
        )

        val cart = listOf(
            ReceiptCartItem(
                sku = "BNR-280",
                productName = "Banner Flexi 280gsm",
                originalPrice = 25000.0,
                customPrice = 25000.0,
                quantity = 4
            )
        )

        val receiptNum = "STRUK-0001"
        val genResult = receiptRepo.generateAndSaveReceipt(
            receiptNumber = receiptNum,
            storeSettings = storeSettings,
            paymentMethod = "LUNAS",
            items = cart
        )

        assertTrue("Generate receipt should succeed", genResult.isSuccess)
        val receiptId = genResult.getOrThrow()

        val saved = receiptDb.receiptRecordDao().getReceiptWithItemsById(receiptId)
        assertNotNull(saved)
        assertEquals("STRUK-0001", saved?.receipt?.receiptNumber)
        assertEquals(1, saved?.receipt?.totalItemTypes)
        assertEquals(4, saved?.receipt?.totalQuantity)
        assertEquals(100000.0, saved?.receipt?.totalAmount ?: 0.0, 0.01)
        assertEquals(1, saved?.items?.size)
    }

    @Test
    fun testGenerateReceiptMultipleProductsAndCustomPrice() = runBlocking {
        val storeSettings = ReceiptStoreSettings()
        val cart = listOf(
            ReceiptCartItem(
                sku = "BNR-280",
                productName = "Banner Flexi 280gsm",
                originalPrice = 25000.0,
                customPrice = 22000.0, // Discounted custom price for this specific receipt
                quantity = 10
            ),
            ReceiptCartItem(
                sku = "X-BNR-60",
                productName = "X-Banner 60x160cm",
                originalPrice = 65000.0,
                customPrice = 65000.0,
                quantity = 2
            )
        )

        val genResult = receiptRepo.generateAndSaveReceipt(
            receiptNumber = "STRUK-0002",
            storeSettings = storeSettings,
            paymentMethod = "TRANSFER BCA",
            items = cart
        )

        assertTrue(genResult.isSuccess)
        val receiptId = genResult.getOrThrow()
        val saved = receiptDb.receiptRecordDao().getReceiptWithItemsById(receiptId)

        assertNotNull(saved)
        assertEquals(2, saved?.receipt?.totalItemTypes)
        assertEquals(12, saved?.receipt?.totalQuantity) // 10 + 2
        // Total: (10 * 22.000) + (2 * 65.000) = 220.000 + 130.000 = 350.000
        assertEquals(350000.0, saved?.receipt?.totalAmount ?: 0.0, 0.01)
    }

    @Test
    fun testSequentialReceiptNumberingAndDuplicatePrevention() = runBlocking {
        val settings = ReceiptStoreSettings(receiptPrefix = "STRUK-", startNumber = 1)

        val next1 = receiptRepo.getNextReceiptNumber(settings.receiptPrefix, settings.startNumber)
        assertEquals("STRUK-0001", next1)

        // Save receipt STRUK-0001
        receiptRepo.generateAndSaveReceipt(
            receiptNumber = "STRUK-0001",
            storeSettings = settings,
            paymentMethod = "LUNAS",
            items = listOf(ReceiptCartItem(sku = "TEST", productName = "Item", originalPrice = 1000.0, customPrice = 1000.0, quantity = 1))
        )

        // Next number should automatically be STRUK-0002
        val next2 = receiptRepo.getNextReceiptNumber(settings.receiptPrefix, settings.startNumber)
        assertEquals("STRUK-0002", next2)

        // Trying to save duplicate STRUK-0001 must fail
        val dupResult = receiptRepo.generateAndSaveReceipt(
            receiptNumber = "STRUK-0001",
            storeSettings = settings,
            paymentMethod = "LUNAS",
            items = listOf(ReceiptCartItem(sku = "TEST", productName = "Item", originalPrice = 1000.0, customPrice = 1000.0, quantity = 1))
        )
        assertTrue("Duplicate receipt number must be prevented", dupResult.isFailure)
    }

    @Test
    fun testHistoricalReceiptSnapshotImmunity() = runBlocking {
        // 1. Add product to master with price 25.000
        val pId = receiptRepo.addProduct("IMMUNE-01", "Banner Flexi 280", 25000.0).getOrThrow()

        // 2. Generate receipt using this product
        val cart = listOf(
            ReceiptCartItem(sku = "IMMUNE-01", productName = "Banner Flexi 280", originalPrice = 25000.0, customPrice = 25000.0, quantity = 2)
        )
        val rId = receiptRepo.generateAndSaveReceipt("STRUK-HIST-01", ReceiptStoreSettings(), "LUNAS", cart).getOrThrow()

        // 3. Update master product price to 35.000
        receiptRepo.updateProduct(pId, "IMMUNE-01", "Banner Flexi 280 (Edisi Baru)", 35000.0)

        // 4. Historical receipt snapshot MUST retain original price 25.000 and total 50.000!
        val savedReceipt = receiptDb.receiptRecordDao().getReceiptWithItemsById(rId)
        assertNotNull(savedReceipt)
        val snapshotItem = savedReceipt?.items?.first()
        assertEquals(25000.0, snapshotItem?.unitPrice ?: 0.0, 0.01)
        assertEquals(50000.0, snapshotItem?.subtotal ?: 0.0, 0.01)
        assertEquals(50000.0, savedReceipt?.receipt?.totalAmount ?: 0.0, 0.01)
    }

    @Test
    fun testAccountingDatabaseStrictlyUntouched(): Unit = runBlocking {
        // Check initial accounts count in accounting database
        val initialAccountsCount = accountingDb.accountDao().countAccounts()
        val initialTxCount = accountingDb.transactionDao().countTransactions()
        val initialJournalCount = accountingDb.journalDao().getAllJournalEntries().first().size

        // Perform multiple receipt generator actions
        receiptRepo.addProduct("SEC-01", "Cetak Spanduk Kain", 40000.0)
        receiptRepo.generateAndSaveReceipt(
            "STRUK-SEC-01",
            ReceiptStoreSettings(),
            "CASH",
            listOf(ReceiptCartItem(sku = "SEC-01", productName = "Cetak Spanduk Kain", originalPrice = 40000.0, customPrice = 40000.0, quantity = 3))
        )

        // Assert accounting database remains completely unchanged!
        val finalAccountsCount = accountingDb.accountDao().countAccounts()
        val finalTxCount = accountingDb.transactionDao().countTransactions()
        val finalJournalCount = accountingDb.journalDao().getAllJournalEntries().first().size

        assertEquals("Accounting accounts must be untouched", initialAccountsCount, finalAccountsCount)
        assertEquals("Accounting transactions must be untouched", initialTxCount, finalTxCount)
        assertEquals("Accounting journals must be untouched", initialJournalCount, finalJournalCount)
    }
}

