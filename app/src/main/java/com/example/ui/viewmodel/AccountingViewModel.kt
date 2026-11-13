package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.Account
import com.example.data.model.AccountCategory
import com.example.data.model.ExcelArchive
import com.example.data.model.InventoryLog
import com.example.data.model.InventoryMutationType
import com.example.data.model.JournalLineDetail
import com.example.data.model.JournalWithLines
import com.example.data.model.NormalBalance
import com.example.data.model.OpeningBalanceBatch
import com.example.data.model.OpeningBalanceDetail
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.data.repository.AccountBalanceItem
import com.example.data.repository.AccountingRepository
import com.example.data.repository.BalanceSheetData
import com.example.data.repository.CashFlowStatementData
import com.example.data.repository.IncomeStatementData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    DASHBOARD("Beranda"),
    TRANSACTIONS("Transaksi"),
    LEDGER_JOURNAL("Jurnal & Buku Besar"),
    REPORTS("Laporan Keuangan"),
    INVENTORY("Persediaan"),
    ACCOUNTS("Daftar Akun"),
    INITIAL_BALANCE("Saldo Awal"),
    EXCEL_MIGRATION("Migrasi Excel"),
    BACKUP_SETTINGS("Pengaturan & Backup"),
    RECEIPT_GENERATOR("Cetak Nota & Struk"),
    INVOICE_GENERATOR("Invoice Generator"),
    QUICK_TRANSACTION("Transaksi Cepat")
}

sealed class UiMessage {
    data class Success(val message: String) : UiMessage()
    data class Error(val message: String) : UiMessage()
    data class Info(val message: String) : UiMessage()
}

class AccountingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = AccountingRepository(database)

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<AppScreen>()

    private val _uiMessage = MutableSharedFlow<UiMessage>()
    val uiMessage: SharedFlow<UiMessage> = _uiMessage.asSharedFlow()

    // Transaction filter
    private val _selectedStatusFilter = MutableStateFlow<TransactionStatus?>(null)
    val selectedStatusFilter: StateFlow<TransactionStatus?> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Accounts Search & Category filter
    private val _accountSearchQuery = MutableStateFlow("")
    val accountSearchQuery: StateFlow<String> = _accountSearchQuery.asStateFlow()

    private val _accountCategoryFilter = MutableStateFlow<AccountCategory?>(null)
    val accountCategoryFilter: StateFlow<AccountCategory?> = _accountCategoryFilter.asStateFlow()

    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccounts: StateFlow<List<Account>> = repository.activeAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountBalances: StateFlow<List<AccountBalanceItem>> = repository.accountBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionRecord>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJournals: StateFlow<List<JournalWithLines>> = repository.allJournalsWithLines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPostedLines: StateFlow<List<JournalLineDetail>> = repository.allPostedJournalLines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryLogs: StateFlow<List<InventoryLog>> = repository.allInventoryLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archives: StateFlow<List<ExcelArchive>> = repository.allArchives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOpeningBatches: StateFlow<List<OpeningBalanceBatch>> = repository.allOpeningBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeStatement: StateFlow<IncomeStatementData> = repository.getIncomeStatement()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            IncomeStatementData(emptyList(), emptyList(), 0.0, 0.0, 0.0)
        )

    val balanceSheet: StateFlow<BalanceSheetData> = repository.getBalanceSheet()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            BalanceSheetData(emptyList(), emptyList(), emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, true, 0.0)
        )

    val cashFlowStatement: StateFlow<CashFlowStatementData> = repository.getCashFlowStatement()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CashFlowStatementData(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        )

    init {
        viewModelScope.launch {
            repository.setupValidatedInitialBalance()
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        if (_screenHistory.isNotEmpty()) {
            _currentScreen.value = _screenHistory.removeAt(_screenHistory.lastIndex)
            return true
        }
        if (_currentScreen.value != AppScreen.DASHBOARD) {
            _currentScreen.value = AppScreen.DASHBOARD
            return true
        }
        return false
    }

    fun setStatusFilter(status: TransactionStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAccountSearchQuery(query: String) {
        _accountSearchQuery.value = query
    }

    fun setAccountCategoryFilter(category: AccountCategory?) {
        _accountCategoryFilter.value = category
    }

    // ===================================
    // CHART OF ACCOUNTS (MODUL 1)
    // ===================================

    fun addNewAccount(
        code: String,
        name: String,
        category: AccountCategory,
        subcategory: String,
        normalBalance: NormalBalance,
        description: String = ""
    ) {
        viewModelScope.launch {
            if (code.isBlank() || name.isBlank()) {
                _uiMessage.emit(UiMessage.Error("Kode dan nama akun wajib diisi"))
                return@launch
            }
            val existing = repository.getAccountByCode(code.trim())
            if (existing != null) {
                _uiMessage.emit(UiMessage.Error("Kode akun ${code.trim()} sudah digunakan!"))
                return@launch
            }
            val newAcc = Account(
                code = code.trim(),
                name = name.trim(),
                category = category,
                subcategory = subcategory.trim(),
                normalBalance = normalBalance,
                isActive = true,
                isSystem = false,
                description = description.trim()
            )
            repository.insertAccount(newAcc)
            _uiMessage.emit(UiMessage.Success("Akun $code – $name berhasil ditambahkan."))
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            val res = repository.updateAccount(account)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Success("Akun ${account.code} berhasil diperbarui."))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal memperbarui akun"))
            }
        }
    }

    fun toggleAccountActive(accountId: Long, isActive: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleAccountActive(accountId, isActive)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Info("Status akun berhasil diubah menjadi ${if (isActive) "Aktif" else "Nonaktif"}."))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal mengubah status akun"))
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            val res = repository.deleteAccount(account)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Success("Akun ${account.code} berhasil dihapus."))
            } else {
                // Strict rule: shows reason if prevented due to journal history
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal menghapus akun"))
            }
        }
    }

    // ===================================
    // OPENING BALANCE (MODUL 2)
    // ===================================

    fun postOpeningBalanceBatch(
        cutoffDateEpochMs: Long,
        startDateEpochMs: Long,
        description: String,
        items: List<AccountingRepository.OpeningBalanceItemInput>,
        isDraft: Boolean = false
    ) {
        viewModelScope.launch {
            val res = repository.postOpeningBalanceBatch(
                cutoffDateEpochMs = cutoffDateEpochMs,
                startDateEpochMs = startDateEpochMs,
                description = description,
                items = items,
                isDraft = isDraft
            )
            if (res.isSuccess) {
                val actionName = if (isDraft) "Draft saldo awal berhasil disimpan" else "Saldo awal berhasil diposting & jurnal terbentuk"
                _uiMessage.emit(UiMessage.Success(actionName))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal memproses saldo awal"))
            }
        }
    }

    suspend fun getOpeningDetailsForBatch(batchId: Long): List<OpeningBalanceDetail> {
        return repository.getOpeningDetailsForBatch(batchId)
    }

    fun resetOpeningBalancesToStandard() {
        viewModelScope.launch {
            val res = repository.resetAllOpeningBalancesToStandard()
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Success("Saldo awal berhasil direset: Bank BCA Rp0,94 & Modal Pemilik Rp0,94"))
            } else {
                _uiMessage.emit(UiMessage.Error("Gagal mereset saldo awal: ${res.exceptionOrNull()?.message}"))
            }
        }
    }

    // ===================================
    // TRANSAKSI & JURNAL
    // ===================================

    fun postNewTransaction(
        type: TransactionType,
        description: String,
        amount: Double,
        dateEpochMs: Long,
        debitAccountId: Long?,
        creditAccountId: Long?,
        referenceNumber: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiMessage.emit(UiMessage.Error("Nominal transaksi harus lebih besar dari 0"))
                return@launch
            }
            if (description.isBlank()) {
                _uiMessage.emit(UiMessage.Error("Keterangan transaksi tidak boleh kosong"))
                return@launch
            }

            val txNumber = "TX-${System.currentTimeMillis() % 10000000}"
            val tx = TransactionRecord(
                txNumber = txNumber,
                dateEpochMs = dateEpochMs,
                description = description.trim(),
                type = type,
                status = TransactionStatus.POSTED,
                amount = amount,
                debitAccountId = debitAccountId,
                creditAccountId = creditAccountId,
                referenceNumber = referenceNumber.trim(),
                notes = notes.trim()
            )

            val res = repository.postTransaction(tx)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Success("Transaksi $txNumber berhasil diposting & jurnal terbentuk otomatis"))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal memposting transaksi"))
            }
        }
    }

    fun saveDraftTransaction(
        type: TransactionType,
        description: String,
        amount: Double,
        dateEpochMs: Long,
        debitAccountId: Long?,
        creditAccountId: Long?,
        referenceNumber: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiMessage.emit(UiMessage.Error("Nominal transaksi harus lebih besar dari 0"))
                return@launch
            }
            val txNumber = "DFT-${System.currentTimeMillis() % 1000000}"
            val tx = TransactionRecord(
                txNumber = txNumber,
                dateEpochMs = dateEpochMs,
                description = description.ifBlank { "Draft Transaksi" },
                type = type,
                status = TransactionStatus.DRAFT,
                amount = amount,
                debitAccountId = debitAccountId,
                creditAccountId = creditAccountId,
                referenceNumber = referenceNumber,
                notes = notes
            )
            repository.saveDraftTransaction(tx)
            _uiMessage.emit(UiMessage.Success("Draft transaksi $txNumber berhasil disimpan"))
        }
    }

    fun postDraft(transactionId: Long) {
        viewModelScope.launch {
            val res = repository.postDraftTransaction(transactionId)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Success("Draft berhasil diposting ke jurnal umum"))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal memposting draft"))
            }
        }
    }

    fun deleteDraft(transactionId: Long) {
        viewModelScope.launch {
            val res = repository.deleteDraftTransaction(transactionId)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Info("Draft transaksi berhasil dihapus"))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal menghapus draft"))
            }
        }
    }

    fun cancelTransaction(transactionId: Long, reason: String) {
        viewModelScope.launch {
            if (reason.isBlank()) {
                _uiMessage.emit(UiMessage.Error("Alasan pembatalan wajib diisi untuk audit trail"))
                return@launch
            }
            val res = repository.cancelTransaction(transactionId, reason)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Success("Transaksi berhasil dibatalkan dengan jurnal pembalik otomatis"))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal membatalkan transaksi"))
            }
        }
    }

    fun recordInventoryMutation(
        type: InventoryMutationType,
        amount: Double,
        description: String,
        dateEpochMs: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            if (amount <= 0) {
                _uiMessage.emit(UiMessage.Error("Nominal mutasi persediaan harus lebih besar dari 0"))
                return@launch
            }
            val txType = when (type) {
                InventoryMutationType.PURCHASE -> TransactionType.PURCHASE_INVENTORY
                InventoryMutationType.USAGE -> TransactionType.USE_INVENTORY
                InventoryMutationType.ADJUSTMENT -> TransactionType.ADJUST_INVENTORY
            }
            val txNumber = "INV-${System.currentTimeMillis() % 1000000}"
            val tx = TransactionRecord(
                txNumber = txNumber,
                dateEpochMs = dateEpochMs,
                description = description.ifBlank { type.displayName },
                type = txType,
                status = TransactionStatus.POSTED,
                amount = amount,
                notes = "Mutasi Persediaan Nilai Total"
            )
            val res = repository.postTransaction(tx)
            if (res.isSuccess) {
                _uiMessage.emit(UiMessage.Success("${type.displayName} sebesar Rp$amount berhasil dicatat & jurnal dibuat"))
            } else {
                _uiMessage.emit(UiMessage.Error(res.exceptionOrNull()?.message ?: "Gagal mencatat mutasi persediaan"))
            }
        }
    }

    fun recordExcelMigrationBatch(
        fileName: String,
        title: String,
        notes: String,
        rows: List<Pair<TransactionRecord, Pair<Long, Long>>>,
        rawTextArchive: String
    ) {
        viewModelScope.launch {
            var postedCount = 0
            var totalDr = 0.0
            var totalCr = 0.0

            for (item in rows) {
                val (tx, accounts) = item
                val (drAccId, crAccId) = accounts
                val res = repository.postTransaction(
                    tx.copy(
                        debitAccountId = drAccId,
                        creditAccountId = crAccId,
                        status = TransactionStatus.POSTED
                    )
                )
                if (res.isSuccess) {
                    postedCount++
                    totalDr += tx.amount
                    totalCr += tx.amount
                }
            }

            repository.saveExcelArchive(
                fileName = fileName,
                title = title,
                notes = notes,
                totalRows = postedCount,
                totalDebit = totalDr,
                totalCredit = totalCr,
                rawContent = rawTextArchive
            )

            _uiMessage.emit(UiMessage.Success("Migrasi selesai: $postedCount baris transaksi berhasil diimpor & diarsipkan secara read-only"))
        }
    }

    suspend fun exportJournalsCsv(): String = repository.exportJournalsToCsv()

    suspend fun exportLedgerCsv(): String = repository.exportGeneralLedgerToCsv()

    suspend fun exportDatabaseBackup(): String = repository.exportDatabaseBackup()

    suspend fun importDatabaseBackup(backupJson: String): Result<String> {
        val result = repository.importDatabaseBackup(backupJson)
        if (result.isSuccess) {
            _uiMessage.emit(UiMessage.Success(result.getOrDefault("Cadangan database berhasil dipulihkan")))
        } else {
            _uiMessage.emit(UiMessage.Error(result.exceptionOrNull()?.message ?: "Gagal memulihkan cadangan"))
        }
        return result
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetDatabase()
            _uiMessage.emit(UiMessage.Info("Database telah di-reset ke kondisi awal dengan saldo pembukuan 31 Okt 2026"))
        }
    }

    // ===================================
    // TRANSAKSI CEPAT
    // ===================================
    fun postQuickTransactionBatch(
        penjualanNominal: Double,
        penjualanOption: String,
        penjualanDesc: String,
        ongkirNominal: Double,
        ongkirOption: String,
        ongkirHutangCategory: String,
        ongkirDesc: String,
        pembelianNominal: Double,
        pembelianOption: String,
        pembelianHutangCategory: String,
        pembelianDesc: String,
        pemakaianNominal: Double,
        pemakaianDesc: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var postedCount = 0

            val kas = repository.getAccountByCode("1001") ?: repository.getAccountByCode("1120")
            val bank = repository.getAccountByCode("1002") ?: repository.getAccountByCode("1110")
            val piutang = repository.getAccountByCode("1101") ?: repository.getAccountByCode("1130")
            val hutang = repository.getAccountByCode("2001") ?: repository.getAccountByCode("2110")
            val penjualan = repository.getAccountByCode("4001") ?: repository.getAccountByCode("4110")
            val ongkir = repository.getAccountByCode("6001") ?: repository.getAccountByCode("6003")
            val persediaan = repository.getAccountByCode("1201") ?: repository.getAccountByCode("1140")
            val hpp = repository.getAccountByCode("5001") ?: repository.getAccountByCode("5110")

            // 1. Penjualan
            if (penjualanNominal > 0) {
                val debitAcc = when (penjualanOption.uppercase()) {
                    "BANK" -> bank
                    "PIUTANG" -> piutang
                    else -> kas
                }
                val tx = TransactionRecord(
                    txNumber = "TX-SL-${System.currentTimeMillis() % 10000000}",
                    dateEpochMs = now,
                    description = penjualanDesc.ifBlank { "Penjualan Percetakan (${penjualanOption})" }.trim(),
                    type = TransactionType.REVENUE,
                    status = TransactionStatus.POSTED,
                    amount = penjualanNominal,
                    debitAccountId = debitAcc?.id,
                    creditAccountId = penjualan?.id,
                    notes = "Transaksi Cepat Penjualan"
                )
                val res = repository.postTransaction(tx)
                if (res.isSuccess) postedCount++
            }

            // 2. Ongkir
            if (ongkirNominal > 0) {
                val isHutang = ongkirOption.uppercase() == "HUTANG"
                val creditAcc = when (ongkirOption.uppercase()) {
                    "BANK" -> bank
                    "HUTANG" -> hutang
                    else -> kas
                }
                val refNo = if (isHutang) ongkirHutangCategory else ""
                val descPrefix = if (isHutang) "[Kategori $ongkirHutangCategory] " else ""
                val desc = descPrefix + ongkirDesc.ifBlank { "Beban Ongkos Kirim (${ongkirOption})" }.trim()

                val tx = TransactionRecord(
                    txNumber = "TX-OK-${System.currentTimeMillis() % 10000000 + 1}",
                    dateEpochMs = now,
                    description = desc,
                    type = TransactionType.EXPENSE,
                    status = TransactionStatus.POSTED,
                    amount = ongkirNominal,
                    debitAccountId = ongkir?.id,
                    creditAccountId = creditAcc?.id,
                    referenceNumber = refNo,
                    notes = "Transaksi Cepat Ongkir"
                )
                val res = repository.postTransaction(tx)
                if (res.isSuccess) postedCount++
            }

            // 3. Pembelian Persediaan
            if (pembelianNominal > 0) {
                val isHutang = pembelianOption.uppercase() == "HUTANG"
                val creditAcc = when (pembelianOption.uppercase()) {
                    "BANK" -> bank
                    "HUTANG" -> hutang
                    else -> kas
                }
                val refNo = if (isHutang) pembelianHutangCategory else ""
                val descPrefix = if (isHutang) "[Kategori $pembelianHutangCategory] " else ""
                val desc = descPrefix + pembelianDesc.ifBlank { "Pembelian Persediaan (${pembelianOption})" }.trim()

                val tx = TransactionRecord(
                    txNumber = "TX-PB-${System.currentTimeMillis() % 10000000 + 2}",
                    dateEpochMs = now,
                    description = desc,
                    type = TransactionType.PURCHASE_INVENTORY,
                    status = TransactionStatus.POSTED,
                    amount = pembelianNominal,
                    debitAccountId = persediaan?.id,
                    creditAccountId = creditAcc?.id,
                    referenceNumber = refNo,
                    notes = "Transaksi Cepat Pembelian Persediaan"
                )
                val res = repository.postTransaction(tx)
                if (res.isSuccess) postedCount++
            }

            // 4. Pemakaian Persediaan (HPP)
            if (pemakaianNominal > 0) {
                val tx = TransactionRecord(
                    txNumber = "TX-HPP-${System.currentTimeMillis() % 10000000 + 3}",
                    dateEpochMs = now,
                    description = pemakaianDesc.ifBlank { "Pemakaian Persediaan Bahan Cetak (HPP)" }.trim(),
                    type = TransactionType.USE_INVENTORY,
                    status = TransactionStatus.POSTED,
                    amount = pemakaianNominal,
                    debitAccountId = hpp?.id,
                    creditAccountId = persediaan?.id,
                    notes = "Transaksi Cepat Pemakaian Persediaan (HPP)"
                )
                val res = repository.postTransaction(tx)
                if (res.isSuccess) postedCount++
            }

            if (postedCount > 0) {
                _uiMessage.emit(UiMessage.Success("$postedCount Transaksi Cepat berhasil diposting ke Jurnal & Buku Besar."))
                onSuccess()
            } else {
                _uiMessage.emit(UiMessage.Error("Harap isi setidaknya salah satu nominal transaksi."))
            }
        }
    }
}
