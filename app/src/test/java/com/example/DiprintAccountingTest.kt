package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.model.AccountCategory
import com.example.data.model.NormalBalance
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.repository.AccountingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DiprintAccountingTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AccountingRepository

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AccountingRepository(database)

        // Populate initial default Chart of Accounts
        AppDatabase.populateInitialAccounts(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInitialCOAPopulation() = runBlocking {
        val accounts = repository.allAccounts.first()
        assertTrue("Accounts list should not be empty", accounts.isNotEmpty())

        val kasAccount = accounts.find { it.code == "1001" }
        assertNotNull("Kas should exist", kasAccount)
        assertEquals("Kas", kasAccount?.name)
        assertEquals(AccountCategory.ASSET, kasAccount?.category)
        assertEquals(NormalBalance.DEBIT, kasAccount?.normalBalance)

        val bankAccount = accounts.find { it.code == "1002" }
        assertNotNull("Bank BCA should exist", bankAccount)
        assertEquals("Bank BCA", bankAccount?.name)
        assertEquals(AccountCategory.ASSET, bankAccount?.category)
        assertEquals(NormalBalance.DEBIT, bankAccount?.normalBalance)

        val salesAccount = accounts.find { it.code == "4001" }
        assertNotNull("Penjualan should exist", salesAccount)
        assertEquals(AccountCategory.REVENUE, salesAccount?.category)
        assertEquals(NormalBalance.CREDIT, salesAccount?.normalBalance)

        val persediaanAccount = accounts.find { it.code == "1201" }
        assertNotNull("Persediaan should exist", persediaanAccount)
        assertEquals(AccountCategory.ASSET, persediaanAccount?.category)
    }

    @Test
    fun testTransactionCreationAndPosting() = runBlocking {
        val dateEpochMs = System.currentTimeMillis()
        val amount = 5000000.0
        val kasAccount = repository.getAccountByCode("1002")
        val salesAccount = repository.getAccountByCode("4001")
        assertNotNull(kasAccount)
        assertNotNull(salesAccount)

        val tx = TransactionRecord(
            txNumber = "TX-TEST-001",
            dateEpochMs = dateEpochMs,
            description = "Penjualan Cetak Spanduk & Banner Percetakan",
            type = TransactionType.REVENUE,
            amount = amount,
            debitAccountId = kasAccount!!.id,
            creditAccountId = salesAccount!!.id,
            referenceNumber = "INV-2024-001",
            notes = "Pembayaran via Transfer BCA"
        )

        val result = repository.postTransaction(tx)
        assertTrue("Transaction posting should succeed", result.isSuccess)

        val transactions = repository.allTransactions.first()
        val createdTx = transactions.find { it.txNumber == "TX-TEST-001" }
        assertNotNull(createdTx)
        assertEquals(amount, createdTx?.amount ?: 0.0, 0.001)

        val journals = repository.allJournalsWithLines.first()
        val journalForTx = journals.find { it.entry.transactionId == createdTx?.id }
        assertNotNull("Journal entry must be created", journalForTx)
        assertEquals(2, journalForTx?.lines?.size)

        val drLine = journalForTx?.lines?.find { it.accountId == kasAccount.id }
        val crLine = journalForTx?.lines?.find { it.accountId == salesAccount.id }

        assertEquals(amount, drLine?.debitAmount ?: 0.0, 0.001)
        assertEquals(0.0, drLine?.creditAmount ?: 1.0, 0.001)

        assertEquals(0.0, crLine?.debitAmount ?: 1.0, 0.001)
        assertEquals(amount, crLine?.creditAmount ?: 0.0, 0.001)
    }

    @Test
    fun testFinancialReportsCalculation() = runBlocking {
        val income = repository.getIncomeStatement().first()
        assertNotNull(income)
        assertTrue(income.totalRevenue >= 0)
        assertTrue(income.totalExpense >= 0)

        val balanceSheet = repository.getBalanceSheet().first()
        assertNotNull(balanceSheet)
        assertTrue("Balance sheet must be calculated", balanceSheet.totalAssets >= 0)
    }

    @Test
    fun testCsvExports() = runBlocking {
        val journalCsv = repository.exportJournalsToCsv()
        assertNotNull(journalCsv)
        assertTrue("CSV header should contain Nomor Jurnal", journalCsv.contains("Nomor Jurnal"))

        val ledgerCsv = repository.exportGeneralLedgerToCsv()
        assertNotNull(ledgerCsv)
        assertTrue("CSV header should contain Saldo Berjalan", ledgerCsv.contains("Saldo Berjalan"))
    }

}
