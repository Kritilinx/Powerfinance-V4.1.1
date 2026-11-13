package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.model.Account
import com.example.data.model.AccountCategory
import com.example.data.model.ExcelArchive
import com.example.data.model.InventoryLog
import com.example.data.model.InventoryMutationType
import com.example.data.model.JournalEntry
import com.example.data.model.JournalLine
import com.example.data.model.JournalLineDetail
import com.example.data.model.JournalWithLines
import com.example.data.model.NormalBalance
import com.example.data.model.OpeningBalanceBatch
import com.example.data.model.OpeningBalanceDetail
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class AccountBalanceItem(
    val account: Account,
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double
)

data class IncomeStatementData(
    val revenueItems: List<AccountBalanceItem>,
    val expenseItems: List<AccountBalanceItem>,
    val totalRevenue: Double,
    val totalExpense: Double,
    val netIncome: Double
)

data class BalanceSheetData(
    val assetItems: List<AccountBalanceItem>,
    val liabilityItems: List<AccountBalanceItem>,
    val equityItems: List<AccountBalanceItem>,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val totalEquityWithoutNetIncome: Double,
    val netIncomeCurrentPeriod: Double,
    val totalEquity: Double,
    val isBalanced: Boolean,
    val discrepancy: Double
)

data class CashFlowItem(
    val dateEpochMs: Long,
    val description: String,
    val category: String, // Operasi, Investasi, Pendanaan
    val inAmount: Double,
    val outAmount: Double
)

data class CashFlowStatementData(
    val operatingIn: Double,
    val operatingOut: Double,
    val netOperating: Double,
    val financingIn: Double,
    val financingOut: Double,
    val netFinancing: Double,
    val netCashChange: Double,
    val cashItems: List<CashFlowItem>
)

class AccountingRepository(private val database: AppDatabase) {

    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val journalDao = database.journalDao()
    private val inventoryDao = database.inventoryDao()
    private val excelArchiveDao = database.excelArchiveDao()
    private val openingBalanceDao = database.openingBalanceDao()

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val activeAccounts: Flow<List<Account>> = accountDao.getActiveAccounts()
    val allTransactions: Flow<List<TransactionRecord>> = transactionDao.getAllTransactions()
    val allJournalsWithLines: Flow<List<JournalWithLines>> = journalDao.getJournalsWithLines()
    val allPostedJournalLines: Flow<List<JournalLineDetail>> = journalDao.getAllPostedJournalLineDetails()
    val allInventoryLogs: Flow<List<InventoryLog>> = inventoryDao.getAllInventoryLogs()
    val allArchives: Flow<List<ExcelArchive>> = excelArchiveDao.getAllArchives()
    val allOpeningBatches: Flow<List<OpeningBalanceBatch>> = openingBalanceDao.getAllBatches()

    suspend fun getAccountById(id: Long): Account? = accountDao.getAccountById(id)
    suspend fun getAccountByCode(code: String): Account? = accountDao.getAccountByCode(code)
    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = accountDao.getAccountByCode(account.code)
            if (existing != null && existing.id != account.id) {
                return@withContext Result.failure(IllegalArgumentException("Kode akun ${account.code} sudah digunakan oleh akun lain."))
            }
            accountDao.updateAccount(account)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleAccountActive(accountId: Long, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val acc = accountDao.getAccountById(accountId)
                ?: return@withContext Result.failure(IllegalArgumentException("Akun tidak ditemukan"))
            accountDao.updateAccount(acc.copy(isActive = isActive))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(account: Account): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (account.isSystem) {
                return@withContext Result.failure(IllegalStateException("Akun sistem bawaan '${account.name}' tidak dapat dihapus."))
            }
            val journalUsageCount = accountDao.getJournalLinesCountForAccount(account.id)
            if (journalUsageCount > 0) {
                return@withContext Result.failure(
                    IllegalStateException("Akun '${account.code} - ${account.name}' memiliki $journalUsageCount riwayat jurnal dan tidak boleh dihapus demi menjaga integritas pembukuan.")
                )
            }
            accountDao.deleteAccount(account)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun searchAccounts(query: String): Flow<List<Account>> {
        return if (query.isBlank()) {
            accountDao.getAllAccounts()
        } else {
            accountDao.searchAccounts(query.trim())
        }
    }

    // Flow for real-time account balances from posted journals
    val accountBalances: Flow<List<AccountBalanceItem>> = combine(
        allAccounts,
        allPostedJournalLines
    ) { accounts, postedLines ->
        val debitMap = mutableMapOf<Long, Double>()
        val creditMap = mutableMapOf<Long, Double>()

        for (line in postedLines) {
            debitMap[line.accountId] = (debitMap[line.accountId] ?: 0.0) + line.debitAmount
            creditMap[line.accountId] = (creditMap[line.accountId] ?: 0.0) + line.creditAmount
        }

        accounts.map { acc ->
            val totalDebit = debitMap[acc.id] ?: 0.0
            val totalCredit = creditMap[acc.id] ?: 0.0
            val net = if (acc.normalBalance == NormalBalance.DEBIT) {
                totalDebit - totalCredit
            } else {
                totalCredit - totalDebit
            }
            AccountBalanceItem(
                account = acc,
                totalDebit = totalDebit,
                totalCredit = totalCredit,
                netBalance = net
            )
        }
    }

    // Income Statement (Laporan Laba Rugi)
    fun getIncomeStatement(startDateMs: Long? = null, endDateMs: Long? = null): Flow<IncomeStatementData> {
        return combine(allAccounts, allPostedJournalLines) { accounts, postedLines ->
            val filteredLines = postedLines.filter { line ->
                (startDateMs == null || line.dateEpochMs >= startDateMs) &&
                (endDateMs == null || line.dateEpochMs <= endDateMs)
            }

            val debitMap = mutableMapOf<Long, Double>()
            val creditMap = mutableMapOf<Long, Double>()
            for (line in filteredLines) {
                debitMap[line.accountId] = (debitMap[line.accountId] ?: 0.0) + line.debitAmount
                creditMap[line.accountId] = (creditMap[line.accountId] ?: 0.0) + line.creditAmount
            }

            val revItems = accounts.filter { it.category == AccountCategory.REVENUE }.map { acc ->
                val dr = debitMap[acc.id] ?: 0.0
                val cr = creditMap[acc.id] ?: 0.0
                val net = cr - dr
                AccountBalanceItem(acc, dr, cr, net)
            }.filter { it.netBalance != 0.0 || it.account.isSystem }

            val expItems = accounts.filter { it.category == AccountCategory.EXPENSE || it.category == AccountCategory.COGS }.map { acc ->
                val dr = debitMap[acc.id] ?: 0.0
                val cr = creditMap[acc.id] ?: 0.0
                val net = dr - cr
                AccountBalanceItem(acc, dr, cr, net)
            }.filter { it.netBalance != 0.0 || it.account.isSystem }

            val totalRev = revItems.sumOf { it.netBalance }
            val totalExp = expItems.sumOf { it.netBalance }
            val netIncome = totalRev - totalExp

            IncomeStatementData(
                revenueItems = revItems,
                expenseItems = expItems,
                totalRevenue = totalRev,
                totalExpense = totalExp,
                netIncome = netIncome
            )
        }
    }

    // Balance Sheet (Neraca)
    fun getBalanceSheet(asOfDateMs: Long? = null): Flow<BalanceSheetData> {
        return combine(allAccounts, allPostedJournalLines) { accounts, postedLines ->
            val filteredLines = if (asOfDateMs != null) {
                postedLines.filter { it.dateEpochMs <= asOfDateMs }
            } else {
                postedLines
            }

            val debitMap = mutableMapOf<Long, Double>()
            val creditMap = mutableMapOf<Long, Double>()
            for (line in filteredLines) {
                debitMap[line.accountId] = (debitMap[line.accountId] ?: 0.0) + line.debitAmount
                creditMap[line.accountId] = (creditMap[line.accountId] ?: 0.0) + line.creditAmount
            }

            val balanceItems = accounts.map { acc ->
                val dr = debitMap[acc.id] ?: 0.0
                val cr = creditMap[acc.id] ?: 0.0
                val net = if (acc.normalBalance == NormalBalance.DEBIT) {
                    dr - cr
                } else {
                    cr - dr
                }
                AccountBalanceItem(acc, dr, cr, net)
            }

            val assetItems = balanceItems.filter { it.account.category == AccountCategory.ASSET }
            val liabilityItems = balanceItems.filter { it.account.category == AccountCategory.LIABILITY }
            val equityItems = balanceItems.filter { it.account.category == AccountCategory.EQUITY }

            val revItems = balanceItems.filter { it.account.category == AccountCategory.REVENUE }
            val expItems = balanceItems.filter { it.account.category == AccountCategory.EXPENSE || it.account.category == AccountCategory.COGS }
            val netIncomeCurrentPeriod = revItems.sumOf { it.netBalance } - expItems.sumOf { it.netBalance }

            val totalAssets = assetItems.sumOf { it.netBalance }
            val totalLiabilities = liabilityItems.sumOf { it.netBalance }

            // Special handling for Equity:
            // Standard equity accounts (Normal Balance CREDIT) add to equity
            // Donation/Charity (Normal Balance DEBIT) and Prive (DEBIT) REDUCE equity
            var totalEquityWithoutNetIncome = 0.0
            for (eq in equityItems) {
                if (eq.account.normalBalance == NormalBalance.CREDIT) {
                    totalEquityWithoutNetIncome += eq.netBalance
                } else {
                    totalEquityWithoutNetIncome -= eq.netBalance
                }
            }

            val totalEquity = totalEquityWithoutNetIncome + netIncomeCurrentPeriod
            val discrepancy = abs(totalAssets - (totalLiabilities + totalEquity))
            val isBalanced = discrepancy < 0.01

            BalanceSheetData(
                assetItems = assetItems,
                liabilityItems = liabilityItems,
                equityItems = equityItems,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                totalEquityWithoutNetIncome = totalEquityWithoutNetIncome,
                netIncomeCurrentPeriod = netIncomeCurrentPeriod,
                totalEquity = totalEquity,
                isBalanced = isBalanced,
                discrepancy = discrepancy
            )
        }
    }

    // Cash Flow Statement (Laporan Arus Kas Langsung)
    fun getCashFlowStatement(startDateMs: Long? = null, endDateMs: Long? = null): Flow<CashFlowStatementData> {
        return combine(allAccounts, journalDao.getJournalsWithLines()) { accounts, journals ->
            val cashAccountIds = accounts
                .filter { it.code in listOf("1001", "1002", "1110", "1120") }
                .map { it.id }
                .toSet()

            var opIn = 0.0
            var opOut = 0.0
            var finIn = 0.0
            var finOut = 0.0
            val cashItems = mutableListOf<CashFlowItem>()

            val filteredJournals = journals.filter { j ->
                j.entry.isPosted && !j.entry.isReversed &&
                (startDateMs == null || j.entry.dateEpochMs >= startDateMs) &&
                (endDateMs == null || j.entry.dateEpochMs <= endDateMs)
            }

            for (j in filteredJournals) {
                val cashLines = j.lines.filter { it.accountId in cashAccountIds }
                if (cashLines.isEmpty()) continue

                val netCashDr = cashLines.sumOf { it.debitAmount }
                val netCashCr = cashLines.sumOf { it.creditAmount }

                val nonCashLines = j.lines.filter { it.accountId !in cashAccountIds }
                val targetAccountIds = nonCashLines.map { it.accountId }.toSet()
                val targetAccounts = accounts.filter { it.id in targetAccountIds }

                val isFinancing = targetAccounts.any {
                    it.category == AccountCategory.EQUITY || it.code in listOf("2001", "2110")
                }

                if (netCashDr > 0) {
                    if (isFinancing) {
                        finIn += netCashDr
                        cashItems.add(CashFlowItem(j.entry.dateEpochMs, j.entry.description, "Pendanaan", netCashDr, 0.0))
                    } else {
                        opIn += netCashDr
                        cashItems.add(CashFlowItem(j.entry.dateEpochMs, j.entry.description, "Operasi", netCashDr, 0.0))
                    }
                }
                if (netCashCr > 0) {
                    if (isFinancing) {
                        finOut += netCashCr
                        cashItems.add(CashFlowItem(j.entry.dateEpochMs, j.entry.description, "Pendanaan", 0.0, netCashCr))
                    } else {
                        opOut += netCashCr
                        cashItems.add(CashFlowItem(j.entry.dateEpochMs, j.entry.description, "Operasi", 0.0, netCashCr))
                    }
                }
            }

            val netOp = opIn - opOut
            val netFin = finIn - finOut
            val netChange = netOp + netFin

            CashFlowStatementData(
                operatingIn = opIn,
                operatingOut = opOut,
                netOperating = netOp,
                financingIn = finIn,
                financingOut = finOut,
                netFinancing = netFin,
                netCashChange = netChange,
                cashItems = cashItems.sortedByDescending { it.dateEpochMs }
            )
        }
    }

    // Core Automatic Double-Entry Journal Generator
    suspend fun postTransaction(
        tx: TransactionRecord,
        customLines: List<Pair<Long, Pair<Double, Double>>>? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val txId = transactionDao.insertTransaction(tx.copy(status = TransactionStatus.POSTED))
            val entryNumber = "JU-${System.currentTimeMillis() % 1000000}"
            val effectiveDescription = if (tx.referenceNumber in listOf("K", "R") && !tx.description.contains("[${tx.referenceNumber}]")) {
                "[${tx.referenceNumber}] ${tx.description}"
            } else {
                tx.description
            }

            val journalEntry = JournalEntry(
                transactionId = txId,
                entryNumber = entryNumber,
                dateEpochMs = tx.dateEpochMs,
                description = effectiveDescription,
                isPosted = true,
                isReversed = false
            )
            val journalId = journalDao.insertJournalEntry(journalEntry)

            val lines = mutableListOf<JournalLine>()

            if (customLines != null && customLines.isNotEmpty()) {
                for (pair in customLines) {
                    val accId = pair.first
                    val (dr, cr) = pair.second
                    lines.add(
                        JournalLine(
                            journalId = journalId,
                            accountId = accId,
                            debitAmount = dr,
                            creditAmount = cr,
                            memo = tx.description
                        )
                    )
                }
            } else {
                val bcaAcc = accountDao.getAccountByCode("1002") ?: accountDao.getAccountByCode("1110")
                val kasAcc = accountDao.getAccountByCode("1001") ?: accountDao.getAccountByCode("1120")
                val defCashId = bcaAcc?.id ?: kasAcc?.id ?: 1L

                when (tx.type) {
                    TransactionType.REVENUE -> {
                        val debitAccId = tx.debitAccountId ?: defCashId
                        val salesAcc = accountDao.getAccountByCode("4001") ?: accountDao.getAccountByCode("4110")
                        val creditAccId = tx.creditAccountId ?: (salesAcc?.id ?: 1L)
                        lines.add(JournalLine(journalId = journalId, accountId = debitAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = tx.description))
                        lines.add(JournalLine(journalId = journalId, accountId = creditAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                    TransactionType.EXPENSE -> {
                        val expAcc = accountDao.getAccountByCode("6003") ?: accountDao.getAccountByCode("5210")
                        val debitAccId = tx.debitAccountId ?: (expAcc?.id ?: 1L)
                        val creditAccId = tx.creditAccountId ?: defCashId
                        lines.add(JournalLine(journalId = journalId, accountId = debitAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = tx.description))
                        lines.add(JournalLine(journalId = journalId, accountId = creditAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                    TransactionType.PURCHASE_INVENTORY -> {
                        val invAcc = accountDao.getAccountByCode("1201") ?: accountDao.getAccountByCode("1140")
                        val invAccId = tx.debitAccountId ?: (invAcc?.id ?: 1L)
                        val paymentAccId = tx.creditAccountId ?: defCashId
                        lines.add(JournalLine(journalId = journalId, accountId = invAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Beli Persediaan: ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = paymentAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))

                        val prevBalance = inventoryDao.getLatestLog()?.endingBalance ?: 0.0
                        val newBalance = prevBalance + tx.amount
                        inventoryDao.insertLog(
                            InventoryLog(
                                dateEpochMs = tx.dateEpochMs,
                                type = InventoryMutationType.PURCHASE,
                                amount = tx.amount,
                                endingBalance = newBalance,
                                journalId = journalId,
                                transactionId = txId,
                                notes = tx.description
                            )
                        )
                    }
                    TransactionType.USE_INVENTORY -> {
                        val hppAcc = accountDao.getAccountByCode("5001") ?: accountDao.getAccountByCode("5110")
                        val invAcc = accountDao.getAccountByCode("1201") ?: accountDao.getAccountByCode("1140")
                        val hppAccId = hppAcc?.id ?: 1L
                        val invAccId = invAcc?.id ?: 1L
                        lines.add(JournalLine(journalId = journalId, accountId = hppAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Pemakaian Persediaan (HPP): ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = invAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))

                        val prevBalance = inventoryDao.getLatestLog()?.endingBalance ?: 0.0
                        val newBalance = (prevBalance - tx.amount).coerceAtLeast(0.0)
                        inventoryDao.insertLog(
                            InventoryLog(
                                dateEpochMs = tx.dateEpochMs,
                                type = InventoryMutationType.USAGE,
                                amount = tx.amount,
                                endingBalance = newBalance,
                                journalId = journalId,
                                transactionId = txId,
                                notes = tx.description
                            )
                        )
                    }
                    TransactionType.ADJUST_INVENTORY -> {
                        val invAcc = accountDao.getAccountByCode("1201") ?: accountDao.getAccountByCode("1140")
                        val hppAcc = accountDao.getAccountByCode("5001") ?: accountDao.getAccountByCode("5110")
                        val invAccId = invAcc?.id ?: 1L
                        val hppAccId = hppAcc?.id ?: 1L
                        val prevBalance = inventoryDao.getLatestLog()?.endingBalance ?: 0.0
                        val diff = tx.amount - prevBalance

                        if (diff < 0) {
                            val decrease = abs(diff)
                            lines.add(JournalLine(journalId = journalId, accountId = hppAccId, debitAmount = decrease, creditAmount = 0.0, memo = "Penyesuaian Persediaan (Turun): ${tx.description}"))
                            lines.add(JournalLine(journalId = journalId, accountId = invAccId, debitAmount = 0.0, creditAmount = decrease, memo = tx.description))
                        } else {
                            val otherRev = accountDao.getAccountByCode("4002") ?: accountDao.getAccountByCode("4120")
                            val otherRevId = otherRev?.id ?: 1L
                            lines.add(JournalLine(journalId = journalId, accountId = invAccId, debitAmount = diff, creditAmount = 0.0, memo = "Penyesuaian Persediaan (Naik): ${tx.description}"))
                            lines.add(JournalLine(journalId = journalId, accountId = otherRevId, debitAmount = 0.0, creditAmount = diff, memo = tx.description))
                        }

                        inventoryDao.insertLog(
                            InventoryLog(
                                dateEpochMs = tx.dateEpochMs,
                                type = InventoryMutationType.ADJUSTMENT,
                                amount = tx.amount,
                                endingBalance = tx.amount,
                                journalId = journalId,
                                transactionId = txId,
                                notes = tx.description
                            )
                        )
                    }
                    TransactionType.DONATION -> {
                        // CRITICAL: Charity/Donasi adalah kontra-ekuitas
                        val charityAcc = accountDao.getAccountByCode("3003") ?: accountDao.getAccountByCode("3130")
                        val charityAccId = tx.debitAccountId ?: (charityAcc?.id ?: 1L)
                        val cashAccId = tx.creditAccountId ?: defCashId
                        lines.add(JournalLine(journalId = journalId, accountId = charityAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Charity Pemilik (Pengurang Ekuitas): ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = cashAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                    TransactionType.PAY_DEBT -> {
                        val debtAcc = accountDao.getAccountByCode("2001") ?: accountDao.getAccountByCode("2110")
                        val debtAccId = tx.debitAccountId ?: (debtAcc?.id ?: 1L)
                        val cashAccId = tx.creditAccountId ?: defCashId
                        lines.add(JournalLine(journalId = journalId, accountId = debtAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Bayar Hutang: ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = cashAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                    TransactionType.RECEIVE_RECEIVABLE -> {
                        val cashAccId = tx.debitAccountId ?: defCashId
                        val recAcc = accountDao.getAccountByCode("1101") ?: accountDao.getAccountByCode("1130")
                        val recAccId = tx.creditAccountId ?: (recAcc?.id ?: 1L)
                        lines.add(JournalLine(journalId = journalId, accountId = cashAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Terima Piutang: ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = recAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                    TransactionType.OWNER_CAPITAL -> {
                        val cashAccId = tx.debitAccountId ?: defCashId
                        val eqAcc = accountDao.getAccountByCode("3001") ?: accountDao.getAccountByCode("3110")
                        val eqAccId = eqAcc?.id ?: 1L
                        lines.add(JournalLine(journalId = journalId, accountId = cashAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Setor Modal: ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = eqAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                    TransactionType.OWNER_DRAW -> {
                        val drawAcc = accountDao.getAccountByCode("3002") ?: accountDao.getAccountByCode("3140")
                        val drawAccId = tx.debitAccountId ?: (drawAcc?.id ?: 1L)
                        val cashAccId = tx.creditAccountId ?: defCashId
                        lines.add(JournalLine(journalId = journalId, accountId = drawAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Prive Pemilik (Pengurang Ekuitas): ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = cashAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                    TransactionType.TRANSFER -> {
                        val bcaAcc = accountDao.getAccountByCode("1002") ?: accountDao.getAccountByCode("1110")
                        val kasAcc = accountDao.getAccountByCode("1001") ?: accountDao.getAccountByCode("1120")
                        val targetAccId = tx.debitAccountId ?: (bcaAcc?.id ?: 1L)
                        val sourceAccId = tx.creditAccountId ?: (kasAcc?.id ?: defCashId)
                        lines.add(JournalLine(journalId = journalId, accountId = targetAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = "Transfer Masuk: ${tx.description}"))
                        lines.add(JournalLine(journalId = journalId, accountId = sourceAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = "Transfer Keluar: ${tx.description}"))
                    }
                    TransactionType.INITIAL_BALANCE, TransactionType.EXCEL_MIGRATION -> {
                        val debitAccId = tx.debitAccountId ?: defCashId
                        val obeAcc = accountDao.getAccountByCode("3999") ?: accountDao.getAccountByCode("3120")
                        val creditAccId = tx.creditAccountId ?: (obeAcc?.id ?: 1L)
                        lines.add(JournalLine(journalId = journalId, accountId = debitAccId, debitAmount = tx.amount, creditAmount = 0.0, memo = tx.description))
                        lines.add(JournalLine(journalId = journalId, accountId = creditAccId, debitAmount = 0.0, creditAmount = tx.amount, memo = tx.description))
                    }
                }
            }

            // Strict double-entry balance check
            val sumDr = lines.sumOf { it.debitAmount }
            val sumCr = lines.sumOf { it.creditAmount }
            if (abs(sumDr - sumCr) > 0.001) {
                return@withContext Result.failure(IllegalStateException("Ketidakseimbangan akuntansi: Total Debit ($sumDr) != Total Kredit ($sumCr)"))
            }

            journalDao.insertJournalLines(lines)
            Result.success(txId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cancel Transaction with Reversing Entry
    suspend fun cancelTransaction(transactionId: Long, reason: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@withContext Result.failure(IllegalArgumentException("Transaksi tidak ditemukan"))

            if (tx.status == TransactionStatus.CANCELLED) {
                return@withContext Result.failure(IllegalStateException("Transaksi sudah pernah dibatalkan"))
            }

            val origJournal = journalDao.getJournalByTransactionId(transactionId)
            if (origJournal != null) {
                val origLines = journalDao.getLinesForJournal(origJournal.id)

                journalDao.updateJournalEntry(origJournal.copy(isReversed = true))

                val revEntryNumber = "${origJournal.entryNumber}-REV"
                val revJournal = JournalEntry(
                    transactionId = transactionId,
                    entryNumber = revEntryNumber,
                    dateEpochMs = System.currentTimeMillis(),
                    description = "PEMBATALAN (${origJournal.description}): $reason",
                    isPosted = true,
                    isReversed = false,
                    reversalOfEntryId = origJournal.id
                )
                val revJournalId = journalDao.insertJournalEntry(revJournal)

                val revLines = origLines.map { line ->
                    JournalLine(
                        journalId = revJournalId,
                        accountId = line.accountId,
                        debitAmount = line.creditAmount, // Swapped!
                        creditAmount = line.debitAmount, // Swapped!
                        memo = "Pembalik baris ${line.id}: $reason"
                    )
                }
                journalDao.insertJournalLines(revLines)

                if (tx.type == TransactionType.PURCHASE_INVENTORY || tx.type == TransactionType.USE_INVENTORY) {
                    val prevBalance = inventoryDao.getLatestLog()?.endingBalance ?: 0.0
                    val revBalance = if (tx.type == TransactionType.PURCHASE_INVENTORY) {
                        (prevBalance - tx.amount).coerceAtLeast(0.0)
                    } else {
                        prevBalance + tx.amount
                    }
                    inventoryDao.insertLog(
                        InventoryLog(
                            dateEpochMs = System.currentTimeMillis(),
                            type = InventoryMutationType.ADJUSTMENT,
                            amount = tx.amount,
                            endingBalance = revBalance,
                            journalId = revJournalId,
                            transactionId = transactionId,
                            notes = "Pembatalan transaksi ${tx.txNumber}: $reason"
                        )
                    )
                }
            }

            transactionDao.updateTransaction(
                tx.copy(
                    status = TransactionStatus.CANCELLED,
                    notes = if (tx.notes.isBlank()) "Dibatalkan: $reason" else "${tx.notes} | Dibatalkan: $reason"
                )
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDraftTransaction(tx: TransactionRecord): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(tx.copy(status = TransactionStatus.DRAFT))
    }

    suspend fun postDraftTransaction(transactionId: Long): Result<Long> = withContext(Dispatchers.IO) {
        val draft = transactionDao.getTransactionById(transactionId)
            ?: return@withContext Result.failure(IllegalArgumentException("Draft tidak ditemukan"))
        postTransaction(draft.copy(status = TransactionStatus.POSTED))
    }

    suspend fun deleteDraftTransaction(transactionId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        val draft = transactionDao.getTransactionById(transactionId)
            ?: return@withContext Result.failure(IllegalArgumentException("Draft tidak ditemukan"))
        if (draft.status != TransactionStatus.DRAFT) {
            return@withContext Result.failure(IllegalStateException("Hanya draft yang dapat dihapus secara langsung"))
        }
        transactionDao.deleteTransaction(draft)
        Result.success(true)
    }

    // ==========================================
    // PHASE 2: OPENING BALANCE BATCH & POSTING
    // ==========================================

    data class OpeningBalanceItemInput(
        val accountId: Long,
        val debitAmount: Double,
        val creditAmount: Double,
        val notes: String = ""
    )

    /**
     * Posting opening balance secara atomik dalam database transaction.
     * Mencegah duplikasi posting untuk cutoff yang sama.
     */
    suspend fun postOpeningBalanceBatch(
        cutoffDateEpochMs: Long,
        startDateEpochMs: Long,
        description: String,
        items: List<OpeningBalanceItemInput>,
        isDraft: Boolean = false
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // 1. Validasi pencegahan posting ganda
            if (!isDraft) {
                val existingPosted = openingBalanceDao.getPostedBatchForCutoff(cutoffDateEpochMs)
                if (existingPosted != null) {
                    return@withContext Result.failure(
                        IllegalStateException("Saldo awal untuk tanggal cut-off ${FormatUtils.formatDate(cutoffDateEpochMs)} sudah pernah diposting dalam batch ${existingPosted.batchNumber}!")
                    )
                }
            }

            val totalDr = items.sumOf { it.debitAmount }
            val totalCr = items.sumOf { it.creditAmount }

            // 2. Validasi keseimbangan debit dan kredit
            if (abs(totalDr - totalCr) > 0.001) {
                return@withContext Result.failure(
                    IllegalStateException("Saldo awal tidak seimbang! Total Debit (${FormatUtils.formatRupiah(totalDr)}) tidak sama dengan Total Kredit (${FormatUtils.formatRupiah(totalCr)}). Selisih: ${FormatUtils.formatRupiah(abs(totalDr - totalCr))}")
                )
            }

            // 3. Eksekusi atomik dalam transaction Room
            database.withTransaction {
                val batchNumber = "OB-${SimpleDateFormat("yyyyMMdd", Locale("id", "ID")).format(cutoffDateEpochMs)}-${System.currentTimeMillis() % 1000}"
                
                var journalId: Long? = null
                if (!isDraft) {
                    val entryNumber = "JU-$batchNumber"
                    val journalEntry = JournalEntry(
                        entryNumber = entryNumber,
                        dateEpochMs = cutoffDateEpochMs,
                        description = description.ifBlank { "Jurnal Saldo Awal Cut-Off ${FormatUtils.formatDate(cutoffDateEpochMs)}" },
                        isPosted = true,
                        isReversed = false
                    )
                    journalId = journalDao.insertJournalEntry(journalEntry)

                    val journalLines = items.map { item ->
                        JournalLine(
                            journalId = journalId,
                            accountId = item.accountId,
                            debitAmount = item.debitAmount,
                            creditAmount = item.creditAmount,
                            memo = item.notes.ifBlank { "Saldo Awal ${FormatUtils.formatDate(cutoffDateEpochMs)}" }
                        )
                    }
                    journalDao.insertJournalLines(journalLines)

                    // Buat transaction record untuk audit trail
                    transactionDao.insertTransaction(
                        TransactionRecord(
                            txNumber = "TX-$batchNumber",
                            dateEpochMs = cutoffDateEpochMs,
                            description = description.ifBlank { "Saldo Awal Cut-Off ${FormatUtils.formatDate(cutoffDateEpochMs)}" },
                            type = TransactionType.INITIAL_BALANCE,
                            status = TransactionStatus.POSTED,
                            amount = totalDr,
                            referenceNumber = batchNumber,
                            notes = "Posting Saldo Awal: Total Dr=Cr ${FormatUtils.formatRupiah(totalDr)}"
                        )
                    )
                }

                val batch = OpeningBalanceBatch(
                    batchNumber = batchNumber,
                    cutoffDateEpochMs = cutoffDateEpochMs,
                    startDateEpochMs = startDateEpochMs,
                    description = description,
                    totalDebit = totalDr,
                    totalCredit = totalCr,
                    isPosted = !isDraft,
                    journalEntryId = journalId
                )
                val batchId = openingBalanceDao.insertBatch(batch)

                val details = items.map { item ->
                    OpeningBalanceDetail(
                        batchId = batchId,
                        accountId = item.accountId,
                        debitAmount = item.debitAmount,
                        creditAmount = item.creditAmount,
                        notes = item.notes
                    )
                }
                openingBalanceDao.insertDetails(details)

                Result.success(batchId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestPostedOpeningBatch(): OpeningBalanceBatch? = withContext(Dispatchers.IO) {
        openingBalanceDao.getLatestPostedBatch()
    }

    suspend fun getOpeningDetailsForBatch(batchId: Long): List<OpeningBalanceDetail> = withContext(Dispatchers.IO) {
        openingBalanceDao.getDetailsForBatch(batchId)
    }

    // Validated Default Opening Balance per 31 Oktober 2026
    suspend fun setupValidatedInitialBalance(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            AppDatabase.populateInitialAccounts(database)

            // Cut-off 31 Oktober 2026
            val cal = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 0)
            }
            val cutOffEpochMs = cal.timeInMillis

            val calStart = Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startEpochMs = calStart.timeInMillis

            // Cek apakah batch cutoff ini sudah pernah diposting dengan nilai yang benar (0.94)
            val existing = openingBalanceDao.getPostedBatchForCutoff(cutOffEpochMs)
            if (existing != null) {
                if (abs(existing.totalDebit - 0.94) < 0.0001) {
                    return@withContext Result.success(existing.id)
                } else {
                    // Nilai lama belum sesuai reset yang diminta, bersihkan dan perbarui
                    existing.journalEntryId?.let { jId ->
                        database.openHelper.writableDatabase.execSQL("DELETE FROM journal_lines WHERE journalId = $jId")
                        database.openHelper.writableDatabase.execSQL("DELETE FROM journal_entries WHERE id = $jId")
                    }
                    database.openHelper.writableDatabase.execSQL("DELETE FROM transactions WHERE type = 'INITIAL_BALANCE'")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM opening_balance_details WHERE batchId = ${existing.id}")
                    openingBalanceDao.deleteBatch(existing)
                }
            }

            val bcaAccount = accountDao.getAccountByCode("1002") ?: accountDao.getAccountByCode("1110")
                ?: return@withContext Result.failure(IllegalStateException("Akun Bank BCA tidak ditemukan"))
            val modalAccount = accountDao.getAccountByCode("3001") ?: accountDao.getAccountByCode("3110")
                ?: return@withContext Result.failure(IllegalStateException("Akun Modal Pemilik tidak ditemukan"))

            val bcaDebit = 0.94
            val modalCredit = 0.94

            val items = listOf(
                OpeningBalanceItemInput(accountId = bcaAccount.id, debitAmount = bcaDebit, creditAmount = 0.0, notes = "Saldo Awal Bank BCA (1002)"),
                OpeningBalanceItemInput(accountId = modalAccount.id, debitAmount = 0.0, creditAmount = modalCredit, notes = "Saldo Awal Modal Pemilik (3001)")
            )

            postOpeningBalanceBatch(
                cutoffDateEpochMs = cutOffEpochMs,
                startDateEpochMs = startEpochMs,
                description = "Saldo Awal Cut-Off 31 Oktober 2026",
                items = items,
                isDraft = false
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetAllOpeningBalancesToStandard(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            AppDatabase.populateInitialAccounts(database)

            val cal = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 0)
            }
            val cutOffEpochMs = cal.timeInMillis

            val calStart = Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startEpochMs = calStart.timeInMillis

            val allBatches = openingBalanceDao.getAllBatchesList()
            for (batch in allBatches) {
                batch.journalEntryId?.let { jId ->
                    database.openHelper.writableDatabase.execSQL("DELETE FROM journal_lines WHERE journalId = $jId")
                    database.openHelper.writableDatabase.execSQL("DELETE FROM journal_entries WHERE id = $jId")
                }
                database.openHelper.writableDatabase.execSQL("DELETE FROM opening_balance_details WHERE batchId = ${batch.id}")
                openingBalanceDao.deleteBatch(batch)
            }
            database.openHelper.writableDatabase.execSQL("DELETE FROM transactions WHERE type = 'INITIAL_BALANCE'")

            val bcaAccount = accountDao.getAccountByCode("1002") ?: accountDao.getAccountByCode("1110")
                ?: return@withContext Result.failure(IllegalStateException("Akun Bank BCA tidak ditemukan"))
            val modalAccount = accountDao.getAccountByCode("3001") ?: accountDao.getAccountByCode("3110")
                ?: return@withContext Result.failure(IllegalStateException("Akun Modal Pemilik tidak ditemukan"))

            val items = listOf(
                OpeningBalanceItemInput(accountId = bcaAccount.id, debitAmount = 0.94, creditAmount = 0.0, notes = "Saldo Awal Bank BCA (1002)"),
                OpeningBalanceItemInput(accountId = modalAccount.id, debitAmount = 0.0, creditAmount = 0.94, notes = "Saldo Awal Modal Pemilik (3001)")
            )

            postOpeningBalanceBatch(
                cutoffDateEpochMs = cutOffEpochMs,
                startDateEpochMs = startEpochMs,
                description = "Saldo Awal Cut-Off 31 Oktober 2026",
                items = items,
                isDraft = false
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isInitialBalanceSetup(): Boolean = withContext(Dispatchers.IO) {
        val latest = openingBalanceDao.getLatestPostedBatch()
        latest != null
    }

    suspend fun saveExcelArchive(
        fileName: String,
        title: String,
        notes: String,
        totalRows: Int,
        totalDebit: Double,
        totalCredit: Double,
        rawContent: String
    ): Long = withContext(Dispatchers.IO) {
        val archive = ExcelArchive(
            dateEpochMs = System.currentTimeMillis(),
            fileName = fileName,
            title = title,
            notes = notes,
            totalRows = totalRows,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            rawContent = rawContent,
            isMigrated = true
        )
        excelArchiveDao.insertArchive(archive)
    }

    suspend fun exportJournalsToCsv(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("Nomor Jurnal,Tanggal,Keterangan,Kode Akun,Nama Akun,Kategori,Debit (Rp),Kredit (Rp),Memo,Status\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
        val cursor = database.openHelper.readableDatabase.query(
            """
            SELECT 
                je.entryNumber,
                je.dateEpochMs,
                je.description,
                a.code,
                a.name,
                a.category,
                jl.debitAmount,
                jl.creditAmount,
                jl.memo,
                CASE WHEN je.isReversed = 1 THEN 'DIBALIK' ELSE 'POSTED' END AS status
            FROM journal_lines jl
            INNER JOIN journal_entries je ON jl.journalId = je.id
            INNER JOIN accounts a ON jl.accountId = a.id
            WHERE je.isPosted = 1
            ORDER BY je.dateEpochMs ASC, je.id ASC, jl.id ASC
            """.trimIndent()
        )
        while (cursor.moveToNext()) {
            val num = cursor.getString(0)
            val dateMs = cursor.getLong(1)
            val desc = cursor.getString(2).replace(",", ";")
            val code = cursor.getString(3)
            val name = cursor.getString(4).replace(",", ";")
            val cat = cursor.getString(5)
            val dr = cursor.getDouble(6)
            val cr = cursor.getDouble(7)
            val memo = (cursor.getString(8) ?: "").replace(",", ";")
            val status = cursor.getString(9)
            val dateStr = sdf.format(dateMs)
            sb.append("\"$num\",$dateStr,\"$desc\",\"$code\",\"$name\",\"$cat\",$dr,$cr,\"$memo\",\"$status\"\n")
        }
        cursor.close()
        sb.toString()
    }

    suspend fun exportGeneralLedgerToCsv(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("Kode Akun,Nama Akun,Kategori,Tanggal,Nomor Jurnal,Keterangan,Debit (Rp),Kredit (Rp),Saldo Berjalan (Rp)\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))

        val cursor = database.openHelper.readableDatabase.query(
            """
            SELECT 
                a.code,
                a.name,
                a.category,
                a.normalBalance,
                je.dateEpochMs,
                je.entryNumber,
                je.description,
                jl.debitAmount,
                jl.creditAmount
            FROM journal_lines jl
            INNER JOIN journal_entries je ON jl.journalId = je.id
            INNER JOIN accounts a ON jl.accountId = a.id
            WHERE je.isPosted = 1
            ORDER BY a.code ASC, je.dateEpochMs ASC, je.id ASC
            """.trimIndent()
        )

        var currentCode = ""
        var runningBalance = 0.0

        while (cursor.moveToNext()) {
            val code = cursor.getString(0)
            val name = cursor.getString(1).replace(",", ";")
            val cat = cursor.getString(2)
            val norm = cursor.getString(3)
            val dateMs = cursor.getLong(4)
            val entryNum = cursor.getString(5)
            val desc = cursor.getString(6).replace(",", ";")
            val dr = cursor.getDouble(7)
            val cr = cursor.getDouble(8)

            if (code != currentCode) {
                currentCode = code
                runningBalance = 0.0
            }

            if (norm == "DEBIT") {
                runningBalance += (dr - cr)
            } else {
                runningBalance += (cr - dr)
            }

            val dateStr = sdf.format(dateMs)
            sb.append("\"$code\",\"$name\",\"$cat\",$dateStr,\"$entryNum\",\"$desc\",$dr,$cr,$runningBalance\n")
        }
        cursor.close()
        sb.toString()
    }

    suspend fun resetDatabase(): Unit = withContext(Dispatchers.IO) {
        database.clearAllTables()
        AppDatabase.populateInitialAccounts(database)
        setupValidatedInitialBalance()
    }

    suspend fun exportDatabaseBackup(): String = withContext(Dispatchers.IO) {
        val root = org.json.JSONObject()
        root.put("appName", "Diprint Accounting")
        root.put("version", 1)
        root.put("backupDateEpochMs", System.currentTimeMillis())
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.forLanguageTag("id-ID"))
        root.put("backupFormattedDate", sdf.format(System.currentTimeMillis()))

        // 1. Accounts
        val accounts = accountDao.getAllAccountsList()
        val accArr = org.json.JSONArray()
        for (a in accounts) {
            val obj = org.json.JSONObject()
            obj.put("id", a.id)
            obj.put("code", a.code)
            obj.put("name", a.name)
            obj.put("category", a.category.name)
            obj.put("subcategory", a.subcategory)
            obj.put("normalBalance", a.normalBalance.name)
            obj.put("isActive", a.isActive)
            obj.put("isSystem", a.isSystem)
            obj.put("description", a.description)
            accArr.put(obj)
        }
        root.put("accounts", accArr)

        // 2. Transactions
        val txs = transactionDao.getAllTransactionsList()
        val txArr = org.json.JSONArray()
        for (t in txs) {
            val obj = org.json.JSONObject()
            obj.put("id", t.id)
            obj.put("txNumber", t.txNumber)
            obj.put("dateEpochMs", t.dateEpochMs)
            obj.put("description", t.description)
            obj.put("type", t.type.name)
            obj.put("status", t.status.name)
            obj.put("amount", t.amount)
            if (t.debitAccountId != null) obj.put("debitAccountId", t.debitAccountId)
            if (t.creditAccountId != null) obj.put("creditAccountId", t.creditAccountId)
            obj.put("referenceNumber", t.referenceNumber)
            obj.put("notes", t.notes)
            obj.put("createdAt", t.createdAt)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        // 3. Journal Entries
        val entries = journalDao.getAllJournalEntriesList()
        val jeArr = org.json.JSONArray()
        for (e in entries) {
            val obj = org.json.JSONObject()
            obj.put("id", e.id)
            if (e.transactionId != null) obj.put("transactionId", e.transactionId)
            obj.put("entryNumber", e.entryNumber)
            obj.put("dateEpochMs", e.dateEpochMs)
            obj.put("description", e.description)
            obj.put("isPosted", e.isPosted)
            obj.put("isReversed", e.isReversed)
            if (e.reversalOfEntryId != null) obj.put("reversalOfEntryId", e.reversalOfEntryId)
            obj.put("createdAt", e.createdAt)
            jeArr.put(obj)
        }
        root.put("journal_entries", jeArr)

        // 4. Journal Lines
        val lines = journalDao.getAllJournalLinesList()
        val jlArr = org.json.JSONArray()
        for (l in lines) {
            val obj = org.json.JSONObject()
            obj.put("id", l.id)
            obj.put("journalId", l.journalId)
            obj.put("accountId", l.accountId)
            obj.put("debitAmount", l.debitAmount)
            obj.put("creditAmount", l.creditAmount)
            obj.put("memo", l.memo)
            jlArr.put(obj)
        }
        root.put("journal_lines", jlArr)

        // 5. Opening Batches & Details
        val batches = openingBalanceDao.getAllBatchesList()
        val obbArr = org.json.JSONArray()
        for (b in batches) {
            val obj = org.json.JSONObject()
            obj.put("id", b.id)
            obj.put("batchNumber", b.batchNumber)
            obj.put("cutoffDateEpochMs", b.cutoffDateEpochMs)
            obj.put("startDateEpochMs", b.startDateEpochMs)
            obj.put("description", b.description)
            obj.put("totalDebit", b.totalDebit)
            obj.put("totalCredit", b.totalCredit)
            obj.put("isPosted", b.isPosted)
            if (b.journalEntryId != null) obj.put("journalEntryId", b.journalEntryId)
            obj.put("createdAt", b.createdAt)
            obbArr.put(obj)
        }
        root.put("opening_balance_batches", obbArr)

        val obDetails = openingBalanceDao.getAllDetailsList()
        val obdArr = org.json.JSONArray()
        for (d in obDetails) {
            val obj = org.json.JSONObject()
            obj.put("id", d.id)
            obj.put("batchId", d.batchId)
            obj.put("accountId", d.accountId)
            obj.put("debitAmount", d.debitAmount)
            obj.put("creditAmount", d.creditAmount)
            obj.put("notes", d.notes)
            obdArr.put(obj)
        }
        root.put("opening_balance_details", obdArr)

        // 6. Inventory Logs
        val invLogs = inventoryDao.getAllInventoryLogsList()
        val invArr = org.json.JSONArray()
        for (i in invLogs) {
            val obj = org.json.JSONObject()
            obj.put("id", i.id)
            obj.put("dateEpochMs", i.dateEpochMs)
            obj.put("type", i.type.name)
            obj.put("amount", i.amount)
            obj.put("endingBalance", i.endingBalance)
            if (i.journalId != null) obj.put("journalId", i.journalId)
            if (i.transactionId != null) obj.put("transactionId", i.transactionId)
            obj.put("notes", i.notes)
            invArr.put(obj)
        }
        root.put("inventory_logs", invArr)

        // 7. Excel Archives
        val archives = excelArchiveDao.getAllArchivesList()
        val arcArr = org.json.JSONArray()
        for (a in archives) {
            val obj = org.json.JSONObject()
            obj.put("id", a.id)
            obj.put("dateEpochMs", a.dateEpochMs)
            obj.put("fileName", a.fileName)
            obj.put("title", a.title)
            obj.put("notes", a.notes)
            obj.put("totalRows", a.totalRows)
            obj.put("totalDebit", a.totalDebit)
            obj.put("totalCredit", a.totalCredit)
            obj.put("rawContent", a.rawContent)
            obj.put("isMigrated", a.isMigrated)
            obj.put("createdAt", a.createdAt)
            arcArr.put(obj)
        }
        root.put("excel_archives", arcArr)

        root.toString(2)
    }

    suspend fun importDatabaseBackup(backupJson: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = org.json.JSONObject(backupJson.trim())
            if (!root.has("accounts") || !root.has("journal_entries") || !root.has("journal_lines")) {
                return@withContext Result.failure(IllegalArgumentException("Format berkas cadangan tidak valid (komponen akun/jurnal tidak ditemukan)"))
            }

            // Parse Accounts
            val accArr = root.getJSONArray("accounts")
            val newAccounts = mutableListOf<Account>()
            for (i in 0 until accArr.length()) {
                val obj = accArr.getJSONObject(i)
                newAccounts.add(
                    Account(
                        id = obj.optLong("id", 0L),
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        category = AccountCategory.valueOf(obj.getString("category")),
                        subcategory = obj.optString("subcategory", ""),
                        normalBalance = NormalBalance.valueOf(obj.getString("normalBalance")),
                        isActive = obj.optBoolean("isActive", true),
                        isSystem = obj.optBoolean("isSystem", false),
                        description = obj.optString("description", "")
                    )
                )
            }

            // Parse Transactions
            val txArr = root.optJSONArray("transactions")
            val newTxs = mutableListOf<TransactionRecord>()
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    newTxs.add(
                        TransactionRecord(
                            id = obj.optLong("id", 0L),
                            txNumber = obj.getString("txNumber"),
                            dateEpochMs = obj.getLong("dateEpochMs"),
                            description = obj.getString("description"),
                            type = TransactionType.valueOf(obj.getString("type")),
                            status = TransactionStatus.valueOf(obj.optString("status", TransactionStatus.POSTED.name)),
                            amount = obj.getDouble("amount"),
                            debitAccountId = if (obj.has("debitAccountId") && !obj.isNull("debitAccountId")) obj.getLong("debitAccountId") else null,
                            creditAccountId = if (obj.has("creditAccountId") && !obj.isNull("creditAccountId")) obj.getLong("creditAccountId") else null,
                            referenceNumber = obj.optString("referenceNumber", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Parse Journal Entries
            val jeArr = root.getJSONArray("journal_entries")
            val newEntries = mutableListOf<JournalEntry>()
            for (i in 0 until jeArr.length()) {
                val obj = jeArr.getJSONObject(i)
                newEntries.add(
                    JournalEntry(
                        id = obj.optLong("id", 0L),
                        transactionId = if (obj.has("transactionId") && !obj.isNull("transactionId")) obj.getLong("transactionId") else null,
                        entryNumber = obj.getString("entryNumber"),
                        dateEpochMs = obj.getLong("dateEpochMs"),
                        description = obj.getString("description"),
                        isPosted = obj.optBoolean("isPosted", true),
                        isReversed = obj.optBoolean("isReversed", false),
                        reversalOfEntryId = if (obj.has("reversalOfEntryId") && !obj.isNull("reversalOfEntryId")) obj.getLong("reversalOfEntryId") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Parse Journal Lines
            val jlArr = root.getJSONArray("journal_lines")
            val newLines = mutableListOf<JournalLine>()
            for (i in 0 until jlArr.length()) {
                val obj = jlArr.getJSONObject(i)
                newLines.add(
                    JournalLine(
                        id = obj.optLong("id", 0L),
                        journalId = obj.getLong("journalId"),
                        accountId = obj.getLong("accountId"),
                        debitAmount = obj.optDouble("debitAmount", 0.0),
                        creditAmount = obj.optDouble("creditAmount", 0.0),
                        memo = obj.optString("memo", "")
                    )
                )
            }

            // Parse Opening Batches & Details
            val obbArr = root.optJSONArray("opening_balance_batches")
            val newBatches = mutableListOf<OpeningBalanceBatch>()
            if (obbArr != null) {
                for (i in 0 until obbArr.length()) {
                    val obj = obbArr.getJSONObject(i)
                    newBatches.add(
                        OpeningBalanceBatch(
                            id = obj.optLong("id", 0L),
                            batchNumber = obj.getString("batchNumber"),
                            cutoffDateEpochMs = obj.getLong("cutoffDateEpochMs"),
                            startDateEpochMs = obj.getLong("startDateEpochMs"),
                            description = obj.getString("description"),
                            totalDebit = obj.getDouble("totalDebit"),
                            totalCredit = obj.getDouble("totalCredit"),
                            isPosted = obj.optBoolean("isPosted", false),
                            journalEntryId = if (obj.has("journalEntryId") && !obj.isNull("journalEntryId")) obj.getLong("journalEntryId") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val obdArr = root.optJSONArray("opening_balance_details")
            val newDetails = mutableListOf<OpeningBalanceDetail>()
            if (obdArr != null) {
                for (i in 0 until obdArr.length()) {
                    val obj = obdArr.getJSONObject(i)
                    newDetails.add(
                        OpeningBalanceDetail(
                            id = obj.optLong("id", 0L),
                            batchId = obj.getLong("batchId"),
                            accountId = obj.getLong("accountId"),
                            debitAmount = obj.optDouble("debitAmount", 0.0),
                            creditAmount = obj.optDouble("creditAmount", 0.0),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            // Parse Inventory Logs
            val invArr = root.optJSONArray("inventory_logs")
            val newLogs = mutableListOf<InventoryLog>()
            if (invArr != null) {
                for (i in 0 until invArr.length()) {
                    val obj = invArr.getJSONObject(i)
                    newLogs.add(
                        InventoryLog(
                            id = obj.optLong("id", 0L),
                            dateEpochMs = obj.getLong("dateEpochMs"),
                            type = InventoryMutationType.valueOf(obj.getString("type")),
                            amount = obj.getDouble("amount"),
                            endingBalance = obj.getDouble("endingBalance"),
                            journalId = if (obj.has("journalId") && !obj.isNull("journalId")) obj.getLong("journalId") else null,
                            transactionId = if (obj.has("transactionId") && !obj.isNull("transactionId")) obj.getLong("transactionId") else null,
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            // Parse Excel Archives
            val arcArr = root.optJSONArray("excel_archives")
            val newArchives = mutableListOf<ExcelArchive>()
            if (arcArr != null) {
                for (i in 0 until arcArr.length()) {
                    val obj = arcArr.getJSONObject(i)
                    newArchives.add(
                        ExcelArchive(
                            id = obj.optLong("id", 0L),
                            dateEpochMs = obj.getLong("dateEpochMs"),
                            fileName = obj.getString("fileName"),
                            title = obj.getString("title"),
                            notes = obj.optString("notes", ""),
                            totalRows = obj.getInt("totalRows"),
                            totalDebit = obj.getDouble("totalDebit"),
                            totalCredit = obj.getDouble("totalCredit"),
                            rawContent = obj.getString("rawContent"),
                            isMigrated = obj.optBoolean("isMigrated", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Atomic database restore inside Room transaction
            database.withTransaction {
                database.clearAllTables()
                accountDao.insertAccounts(newAccounts)
                if (newBatches.isNotEmpty()) openingBalanceDao.insertBatches(newBatches)
                if (newDetails.isNotEmpty()) openingBalanceDao.insertDetails(newDetails)
                if (newTxs.isNotEmpty()) transactionDao.insertTransactions(newTxs)
                if (newEntries.isNotEmpty()) journalDao.insertJournalEntries(newEntries)
                if (newLines.isNotEmpty()) journalDao.insertJournalLines(newLines)
                if (newLogs.isNotEmpty()) inventoryDao.insertLogs(newLogs)
                if (newArchives.isNotEmpty()) excelArchiveDao.insertArchives(newArchives)
            }

            Result.success("Cadangan berhasil dipulihkan: ${newAccounts.size} akun, ${newEntries.size} jurnal, ${newTxs.size} transaksi.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
