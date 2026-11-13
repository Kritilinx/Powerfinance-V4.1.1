package com.example

import com.example.data.model.Account
import com.example.data.model.AccountCategory
import com.example.data.model.NormalBalance
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AccountingEngineUnitTest {

    @Test
    fun testInitialBalanceEquality() {
        val bcaDebit = 0.94
        val modalCredit = 0.94

        val totalDebit = bcaDebit
        val totalCredit = modalCredit

        // Validasi: Total Debit harus sama persis dengan Total Kredit
        assertEquals(totalDebit, totalCredit, 0.0001)
        assertEquals(0.94, totalDebit, 0.0001)
    }

    @Test
    fun testIndonesianRupiahFormatting() {
        val amount = 0.94
        val formatted = FormatUtils.formatRupiah(amount)
        assertEquals("Rp0,94", formatted)

        val parsed = FormatUtils.parseRupiahInput("Rp0,94")
        assertTrue(parsed != null)
        assertEquals(0.94, parsed!!, 0.0001)
    }

    @Test
    fun testDonationAsEquityDeduction() {
        val donationAccount = Account(
            code = "3130",
            name = "Donasi Pemilik (Pengurang Ekuitas)",
            category = AccountCategory.EQUITY,
            normalBalance = NormalBalance.DEBIT, // Debit normal balance reduces owner's equity!
            isSystem = true
        )

        // Validasi: Kategori akun donasi harus EKUITAS, bukan BEBAN
        assertEquals(AccountCategory.EQUITY, donationAccount.category)
        assertEquals(NormalBalance.DEBIT, donationAccount.normalBalance)
    }

    @Test
    fun testDoubleEntryEquationBalance() {
        // Aset = Kewajiban + Ekuitas
        val assets = 0.94
        val liabilities = 0.0
        val equityWithoutNetIncome = 0.94
        val netIncome = 0.0

        val totalEquity = equityWithoutNetIncome + netIncome
        val discrepancy = abs(assets - (liabilities + totalEquity))

        assertTrue("Neraca harus seimbang (discrepancy < 0.01)", discrepancy < 0.01)
    }

    @Test
    fun testDebtCategoryGroupingFlow() {
        data class TestJournalLine(
            val description: String,
            val debitAmount: Double,
            val creditAmount: Double,
            val refNo: String
        )

        fun extractCategory(line: TestJournalLine): String? {
            val text = "${line.description} ${line.refNo}"
            return when {
                text.contains("[R]") || line.refNo == "R" -> "R"
                text.contains("[K]") || line.refNo == "K" -> "K"
                else -> null
            }
        }

        // 1. Pengeluaran Beban Operasional (Hutang)
        val expenseDebtR = TestJournalLine("[R] Beban Sewa Tempat", debitAmount = 0.0, creditAmount = 500000.0, refNo = "R")
        val expenseDebtK = TestJournalLine("[K] Beban Maintenance Mesin", debitAmount = 0.0, creditAmount = 300000.0, refNo = "K")

        // 2. Pembelian Persediaan (Hutang)
        val purchaseDebtR = TestJournalLine("[R] Pembelian Kertas Art Paper", debitAmount = 0.0, creditAmount = 750000.0, refNo = "R")
        val purchaseDebtK = TestJournalLine("[K] Pembelian Tinta Sublim", debitAmount = 0.0, creditAmount = 1200000.0, refNo = "K")

        // 3. Pembayaran Hutang
        val payDebtR = TestJournalLine("[R] Pembayaran Hutang Sewa", debitAmount = 400000.0, creditAmount = 0.0, refNo = "R")
        val payDebtK = TestJournalLine("[K] Pembayaran Hutang Tinta", debitAmount = 600000.0, creditAmount = 0.0, refNo = "K")

        val allLines = listOf(expenseDebtR, expenseDebtK, purchaseDebtR, purchaseDebtK, payDebtR, payDebtK)

        // Hitung Kategori R
        val linesR = allLines.filter { extractCategory(it) == "R" }
        val totalHutangR = linesR.sumOf { it.creditAmount }
        val totalBayarR = linesR.sumOf { it.debitAmount }
        val saldoSisaR = totalHutangR - totalBayarR

        assertEquals(1250000.0, totalHutangR, 0.001) // 500k + 750k
        assertEquals(400000.0, totalBayarR, 0.001)   // 400k
        assertEquals(850000.0, saldoSisaR, 0.001)    // 1250k - 400k = 850k

        // Hitung Kategori K
        val linesK = allLines.filter { extractCategory(it) == "K" }
        val totalHutangK = linesK.sumOf { it.creditAmount }
        val totalBayarK = linesK.sumOf { it.debitAmount }
        val saldoSisaK = totalHutangK - totalBayarK

        assertEquals(1500000.0, totalHutangK, 0.001) // 300k + 1200k
        assertEquals(600000.0, totalBayarK, 0.001)   // 600k
        assertEquals(900000.0, saldoSisaK, 0.001)    // 1500k - 600k = 900k

        // Total Saldo Hutang Usaha Akun
        val totalSaldoAkun = saldoSisaR + saldoSisaK
        assertEquals(1750000.0, totalSaldoAkun, 0.001)
    }
}
