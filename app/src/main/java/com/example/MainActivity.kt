package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.invoice.ui.InvoiceGeneratorScreen
import com.example.receipt.ui.ReceiptGeneratorScreen
import com.example.ui.components.DiprintBottomNav
import com.example.ui.components.DiprintTopBar
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExcelMigrationScreen
import com.example.ui.screens.InitialBalanceScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LedgerJournalScreen
import com.example.ui.screens.QuickTransactionScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DiprintTheme
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.UiMessage
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: AccountingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DiprintTheme {
                DiprintApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DiprintApp(viewModel: AccountingViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val accountBalances by viewModel.accountBalances.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allJournals by viewModel.allJournals.collectAsStateWithLifecycle()
    val allPostedLines by viewModel.allPostedLines.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val inventoryLogs by viewModel.inventoryLogs.collectAsStateWithLifecycle()
    val incomeStatement by viewModel.incomeStatement.collectAsStateWithLifecycle()
    val balanceSheet by viewModel.balanceSheet.collectAsStateWithLifecycle()
    val cashFlowStatement by viewModel.cashFlowStatement.collectAsStateWithLifecycle()
    val archives by viewModel.archives.collectAsStateWithLifecycle()
    val allOpeningBatches by viewModel.allOpeningBatches.collectAsStateWithLifecycle()
    val accountSearchQuery by viewModel.accountSearchQuery.collectAsStateWithLifecycle()
    val accountCategoryFilter by viewModel.accountCategoryFilter.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept system back button
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.handleBack()
    }

    // Collect snackbar UI messages
    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar("Kesalahan: ${msg.message}")
                is UiMessage.Info -> snackbarHostState.showSnackbar(msg.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentScreen != AppScreen.RECEIPT_GENERATOR && currentScreen != AppScreen.INVOICE_GENERATOR) {
                DiprintTopBar(
                    currentScreen = currentScreen,
                    onBack = if (currentScreen != AppScreen.DASHBOARD) {
                        { viewModel.handleBack() }
                    } else null,
                    onOpenSettings = { viewModel.navigateTo(AppScreen.BACKUP_SETTINGS) },
                    onOpenQuickTransaction = { viewModel.navigateTo(AppScreen.QUICK_TRANSACTION) }
                )
            }
        },
        bottomBar = {
            if (currentScreen != AppScreen.RECEIPT_GENERATOR && currentScreen != AppScreen.INVOICE_GENERATOR && currentScreen != AppScreen.QUICK_TRANSACTION) {
                DiprintBottomNav(
                    currentScreen = currentScreen,
                    onSelectScreen = { screen -> viewModel.navigateTo(screen) }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> {
                    DashboardScreen(
                        balanceSheet = balanceSheet,
                        incomeStatement = incomeStatement,
                        accountBalances = accountBalances,
                        recentTransactions = allTransactions,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onOpenNewTransaction = { viewModel.navigateTo(AppScreen.TRANSACTIONS) }
                    )
                }
                AppScreen.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = allTransactions,
                        accounts = allAccounts,
                        journals = allJournals,
                        selectedStatus = selectedStatusFilter,
                        searchQuery = searchQuery,
                        onStatusFilterChange = { st -> viewModel.setStatusFilter(st) },
                        onSearchChange = { q -> viewModel.setSearchQuery(q) },
                        onPostTransaction = { type, desc, amt, date, dr, cr, ref, notes ->
                            viewModel.postNewTransaction(type, desc, amt, date, dr, cr, ref, notes)
                        },
                        onSaveDraft = { type, desc, amt, date, dr, cr, ref, notes ->
                            viewModel.saveDraftTransaction(type, desc, amt, date, dr, cr, ref, notes)
                        },
                        onPostDraft = { txId -> viewModel.postDraft(txId) },
                        onDeleteDraft = { txId -> viewModel.deleteDraft(txId) },
                        onCancelTransaction = { txId, reason -> viewModel.cancelTransaction(txId, reason) }
                    )
                }
                AppScreen.LEDGER_JOURNAL -> {
                    LedgerJournalScreen(
                        accounts = allAccounts,
                        journals = allJournals,
                        postedLines = allPostedLines
                    )
                }
                AppScreen.REPORTS -> {
                    ReportsScreen(
                        incomeStatement = incomeStatement,
                        balanceSheet = balanceSheet,
                        cashFlow = cashFlowStatement,
                        accountBalances = accountBalances
                    )
                }
                AppScreen.INVENTORY -> {
                    InventoryScreen(
                        accountBalances = accountBalances,
                        inventoryLogs = inventoryLogs,
                        incomeStatement = incomeStatement,
                        onRecordMutation = { type, amt, desc ->
                            viewModel.recordInventoryMutation(type, amt, desc)
                        }
                    )
                }
                AppScreen.INITIAL_BALANCE -> {
                    InitialBalanceScreen(
                        accounts = allAccounts,
                        openingBatches = allOpeningBatches,
                        onPostOpeningBalance = { cutoff, start, desc, items, isDraft ->
                            viewModel.postOpeningBalanceBatch(cutoff, start, desc, items, isDraft)
                        }
                    )
                }
                AppScreen.EXCEL_MIGRATION -> {
                    ExcelMigrationScreen(
                        accounts = allAccounts,
                        archives = archives,
                        onCommitMigration = { fileName, title, notes, rows, rawArchive ->
                            viewModel.recordExcelMigrationBatch(fileName, title, notes, rows, rawArchive)
                        }
                    )
                }
                AppScreen.ACCOUNTS -> {
                    AccountsScreen(
                        accounts = allAccounts,
                        accountBalances = accountBalances,
                        searchQuery = accountSearchQuery,
                        selectedCategory = accountCategoryFilter,
                        onSearchChange = { q -> viewModel.setAccountSearchQuery(q) },
                        onCategoryChange = { cat -> viewModel.setAccountCategoryFilter(cat) },
                        onAddAccount = { code, name, cat, sub, norm, desc ->
                            viewModel.addNewAccount(code, name, cat, sub, norm, desc)
                        },
                        onUpdateAccount = { acc -> viewModel.updateAccount(acc) },
                        onToggleActive = { id, active -> viewModel.toggleAccountActive(id, active) },
                        onDeleteAccount = { acc -> viewModel.deleteAccount(acc) }
                    )
                }
                AppScreen.BACKUP_SETTINGS -> {
                    BackupSettingsScreen(
                        onExportJournalsCsv = { viewModel.exportJournalsCsv() },
                        onExportLedgerCsv = { viewModel.exportLedgerCsv() },
                        onExportBackup = { viewModel.exportDatabaseBackup() },
                        onImportBackup = { json -> viewModel.importDatabaseBackup(json) },
                        onResetDatabase = { viewModel.resetAllData() }
                    )
                }
                AppScreen.RECEIPT_GENERATOR -> {
                    ReceiptGeneratorScreen(
                        onClose = { viewModel.handleBack() }
                    )
                }
                AppScreen.INVOICE_GENERATOR -> {
                    InvoiceGeneratorScreen(
                        onClose = { viewModel.handleBack() }
                    )
                }
                AppScreen.QUICK_TRANSACTION -> {
                    QuickTransactionScreen(
                        onPostBatch = { pNom, pOpt, pDesc, oNom, oOpt, oCat, oDesc, bNom, bOpt, bCat, bDesc, uNom, uDesc ->
                            viewModel.postQuickTransactionBatch(
                                pNom, pOpt, pDesc,
                                oNom, oOpt, oCat, oDesc,
                                bNom, bOpt, bCat, bDesc,
                                uNom, uDesc
                            ) {
                                viewModel.handleBack()
                            }
                        },
                        onCancel = { viewModel.handleBack() }
                    )
                }
            }
        }
    }
}
