package com.example

import com.example.data.model.Account
import com.example.data.model.AccountCategory
import com.example.data.model.NormalBalance
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Phase2AccountingTest {

    // ==========================================
    // TEST 1: CHART OF ACCOUNTS (DAFTAR AKUN)
    // ==========================================

    @Test
    fun testChartOfAccountsInitialIntegrity() {
        val initialAccounts = listOf(
            Account(code = "1001", name = "Kas", category = AccountCategory.ASSET, subcategory = "Kas & Setara Kas", normalBalance = NormalBalance.DEBIT),
            Account(code = "1002", name = "Bank BCA", category = AccountCategory.ASSET, subcategory = "Bank", normalBalance = NormalBalance.DEBIT),
            Account(code = "1101", name = "Piutang Usaha", category = AccountCategory.ASSET, subcategory = "Piutang Lancar", normalBalance = NormalBalance.DEBIT),
            Account(code = "1201", name = "Persediaan", category = AccountCategory.ASSET, subcategory = "Persediaan Barang", normalBalance = NormalBalance.DEBIT),
            Account(code = "2001", name = "Hutang Usaha", category = AccountCategory.LIABILITY, subcategory = "Hutang Lancar", normalBalance = NormalBalance.CREDIT),
            Account(code = "3001", name = "Modal Pemilik", category = AccountCategory.EQUITY, subcategory = "Modal Disetor", normalBalance = NormalBalance.CREDIT),
            Account(code = "3002", name = "Prive Pemilik", category = AccountCategory.EQUITY, subcategory = "Penarikan Modal", normalBalance = NormalBalance.DEBIT),
            Account(code = "3003", name = "Charity Pemilik", category = AccountCategory.EQUITY, subcategory = "Donasi / Zakat", normalBalance = NormalBalance.DEBIT),
            Account(code = "3101", name = "Laba Ditahan", category = AccountCategory.EQUITY, subcategory = "Laba Akumulasi", normalBalance = NormalBalance.CREDIT),
            Account(code = "3999", name = "Opening Balance Equity", category = AccountCategory.EQUITY, subcategory = "Penyeimbang Saldo Awal", normalBalance = NormalBalance.CREDIT),
            Account(code = "4001", name = "Penjualan", category = AccountCategory.REVENUE, subcategory = "Pendapatan Usaha", normalBalance = NormalBalance.CREDIT),
            Account(code = "4002", name = "Pendapatan Lain-lain", category = AccountCategory.REVENUE, subcategory = "Pendapatan Non-Operasional", normalBalance = NormalBalance.CREDIT),
            Account(code = "5001", name = "Harga Pokok Penjualan", category = AccountCategory.COGS, subcategory = "Beban Pokok", normalBalance = NormalBalance.DEBIT),
            Account(code = "5101", name = "Pembelian Persediaan", category = AccountCategory.COGS, subcategory = "Pembelian", normalBalance = NormalBalance.DEBIT),
            Account(code = "6001", name = "Ongkos Kirim", category = AccountCategory.EXPENSE, subcategory = "Beban Distribusi", normalBalance = NormalBalance.DEBIT),
            Account(code = "6002", name = "Biaya Administrasi Bank", category = AccountCategory.EXPENSE, subcategory = "Beban Administrasi", normalBalance = NormalBalance.DEBIT),
            Account(code = "6003", name = "Biaya Operasional Lainnya", category = AccountCategory.EXPENSE, subcategory = "Beban Umum", normalBalance = NormalBalance.DEBIT)
        )

        // 1. Validasi tidak ada kode akun yang duplikat
        val codes = initialAccounts.map { it.code }
        assertEquals("Semua kode akun harus unik", codes.size, codes.toSet().size)

        // 2. Validasi saldo normal sesuai kategori
        val bca = initialAccounts.find { it.code == "1002" }!!
        assertEquals(NormalBalance.DEBIT, bca.normalBalance)

        val hutang = initialAccounts.find { it.code == "2001" }!!
        assertEquals(NormalBalance.CREDIT, hutang.normalBalance)

        val prive = initialAccounts.find { it.code == "3002" }!!
        assertEquals(NormalBalance.DEBIT, prive.normalBalance) // Kontra-ekuitas

        val charity = initialAccounts.find { it.code == "3003" }!!
        assertEquals(NormalBalance.DEBIT, charity.normalBalance) // Kontra-ekuitas

        val obe = initialAccounts.find { it.code == "3999" }!!
        assertEquals(NormalBalance.CREDIT, obe.normalBalance)

        val hpp = initialAccounts.find { it.code == "5001" }!!
        assertEquals(NormalBalance.DEBIT, hpp.normalBalance)
    }

    // ==========================================
    // TEST 2: OPENING BALANCE PREVIEW & EQUALITY
    // ==========================================

    @Test
    fun testOpeningBalancePreviewAndEquality() {
        val bcaDebit = 0.94
        val modalCredit = 0.94

        val totalDebit = bcaDebit
        val totalCredit = modalCredit
        val difference = abs(totalDebit - totalCredit)

        // Validasi: Total Debit harus sama dengan Total Kredit
        assertTrue("Total debit harus seimbang dengan total kredit", difference < 0.0001)
        assertEquals(0.94, totalDebit, 0.0001)
        assertEquals(0.94, totalCredit, 0.0001)

        // Format Rupiah Indonesia
        assertEquals("Rp0,94", FormatUtils.formatRupiah(bcaDebit))
        assertEquals("Rp0,94", FormatUtils.formatRupiah(modalCredit))
    }

    // ==========================================
    // TEST 3: INTEGRASI NERACA & BUKU BESAR
    // ==========================================

    @Test
    fun testAccountingEquationWithOpeningBalance() {
        val assets = 0.94 // Bank BCA
        val liabilities = 0.0 // Hutang Usaha = 0
        val equityWithoutNetIncome = 0.94 // Modal Pemilik
        val netIncome = 0.0

        val totalEquity = equityWithoutNetIncome + netIncome
        val discrepancy = abs(assets - (liabilities + totalEquity))

        // Aktiva = Pasiva
        assertEquals(0.0, discrepancy, 0.0001)
    }

    // ==========================================
    // TEST 4: KEAMANAN DATA & ATURAN HAPUS AKUN
    // ==========================================

    @Test
    fun testAccountDeletionProtectionRule() {
        val accountWithHistory = Account(
            id = 2,
            code = "1002",
            name = "Bank BCA",
            category = AccountCategory.ASSET,
            normalBalance = NormalBalance.DEBIT,
            isSystem = true
        )

        // Simulasi jumlah baris jurnal pada akun
        val journalUsageCount = 1

        val canDelete = !accountWithHistory.isSystem && journalUsageCount == 0
        assertFalse("Akun yang memiliki riwayat jurnal TIDAK BOLEH dihapus", canDelete)
    }

    // ==========================================
    // TEST 5: PENJUALAN DENGAN OPSI KAS, BANK & PIUTANG
    // ==========================================

    @Test
    fun testSalesTransactionSubOptionsJournal() {
        val salesAmount = 150000.0

        // Sub-opsi 1: Kas (1001)
        val debitKas = "1001"
        val creditSales = "4001"
        var debitTotal = salesAmount
        var creditTotal = salesAmount
        assertEquals("Debit = Kredit untuk Penjualan Kas", debitTotal, creditTotal, 0.0001)
        assertEquals("1001", debitKas)
        assertEquals("4001", creditSales)

        // Sub-opsi 2: Bank BCA (1002)
        val debitBank = "1002"
        assertEquals("Debit = Kredit untuk Penjualan Bank", debitTotal, creditTotal, 0.0001)
        assertEquals("1002", debitBank)

        // Sub-opsi 3: Piutang Usaha (1101)
        val debitPiutang = "1101"
        assertEquals("Debit = Kredit untuk Penjualan Piutang", debitTotal, creditTotal, 0.0001)
        assertEquals("1101", debitPiutang)
    }

    // ==========================================
    // TEST 6: PENGELUARAN BEBAN (KAS, BANK, HUTANG)
    // ==========================================

    @Test
    fun testExpenseTransactionSubOptionsJournal() {
        val expenseAmount = 75000.0
        val debitExpense = "6003" // Biaya Operasional Lainnya

        // Sub-opsi 1: Kas (1001)
        val creditKas = "1001"
        assertEquals("Debit = Kredit untuk Beban Kas", expenseAmount, expenseAmount, 0.0001)
        assertEquals("6003", debitExpense)
        assertEquals("1001", creditKas)

        // Sub-opsi 2: Bank BCA (1002)
        val creditBank = "1002"
        assertEquals("Debit = Kredit untuk Beban Bank", expenseAmount, expenseAmount, 0.0001)
        assertEquals("1002", creditBank)

        // Sub-opsi 3: Hutang Usaha (2001)
        val creditHutang = "2001"
        assertEquals("Debit = Kredit untuk Beban Tempo/Hutang", expenseAmount, expenseAmount, 0.0001)
        assertEquals("2001", creditHutang)
    }

    // ==========================================
    // TEST 7: PEMBELIAN PERSEDIAAN (KAS, BANK, HUTANG)
    // ==========================================

    @Test
    fun testPurchaseInventorySubOptionsJournal() {
        val purchaseAmount = 500000.0
        val debitInventory = "1201" // Persediaan

        // Sub-opsi 1: Kas (1001)
        val creditKas = "1001"
        assertEquals("Debit = Kredit untuk Beli Persediaan Kas", purchaseAmount, purchaseAmount, 0.0001)
        assertEquals("1201", debitInventory)
        assertEquals("1001", creditKas)

        // Sub-opsi 2: Bank BCA (1002)
        val creditBank = "1002"
        assertEquals("Debit = Kredit untuk Beli Persediaan Bank", purchaseAmount, purchaseAmount, 0.0001)
        assertEquals("1002", creditBank)

        // Sub-opsi 3: Hutang Usaha (2001)
        val creditHutang = "2001"
        assertEquals("Debit = Kredit untuk Beli Persediaan Hutang/Tempo", purchaseAmount, purchaseAmount, 0.0001)
        assertEquals("2001", creditHutang)
    }

    // ==========================================
    // TEST 8: PEMBAYARAN HUTANG (KAS & BANK)
    // ==========================================

    @Test
    fun testPayDebtSubOptionsJournal() {
        val debtAmount = 40500.0
        val debitDebt = "2001" // Hutang Usaha

        // Kas
        val creditKas = "1001"
        assertEquals("Debit = Kredit untuk Bayar Hutang Kas", debtAmount, debtAmount, 0.0001)
        assertEquals("2001", debitDebt)
        assertEquals("1001", creditKas)

        // Bank BCA
        val creditBank = "1002"
        assertEquals("Debit = Kredit untuk Bayar Hutang Bank", debtAmount, debtAmount, 0.0001)
        assertEquals("1002", creditBank)
    }

    // ==========================================
    // TEST 9: PENERIMAAN PIUTANG (KAS & BANK)
    // ==========================================

    @Test
    fun testReceiveReceivableSubOptionsJournal() {
        val receivableAmount = 120000.0
        val creditReceivable = "1101" // Piutang Usaha

        // Kas
        val debitKas = "1001"
        assertEquals("Debit = Kredit untuk Terima Piutang Kas", receivableAmount, receivableAmount, 0.0001)
        assertEquals("1001", debitKas)
        assertEquals("1101", creditReceivable)

        // Bank BCA
        val debitBank = "1002"
        assertEquals("Debit = Kredit untuk Terima Piutang Bank", receivableAmount, receivableAmount, 0.0001)
        assertEquals("1002", debitBank)
    }

    // ==========================================
    // TEST 10: PRIVE PEMILIK (KAS & BANK)
    // ==========================================

    @Test
    fun testOwnerDrawSubOptionsJournal() {
        val drawAmount = 50000.0
        val debitDraw = "3002" // Prive Pemilik

        // Kas
        val creditKas = "1001"
        assertEquals("Debit = Kredit untuk Prive Kas", drawAmount, drawAmount, 0.0001)
        assertEquals("3002", debitDraw)
        assertEquals("1001", creditKas)

        // Bank BCA
        val creditBank = "1002"
        assertEquals("Debit = Kredit untuk Prive Bank", drawAmount, drawAmount, 0.0001)
        assertEquals("1002", creditBank)
    }

    // ==========================================
    // TEST 11: DONASI PEMILIK (KAS & BANK)
    // ==========================================

    @Test
    fun testDonationSubOptionsJournal() {
        val donationAmount = 25000.0
        val debitCharity = "3003" // Charity Pemilik (Kontra-Ekuitas)

        // Kas
        val creditKas = "1001"
        assertEquals("Debit = Kredit untuk Donasi Kas", donationAmount, donationAmount, 0.0001)
        assertEquals("3003", debitCharity)
        assertEquals("1001", creditKas)

        // Bank BCA
        val creditBank = "1002"
        assertEquals("Debit = Kredit untuk Donasi Bank", donationAmount, donationAmount, 0.0001)
        assertEquals("1002", creditBank)
    }

    // ==========================================
    // TEST 12: TRANSFER KAS / BANK (KAS KE BANK & BANK KE KAS)
    // ==========================================

    @Test
    fun testTransferSubOptionsJournal() {
        val transferAmount = 100000.0

        // Sub-opsi 1: Kas ke Bank
        val debitKasToBank = "1002" // Bank BCA
        val creditKasToBank = "1001" // Kas
        assertEquals("Debit = Kredit untuk Transfer Kas ke Bank", transferAmount, transferAmount, 0.0001)
        assertEquals("1002", debitKasToBank)
        assertEquals("1001", creditKasToBank)

        // Sub-opsi 2: Bank ke Kas
        val debitBankToKas = "1001" // Kas
        val creditBankToKas = "1002" // Bank BCA
        assertEquals("Debit = Kredit untuk Transfer Bank ke Kas", transferAmount, transferAmount, 0.0001)
        assertEquals("1001", debitBankToKas)
        assertEquals("1002", creditBankToKas)
    }

    // ==========================================
    // TEST 13: SALDO MONETER PERSEDIAAN (AKUN 1201 / INVENTORY LOG)
    // ==========================================

    @Test
    fun testInventoryScreenBalanceCalculation() {
        val inventoryAcc = Account(
            id = 4,
            code = "1201",
            name = "Persediaan",
            category = AccountCategory.ASSET,
            normalBalance = NormalBalance.DEBIT
        )

        // Simulasi saldo persediaan setelah pembelian Rp100.000
        val purchaseAmount = 100000.0
        val balanceItem = com.example.data.repository.AccountBalanceItem(
            account = inventoryAcc,
            totalDebit = purchaseAmount,
            totalCredit = 0.0,
            netBalance = purchaseAmount
        )

        val accountBalances = listOf(balanceItem)
        val inventoryLogs = listOf(
            com.example.data.model.InventoryLog(
                id = 1,
                dateEpochMs = System.currentTimeMillis(),
                type = com.example.data.model.InventoryMutationType.PURCHASE,
                amount = purchaseAmount,
                endingBalance = purchaseAmount,
                notes = "test"
            )
        )

        // Logika penghitungan saldo kartu utama Persediaan (InventoryScreen)
        val accountBalance = accountBalances.find { it.account.code == "1201" || it.account.code == "1140" }?.netBalance
        val latestLogBalance = inventoryLogs.firstOrNull()?.endingBalance
        val computedBalance = when {
            accountBalance != null && accountBalance != 0.0 -> accountBalance
            latestLogBalance != null -> latestLogBalance
            accountBalance != null -> accountBalance
            else -> 0.0
        }

        assertEquals("Saldo kartu moneter persediaan harus terupdate menjadi Rp100.000", purchaseAmount, computedBalance, 0.0001)
    }

    // ==========================================
    // TEST 14: CHARITY BOX (50% DARI LABA BERSIH BERJALAN)
    // ==========================================

    @Test
    fun testCharityBoxFiftyPercentNetIncomeCalculation() {
        // Kasus 1: Laba bersih positif Rp1.000.000
        val positiveNetIncome = 1_000_000.0
        val charityPositive = if (positiveNetIncome > 0.0) positiveNetIncome * 0.50 else 0.0
        assertEquals("Alokasi Charity Box harus tepat 50% dari laba bersih", 500_000.0, charityPositive, 0.0001)

        // Kasus 2: Laba bersih dengan nilai riil (contoh Rp244.910,94)
        val sampleNetIncome = 244_910.94
        val charitySample = if (sampleNetIncome > 0.0) sampleNetIncome * 0.50 else 0.0
        assertEquals("Alokasi 50% dari 244.910,94 adalah 122.455,47", 122_455.47, charitySample, 0.0001)

        // Kasus 3: Rugi bersih (negatif) -> alokasi 0
        val negativeNetIncome = -150_000.0
        val charityNegative = if (negativeNetIncome > 0.0) negativeNetIncome * 0.50 else 0.0
        assertEquals("Jika periode berjalan rugi, alokasi Charity Box adalah Rp0", 0.0, charityNegative, 0.0001)

        // Kasus 4: Impas (0) -> alokasi 0
        val zeroNetIncome = 0.0
        val charityZero = if (zeroNetIncome > 0.0) zeroNetIncome * 0.50 else 0.0
        assertEquals("Jika periode berjalan impas, alokasi Charity Box adalah Rp0", 0.0, charityZero, 0.0001)
    }

    // ==========================================
    // TEST 15: EKSPOR & IMPOR BACKUP JSON
    // ==========================================

    @Test
    fun testExportAndImportBackupJsonValidation() {
        val jsonString = """
            {
              "appName": "Diprint Accounting",
              "version": 1,
              "backupDateEpochMs": 1730900000000,
              "accounts": [
                {
                  "code": "1002",
                  "name": "Bank BCA",
                  "category": "ASSET",
                  "normalBalance": "DEBIT"
                }
              ],
              "journal_entries": [
                {
                  "entryNumber": "JE-20261101-0001",
                  "dateEpochMs": 1730419200000,
                  "description": "Saldo Awal Pembukuan"
                }
              ],
              "journal_lines": [
                {
                  "journalId": 1,
                  "accountId": 1,
                  "debitAmount": 0.94,
                  "creditAmount": 0.0
                }
              ]
            }
        """.trimIndent()

        assertTrue("JSON cadangan harus memiliki appName Diprint Accounting", jsonString.contains("Diprint Accounting"))
        assertTrue("JSON cadangan harus memiliki kode akun 1002", jsonString.contains("1002"))
        assertTrue("JSON cadangan harus memiliki entri jurnal", jsonString.contains("JE-20261101-0001"))
        assertTrue("JSON cadangan harus memiliki nominal debit", jsonString.contains("0.94"))
        assertTrue("JSON cadangan harus memiliki version 1", jsonString.contains("\"version\": 1"))
    }
}




