package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Account
import com.example.data.model.AccountCategory
import com.example.data.model.JournalWithLines
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.VioletEquity
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionRecord>,
    accounts: List<Account>,
    journals: List<JournalWithLines>,
    selectedStatus: TransactionStatus?,
    searchQuery: String,
    onStatusFilterChange: (TransactionStatus?) -> Unit,
    onSearchChange: (String) -> Unit,
    onPostTransaction: (TransactionType, String, Double, Long, Long?, Long?, String, String) -> Unit,
    onSaveDraft: (TransactionType, String, Double, Long, Long?, Long?, String, String) -> Unit,
    onPostDraft: (Long) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onCancelTransaction: (Long, String) -> Unit
) {
    var showNewTxDialog by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<TransactionRecord?>(null) }
    var txToCancel by remember { mutableStateOf<TransactionRecord?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    val filteredList = transactions.filter { tx ->
        val matchesStatus = selectedStatus == null || tx.status == selectedStatus
        val matchesQuery = searchQuery.isBlank() ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.txNumber.contains(searchQuery, ignoreCase = true) ||
                tx.referenceNumber.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesQuery
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewTxDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_search_input"),
                placeholder = { Text("Cari nomor transaksi atau deskripsi...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { onStatusFilterChange(null) },
                        label = { Text("Semua (${transactions.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                item {
                    val count = transactions.count { it.status == TransactionStatus.POSTED }
                    FilterChip(
                        selected = selectedStatus == TransactionStatus.POSTED,
                        onClick = { onStatusFilterChange(TransactionStatus.POSTED) },
                        label = { Text("Diposting ($count)") }
                    )
                }
                item {
                    val count = transactions.count { it.status == TransactionStatus.DRAFT }
                    FilterChip(
                        selected = selectedStatus == TransactionStatus.DRAFT,
                        onClick = { onStatusFilterChange(TransactionStatus.DRAFT) },
                        label = { Text("Draft ($count)") }
                    )
                }
                item {
                    val count = transactions.count { it.status == TransactionStatus.CANCELLED }
                    FilterChip(
                        selected = selectedStatus == TransactionStatus.CANCELLED,
                        onClick = { onStatusFilterChange(TransactionStatus.CANCELLED) },
                        label = { Text("Dibatalkan ($count)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transactions List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada transaksi yang sesuai",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { tx ->
                        TransactionCard(
                            tx = tx,
                            onClick = { selectedTxForDetail = tx }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // New Transaction Dialog
    if (showNewTxDialog) {
        NewTransactionDialog(
            accounts = accounts,
            onDismiss = { showNewTxDialog = false },
            onPost = { type, desc, amt, date, drAcc, crAcc, ref, notes ->
                onPostTransaction(type, desc, amt, date, drAcc, crAcc, ref, notes)
                showNewTxDialog = false
            },
            onSaveDraft = { type, desc, amt, date, drAcc, crAcc, ref, notes ->
                onSaveDraft(type, desc, amt, date, drAcc, crAcc, ref, notes)
                showNewTxDialog = false
            }
        )
    }

    // Transaction Detail Dialog
    selectedTxForDetail?.let { tx ->
        val associatedJournal = journals.find { it.entry.transactionId == tx.id }
        TransactionDetailDialog(
            tx = tx,
            journal = associatedJournal,
            accounts = accounts,
            onDismiss = { selectedTxForDetail = null },
            onPostDraft = {
                onPostDraft(tx.id)
                selectedTxForDetail = null
            },
            onDeleteDraft = {
                onDeleteDraft(tx.id)
                selectedTxForDetail = null
            },
            onInitiateCancel = {
                txToCancel = tx
                cancelReason = ""
                selectedTxForDetail = null
            }
        )
    }

    // Cancellation Reason Confirmation Dialog (Audit Trail)
    txToCancel?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToCancel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = RoseLoss)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batalkan Transaksi")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Sesuai standar akuntansi double-entry, pembatalan transaksi ${tx.txNumber} akan membuat JURNAL PEMBALIK (Reversing Entry) secara otomatis untuk menjaga audit trail.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Alasan Pembatalan (Wajib)") },
                        placeholder = { Text("Contoh: Salah nominal input / dibatalkan klien") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_reason_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReason.isNotBlank()) {
                            onCancelTransaction(tx.id, cancelReason.trim())
                            txToCancel = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseLoss),
                    enabled = cancelReason.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_cancel")
                ) {
                    Text("Ya, Buat Jurnal Pembalik")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { txToCancel = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun TransactionCard(
    tx: TransactionRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tx_card_${tx.txNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = tx.txNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    val debtCat = when {
                        tx.referenceNumber == "K" || tx.referenceNumber == "R" -> tx.referenceNumber
                        tx.description.contains("[K]") -> "K"
                        tx.description.contains("[R]") -> "R"
                        else -> null
                    }
                    if (debtCat != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (debtCat == "R") Color(0xFFFFE0B2) else Color(0xFFBBDEFB)
                        ) {
                            Text(
                                text = "Hutang \"$debtCat\"",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (debtCat == "R") Color(0xFFE65100) else Color(0xFF0D47A1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = FormatUtils.formatDate(tx.dateEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TransactionStatusBadge(status = tx.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tx.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tx.type == TransactionType.DONATION) VioletEquity else MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = FormatUtils.formatRupiah(tx.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

enum class RevenuePaymentOption(val label: String, val code: String) {
    KAS("Kas", "1001"),
    BANK("Bank", "1002"),
    PIUTANG("Piutang", "1101")
}

enum class DisbursementPaymentOption(val label: String, val code: String) {
    KAS("Kas", "1001"),
    BANK("Bank", "1002"),
    HUTANG("Hutang", "2001")
}

enum class DebtCategoryOption(val symbol: String, val label: String) {
    K("K", "Kategori K"),
    R("R", "Kategori R")
}

enum class CashOrBankOption(val label: String, val code: String) {
    KAS("Kas", "1001"),
    BANK("Bank", "1002")
}

enum class TransferDirectionOption(val label: String, val fromCode: String, val toCode: String) {
    KAS_TO_BANK("Kas ke Bank", "1001", "1002"),
    BANK_TO_KAS("Bank ke Kas", "1002", "1001")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onPost: (TransactionType, String, Double, Long, Long?, Long?, String, String) -> Unit,
    onSaveDraft: (TransactionType, String, Double, Long, Long?, Long?, String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(TransactionType.REVENUE) }
    var selectedRevenueOption by remember { mutableStateOf(RevenuePaymentOption.BANK) }
    var selectedExpensePaymentOption by remember { mutableStateOf(DisbursementPaymentOption.BANK) }
    var selectedPurchasePaymentOption by remember { mutableStateOf(DisbursementPaymentOption.BANK) }
    var selectedPayDebtOption by remember { mutableStateOf(CashOrBankOption.BANK) }
    var selectedExpenseDebtCategory by remember { mutableStateOf(DebtCategoryOption.K) }
    var selectedPurchaseDebtCategory by remember { mutableStateOf(DebtCategoryOption.K) }
    var selectedPayDebtCategory by remember { mutableStateOf(DebtCategoryOption.K) }
    var selectedReceiveReceivableOption by remember { mutableStateOf(CashOrBankOption.BANK) }
    var selectedOwnerDrawOption by remember { mutableStateOf(CashOrBankOption.BANK) }
    var selectedDonationOption by remember { mutableStateOf(CashOrBankOption.BANK) }
    var selectedTransferOption by remember { mutableStateOf(TransferDirectionOption.KAS_TO_BANK) }

    val expenseAccounts = accounts.filter { it.category == AccountCategory.EXPENSE }
    var selectedExpenseAccountId by remember {
        mutableStateOf(expenseAccounts.find { it.code == "6003" }?.id ?: expenseAccounts.firstOrNull()?.id)
    }
    var expenseAccountDropdownExpanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    val referenceNumber = ""
    var notes by remember { mutableStateOf("") }

    val cashAndBankAccounts = accounts.filter { it.code in listOf("1001", "1002", "1110", "1120") }
    var selectedCashAccountId by remember { mutableStateOf(cashAndBankAccounts.find { it.code == "1002" }?.id ?: accounts.firstOrNull()?.id) }

    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Transaksi Baru", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    // Type Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jenis Transaksi") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            TransactionType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.displayName) },
                                    onClick = {
                                        selectedType = t
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Sub-options for Penjualan (Kas, Bank, Piutang)
                if (selectedType == TransactionType.REVENUE) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Metode Penerimaan Penjualan:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RevenuePaymentOption.values().forEach { opt ->
                                        val isSelected = selectedRevenueOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedRevenueOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("revenue_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val targetAcc = when (selectedRevenueOption) {
                                    RevenuePaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    RevenuePaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                    RevenuePaymentOption.PIUTANG -> accounts.find { it.code == "1101" } ?: accounts.find { it.code == "1130" }
                                }
                                val salesAcc = accounts.find { it.code == "4001" } ?: accounts.find { it.code == "4110" }
                                Text(
                                    text = "Jurnal: Debit [${targetAcc?.code ?: ""} - ${targetAcc?.name ?: selectedRevenueOption.label}], Kredit [${salesAcc?.code ?: "4001"} - ${salesAcc?.name ?: "Penjualan"}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Sub-options for Pengeluaran Beban Operasional (Kas, Bank, Hutang)
                if (selectedType == TransactionType.EXPENSE) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Metode Pembayaran Beban:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DisbursementPaymentOption.values().forEach { opt ->
                                        val isSelected = selectedExpensePaymentOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedExpensePaymentOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("expense_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                if (selectedExpensePaymentOption == DisbursementPaymentOption.HUTANG) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Pilih Kategori Hutang:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DebtCategoryOption.values().forEach { cat ->
                                            val isCatSelected = selectedExpenseDebtCategory == cat
                                            FilterChip(
                                                selected = isCatSelected,
                                                onClick = { selectedExpenseDebtCategory = cat },
                                                label = {
                                                    Text(
                                                        text = "Kategori \"${cat.symbol}\"",
                                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("expense_debt_cat_${cat.symbol.lowercase()}"),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }
                                }

                                if (expenseAccounts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = expenseAccountDropdownExpanded,
                                        onExpandedChange = { expenseAccountDropdownExpanded = it }
                                    ) {
                                        val currentExpAcc = accounts.find { it.id == selectedExpenseAccountId } ?: expenseAccounts.firstOrNull()
                                        OutlinedTextField(
                                            value = "${currentExpAcc?.code ?: ""} - ${currentExpAcc?.name ?: "Pilih Akun Beban"}",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Pilihan Akun Beban") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseAccountDropdownExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expenseAccountDropdownExpanded,
                                            onDismissRequest = { expenseAccountDropdownExpanded = false }
                                        ) {
                                            expenseAccounts.forEach { acc ->
                                                DropdownMenuItem(
                                                    text = { Text("${acc.code} - ${acc.name}") },
                                                    onClick = {
                                                        selectedExpenseAccountId = acc.id
                                                        expenseAccountDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                val targetAcc = when (selectedExpensePaymentOption) {
                                    DisbursementPaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    DisbursementPaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                    DisbursementPaymentOption.HUTANG -> accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                                }
                                val expAcc = accounts.find { it.id == selectedExpenseAccountId }
                                    ?: accounts.find { it.code == "6003" }
                                    ?: accounts.find { it.code == "5210" }
                                val expenseDebtTag = if (selectedExpensePaymentOption == DisbursementPaymentOption.HUTANG) " [Kat. ${selectedExpenseDebtCategory.symbol}]" else ""
                                Text(
                                    text = "Jurnal: Debit [${expAcc?.code ?: "6003"} - ${expAcc?.name ?: "Beban"}], Kredit [${targetAcc?.code ?: ""} - ${targetAcc?.name ?: selectedExpensePaymentOption.label}$expenseDebtTag]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Sub-options for Pembelian Persediaan (Kas, Bank, Hutang)
                if (selectedType == TransactionType.PURCHASE_INVENTORY) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Metode Pembayaran Pembelian:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DisbursementPaymentOption.values().forEach { opt ->
                                        val isSelected = selectedPurchasePaymentOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedPurchasePaymentOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("purchase_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                if (selectedPurchasePaymentOption == DisbursementPaymentOption.HUTANG) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Pilih Kategori Hutang:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DebtCategoryOption.values().forEach { cat ->
                                            val isCatSelected = selectedPurchaseDebtCategory == cat
                                            FilterChip(
                                                selected = isCatSelected,
                                                onClick = { selectedPurchaseDebtCategory = cat },
                                                label = {
                                                    Text(
                                                        text = "Kategori \"${cat.symbol}\"",
                                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("purchase_debt_cat_${cat.symbol.lowercase()}"),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                val targetAcc = when (selectedPurchasePaymentOption) {
                                    DisbursementPaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    DisbursementPaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                    DisbursementPaymentOption.HUTANG -> accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                                }
                                val invAcc = accounts.find { it.code == "1201" } ?: accounts.find { it.code == "1140" }
                                val purchaseDebtTag = if (selectedPurchasePaymentOption == DisbursementPaymentOption.HUTANG) " [Kat. ${selectedPurchaseDebtCategory.symbol}]" else ""
                                Text(
                                    text = "Jurnal: Debit [${invAcc?.code ?: "1201"} - ${invAcc?.name ?: "Persediaan"}], Kredit [${targetAcc?.code ?: ""} - ${targetAcc?.name ?: selectedPurchasePaymentOption.label}$purchaseDebtTag]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Sub-options for Pembayaran Hutang (Kas, Bank) + Kategori Hutang K/R
                if (selectedType == TransactionType.PAY_DEBT) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Pilih Kategori Hutang yang Dibayar:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DebtCategoryOption.values().forEach { cat ->
                                        val isCatSelected = selectedPayDebtCategory == cat
                                        FilterChip(
                                            selected = isCatSelected,
                                            onClick = { selectedPayDebtCategory = cat },
                                            label = {
                                                Text(
                                                    text = "Kategori \"${cat.symbol}\"",
                                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("pay_debt_cat_${cat.symbol.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Metode Pembayaran Hutang:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CashOrBankOption.values().forEach { opt ->
                                        val isSelected = selectedPayDebtOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedPayDebtOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("pay_debt_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val targetAcc = when (selectedPayDebtOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                val debtAcc = accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                                Text(
                                    text = "Jurnal: Debit [${debtAcc?.code ?: "2001"} - ${debtAcc?.name ?: "Hutang Usaha"} [Kat. ${selectedPayDebtCategory.symbol}]], Kredit [${targetAcc?.code ?: ""} - ${targetAcc?.name ?: selectedPayDebtOption.label}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Sub-options for Penerimaan Piutang (Kas, Bank)
                if (selectedType == TransactionType.RECEIVE_RECEIVABLE) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Rekening Penerimaan Piutang:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CashOrBankOption.values().forEach { opt ->
                                        val isSelected = selectedReceiveReceivableOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedReceiveReceivableOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("receive_rec_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val targetAcc = when (selectedReceiveReceivableOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                val recAcc = accounts.find { it.code == "1101" } ?: accounts.find { it.code == "1130" }
                                Text(
                                    text = "Jurnal: Debit [${targetAcc?.code ?: ""} - ${targetAcc?.name ?: selectedReceiveReceivableOption.label}], Kredit [${recAcc?.code ?: "1101"} - ${recAcc?.name ?: "Piutang Usaha"}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Sub-options for Prive Pemilik (Kas, Bank)
                if (selectedType == TransactionType.OWNER_DRAW) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Sumber Penarikan Prive:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CashOrBankOption.values().forEach { opt ->
                                        val isSelected = selectedOwnerDrawOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedOwnerDrawOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("owner_draw_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val targetAcc = when (selectedOwnerDrawOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                val drawAcc = accounts.find { it.code == "3002" } ?: accounts.find { it.code == "3140" }
                                Text(
                                    text = "Jurnal: Debit [${drawAcc?.code ?: "3002"} - ${drawAcc?.name ?: "Prive Pemilik"}], Kredit [${targetAcc?.code ?: ""} - ${targetAcc?.name ?: selectedOwnerDrawOption.label}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Sub-options for Donasi Pemilik (Kas, Bank)
                if (selectedType == TransactionType.DONATION) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = VioletEquity.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Sumber Dana Donasi / Charity:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VioletEquity
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CashOrBankOption.values().forEach { opt ->
                                        val isSelected = selectedDonationOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedDonationOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("donation_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VioletEquity,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val targetAcc = when (selectedDonationOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                val charityAcc = accounts.find { it.code == "3003" } ?: accounts.find { it.code == "3130" }
                                Text(
                                    text = "Jurnal: Debit [${charityAcc?.code ?: "3003"} - ${charityAcc?.name ?: "Charity Pemilik"}], Kredit [${targetAcc?.code ?: ""} - ${targetAcc?.name ?: selectedDonationOption.label}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VioletEquity,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Ketentuan Khusus: Donasi dicatat sebagai pengurang Ekuitas Pemilik (Akun ${charityAcc?.code ?: "3003"}), bukan beban operasional.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VioletEquity
                                )
                            }
                        }
                    }
                }

                // Sub-options for Transfer Kas / Bank (Kas ke Bank / Bank ke Kas)
                if (selectedType == TransactionType.TRANSFER) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Arah Transfer Kas / Bank:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TransferDirectionOption.values().forEach { opt ->
                                        val isSelected = selectedTransferOption == opt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedTransferOption = opt },
                                            label = {
                                                Text(
                                                    text = opt.label,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("transfer_opt_${opt.name.lowercase()}"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val kasAcc = accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                val bankAcc = accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                val (debitAcc, creditAcc) = when (selectedTransferOption) {
                                    TransferDirectionOption.KAS_TO_BANK -> Pair(bankAcc, kasAcc)
                                    TransferDirectionOption.BANK_TO_KAS -> Pair(kasAcc, bankAcc)
                                }
                                Text(
                                    text = "Jurnal: Debit [${debitAcc?.code ?: "1002"} - ${debitAcc?.name ?: "Bank"}], Kredit [${creditAcc?.code ?: "1001"} - ${creditAcc?.name ?: "Kas"}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Nominal (Rp)") },
                        placeholder = { Text("Contoh: 150000 atau 500000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_tx_amount"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi / Keterangan") },
                        placeholder = { Text("Contoh: Cetak Brosur A4 PT Maju") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_tx_desc"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Tambahan (Opsional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = FormatUtils.parseRupiahInput(amountInput) ?: 0.0
                    val (drAccountId, crAccountId) = when (selectedType) {
                        TransactionType.REVENUE -> {
                            val targetAcc = when (selectedRevenueOption) {
                                RevenuePaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                RevenuePaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                RevenuePaymentOption.PIUTANG -> accounts.find { it.code == "1101" } ?: accounts.find { it.code == "1130" }
                            }
                            val salesAcc = accounts.find { it.code == "4001" } ?: accounts.find { it.code == "4110" }
                            Pair(targetAcc?.id, salesAcc?.id)
                        }
                        TransactionType.EXPENSE -> {
                            val expAcc = accounts.find { it.id == selectedExpenseAccountId }
                                ?: accounts.find { it.code == "6003" }
                                ?: accounts.find { it.code == "5210" }
                            val targetAcc = when (selectedExpensePaymentOption) {
                                DisbursementPaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                DisbursementPaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                DisbursementPaymentOption.HUTANG -> accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                            }
                            Pair(expAcc?.id, targetAcc?.id)
                        }
                        TransactionType.PURCHASE_INVENTORY -> {
                            val invAcc = accounts.find { it.code == "1201" } ?: accounts.find { it.code == "1140" }
                            val targetAcc = when (selectedPurchasePaymentOption) {
                                DisbursementPaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                DisbursementPaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                DisbursementPaymentOption.HUTANG -> accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                            }
                            Pair(invAcc?.id, targetAcc?.id)
                        }
                        TransactionType.PAY_DEBT -> {
                            val debtAcc = accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                            val targetAcc = when (selectedPayDebtOption) {
                                CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                            }
                            Pair(debtAcc?.id, targetAcc?.id)
                        }
                        TransactionType.RECEIVE_RECEIVABLE -> {
                            val recAcc = accounts.find { it.code == "1101" } ?: accounts.find { it.code == "1130" }
                            val targetAcc = when (selectedReceiveReceivableOption) {
                                CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                            }
                            Pair(targetAcc?.id, recAcc?.id)
                        }
                        TransactionType.OWNER_DRAW -> {
                            val drawAcc = accounts.find { it.code == "3002" } ?: accounts.find { it.code == "3140" }
                            val targetAcc = when (selectedOwnerDrawOption) {
                                CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                            }
                            Pair(drawAcc?.id, targetAcc?.id)
                        }
                        TransactionType.DONATION -> {
                            val charityAcc = accounts.find { it.code == "3003" } ?: accounts.find { it.code == "3130" }
                            val targetAcc = when (selectedDonationOption) {
                                CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                            }
                            Pair(charityAcc?.id, targetAcc?.id)
                        }
                        TransactionType.TRANSFER -> {
                            val kasAcc = accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                            val bankAcc = accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                            when (selectedTransferOption) {
                                TransferDirectionOption.KAS_TO_BANK -> Pair(bankAcc?.id, kasAcc?.id)
                                TransferDirectionOption.BANK_TO_KAS -> Pair(kasAcc?.id, bankAcc?.id)
                            }
                        }
                        else -> {
                            Pair(selectedCashAccountId, selectedCashAccountId)
                        }
                    }

                    val debtCatSymbol = when (selectedType) {
                        TransactionType.EXPENSE -> if (selectedExpensePaymentOption == DisbursementPaymentOption.HUTANG) selectedExpenseDebtCategory.symbol else null
                        TransactionType.PURCHASE_INVENTORY -> if (selectedPurchasePaymentOption == DisbursementPaymentOption.HUTANG) selectedPurchaseDebtCategory.symbol else null
                        TransactionType.PAY_DEBT -> selectedPayDebtCategory.symbol
                        else -> null
                    }

                    val finalDesc = if (debtCatSymbol != null) {
                        val cleanDesc = description.trim()
                        if (cleanDesc.startsWith("[$debtCatSymbol]")) {
                            cleanDesc
                        } else if (cleanDesc.isNotBlank()) {
                            "[$debtCatSymbol] $cleanDesc"
                        } else {
                            when (selectedType) {
                                TransactionType.EXPENSE -> "[$debtCatSymbol] Beban Operasional Hutang"
                                TransactionType.PURCHASE_INVENTORY -> "[$debtCatSymbol] Pembelian Persediaan Hutang"
                                TransactionType.PAY_DEBT -> "[$debtCatSymbol] Pembayaran Hutang Kategori $debtCatSymbol"
                                else -> "[$debtCatSymbol]"
                            }
                        }
                    } else {
                        description.trim().ifBlank { selectedType.displayName }
                    }

                    val finalRef = debtCatSymbol ?: referenceNumber

                    onPost(
                        selectedType,
                        finalDesc,
                        amt,
                        System.currentTimeMillis(),
                        drAccountId,
                        crAccountId,
                        finalRef,
                        notes
                    )
                },
                modifier = Modifier.testTag("btn_post_tx")
            ) {
                Text("Posting Langsung")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        val amt = FormatUtils.parseRupiahInput(amountInput) ?: 0.0
                        val (drAccountId, crAccountId) = when (selectedType) {
                            TransactionType.REVENUE -> {
                                val targetAcc = when (selectedRevenueOption) {
                                    RevenuePaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    RevenuePaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                    RevenuePaymentOption.PIUTANG -> accounts.find { it.code == "1101" } ?: accounts.find { it.code == "1130" }
                                }
                                val salesAcc = accounts.find { it.code == "4001" } ?: accounts.find { it.code == "4110" }
                                Pair(targetAcc?.id, salesAcc?.id)
                            }
                            TransactionType.EXPENSE -> {
                                val expAcc = accounts.find { it.id == selectedExpenseAccountId }
                                    ?: accounts.find { it.code == "6003" }
                                    ?: accounts.find { it.code == "5210" }
                                val targetAcc = when (selectedExpensePaymentOption) {
                                    DisbursementPaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    DisbursementPaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                    DisbursementPaymentOption.HUTANG -> accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                                }
                                Pair(expAcc?.id, targetAcc?.id)
                            }
                            TransactionType.PURCHASE_INVENTORY -> {
                                val invAcc = accounts.find { it.code == "1201" } ?: accounts.find { it.code == "1140" }
                                val targetAcc = when (selectedPurchasePaymentOption) {
                                    DisbursementPaymentOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    DisbursementPaymentOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                    DisbursementPaymentOption.HUTANG -> accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                                }
                                Pair(invAcc?.id, targetAcc?.id)
                            }
                            TransactionType.PAY_DEBT -> {
                                val debtAcc = accounts.find { it.code == "2001" } ?: accounts.find { it.code == "2110" }
                                val targetAcc = when (selectedPayDebtOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                Pair(debtAcc?.id, targetAcc?.id)
                            }
                            TransactionType.RECEIVE_RECEIVABLE -> {
                                val recAcc = accounts.find { it.code == "1101" } ?: accounts.find { it.code == "1130" }
                                val targetAcc = when (selectedReceiveReceivableOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                Pair(targetAcc?.id, recAcc?.id)
                            }
                            TransactionType.OWNER_DRAW -> {
                                val drawAcc = accounts.find { it.code == "3002" } ?: accounts.find { it.code == "3140" }
                                val targetAcc = when (selectedOwnerDrawOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                Pair(drawAcc?.id, targetAcc?.id)
                            }
                            TransactionType.DONATION -> {
                                val charityAcc = accounts.find { it.code == "3003" } ?: accounts.find { it.code == "3130" }
                                val targetAcc = when (selectedDonationOption) {
                                    CashOrBankOption.KAS -> accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                    CashOrBankOption.BANK -> accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                }
                                Pair(charityAcc?.id, targetAcc?.id)
                            }
                            TransactionType.TRANSFER -> {
                                val kasAcc = accounts.find { it.code == "1001" } ?: accounts.find { it.code == "1120" }
                                val bankAcc = accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                                when (selectedTransferOption) {
                                    TransferDirectionOption.KAS_TO_BANK -> Pair(bankAcc?.id, kasAcc?.id)
                                    TransferDirectionOption.BANK_TO_KAS -> Pair(kasAcc?.id, bankAcc?.id)
                                }
                            }
                            else -> {
                                Pair(selectedCashAccountId, selectedCashAccountId)
                            }
                        }

                        val debtCatSymbol = when (selectedType) {
                            TransactionType.EXPENSE -> if (selectedExpensePaymentOption == DisbursementPaymentOption.HUTANG) selectedExpenseDebtCategory.symbol else null
                            TransactionType.PURCHASE_INVENTORY -> if (selectedPurchasePaymentOption == DisbursementPaymentOption.HUTANG) selectedPurchaseDebtCategory.symbol else null
                            TransactionType.PAY_DEBT -> selectedPayDebtCategory.symbol
                            else -> null
                        }

                        val finalDesc = if (debtCatSymbol != null) {
                            val cleanDesc = description.trim()
                            if (cleanDesc.startsWith("[$debtCatSymbol]")) {
                                cleanDesc
                            } else if (cleanDesc.isNotBlank()) {
                                "[$debtCatSymbol] $cleanDesc"
                            } else {
                                when (selectedType) {
                                    TransactionType.EXPENSE -> "[$debtCatSymbol] Beban Operasional Hutang"
                                    TransactionType.PURCHASE_INVENTORY -> "[$debtCatSymbol] Pembelian Persediaan Hutang"
                                    TransactionType.PAY_DEBT -> "[$debtCatSymbol] Pembayaran Hutang Kategori $debtCatSymbol"
                                    else -> "[$debtCatSymbol]"
                                }
                            }
                        } else {
                            description.trim().ifBlank { selectedType.displayName }
                        }

                        val finalRef = debtCatSymbol ?: referenceNumber

                        onSaveDraft(
                            selectedType,
                            finalDesc,
                            amt,
                            System.currentTimeMillis(),
                            drAccountId,
                            crAccountId,
                            finalRef,
                            notes
                        )
                    }
                ) {
                    Text("Draft")
                }
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )
}

@Composable
fun TransactionDetailDialog(
    tx: TransactionRecord,
    journal: JournalWithLines?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onPostDraft: () -> Unit,
    onDeleteDraft: () -> Unit,
    onInitiateCancel: () -> Unit
) {
    val accountMap = accounts.associateBy { it.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Detail Transaksi", fontWeight = FontWeight.Bold)
                TransactionStatusBadge(status = tx.status)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    DetailRow("Nomor Transaksi", tx.txNumber)
                    DetailRow("Tanggal", FormatUtils.formatDateTime(tx.dateEpochMs))
                    DetailRow("Jenis Transaksi", tx.type.displayName)
                    DetailRow("Nominal", FormatUtils.formatRupiah(tx.amount))
                    DetailRow("Keterangan", tx.description)
                    if (tx.referenceNumber.isNotBlank()) {
                        DetailRow("Referensi", tx.referenceNumber)
                    }
                    if (tx.notes.isNotBlank()) {
                        DetailRow("Catatan", tx.notes)
                    }
                }

                // Automatic Journal Lines Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Jurnal Akuntansi Otomatis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (journal == null) {
                    item {
                        Text(
                            text = "Belum ada entri jurnal (transaksi masih berstatus DRAFT).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "No. Jurnal: ${journal.entry.entryNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(journal.lines) { line ->
                        val acc = accountMap[line.accountId]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${acc?.code ?: ""} - ${acc?.name ?: "Akun #${line.accountId}"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = acc?.category?.displayName ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (line.debitAmount > 0) {
                                        Text(
                                            text = "Dr: ${FormatUtils.formatRupiah(line.debitAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (line.creditAmount > 0) {
                                        Text(
                                            text = "Cr: ${FormatUtils.formatRupiah(line.creditAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (tx.status) {
                TransactionStatus.DRAFT -> {
                    Button(onClick = onPostDraft) {
                        Text("Posting Sekarang")
                    }
                }
                TransactionStatus.POSTED -> {
                    Button(
                        onClick = onInitiateCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseLoss),
                        modifier = Modifier.testTag("btn_initiate_cancel")
                    ) {
                        Text("Batalkan Transaksi")
                    }
                }
                TransactionStatus.CANCELLED -> {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                }
            }
        },
        dismissButton = {
            if (tx.status == TransactionStatus.DRAFT) {
                TextButton(
                    onClick = onDeleteDraft,
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseLoss)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Draft")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
