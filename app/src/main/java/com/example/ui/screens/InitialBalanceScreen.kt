package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.OpeningBalanceBatch
import com.example.data.repository.AccountingRepository
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import com.example.util.FormatUtils
import java.util.Calendar
import kotlin.math.abs

data class OpeningBalanceDraftRow(
    val account: Account,
    var debit: Double = 0.0,
    var credit: Double = 0.0,
    var notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialBalanceScreen(
    accounts: List<Account>,
    openingBatches: List<OpeningBalanceBatch>,
    onPostOpeningBalance: (Long, Long, String, List<AccountingRepository.OpeningBalanceItemInput>, Boolean) -> Unit
) {
    // Cut-off: 31 Oktober 2026 23:59:59
    val cutoffCalendar = remember {
        Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val cutoffEpochMs = cutoffCalendar.timeInMillis

    // Start date: 1 November 2026 00:00:00
    val startCalendar = remember {
        Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startEpochMs = startCalendar.timeInMillis

    val latestPostedBatch = openingBatches.find { it.isPosted && it.cutoffDateEpochMs == cutoffEpochMs }

    // Draft rows in UI
    val rows = remember { mutableStateListOf<OpeningBalanceDraftRow>() }
    var isInitialized by remember { mutableStateOf(false) }

    // Auto-populate default validated data if rows are empty
    if (!isInitialized && accounts.isNotEmpty()) {
        val bca = accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
        val modal = accounts.find { it.code == "3001" } ?: accounts.find { it.code == "3110" }

        if (bca != null && modal != null) {
            rows.clear()
            rows.add(OpeningBalanceDraftRow(account = bca, debit = 0.94, credit = 0.0, notes = "Bank BCA (1002)"))
            rows.add(OpeningBalanceDraftRow(account = modal, debit = 0.0, credit = 0.94, notes = "Modal Pemilik (3001)"))
            isInitialized = true
        }
    }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var selectedAccountToAdd by remember { mutableStateOf<Account?>(null) }
    var addAmountInput by remember { mutableStateOf("") }
    var isDebitPosition by remember { mutableStateOf(true) }

    val totalDebit = rows.sumOf { it.debit }
    val totalCredit = rows.sumOf { it.credit }
    val discrepancy = abs(totalDebit - totalCredit)
    val isBalanced = discrepancy < 0.001 && totalDebit > 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("opening_balance_banner_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (latestPostedBatch != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (latestPostedBatch != null) Icons.Default.VerifiedUser else Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Modul Saldo Awal (Opening Balance)",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (latestPostedBatch != null) "DIPOSTING" else "DRAFT / INPUT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tanggal Cut-Off: 31 Oktober 2026",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tanggal Mulai Pembukuan: 1 November 2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    if (latestPostedBatch != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Batch ID: ${latestPostedBatch.batchNumber} • Status: Terkunci di Buku Besar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Action Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val bca = accounts.find { it.code == "1002" } ?: accounts.find { it.code == "1110" }
                        val modal = accounts.find { it.code == "3001" } ?: accounts.find { it.code == "3110" }
                        if (bca != null && modal != null) {
                            rows.clear()
                            rows.add(OpeningBalanceDraftRow(account = bca, debit = 0.94, credit = 0.0, notes = "Bank BCA (1002)"))
                            rows.add(OpeningBalanceDraftRow(account = modal, debit = 0.0, credit = 0.94, notes = "Modal Pemilik (3001)"))
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("btn_load_standard_opening"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Isi Data Resmi", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = { showAddAccountDialog = true },
                    modifier = Modifier.weight(1f).testTag("btn_add_account_to_opening"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Baris", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Preview Table Header
        item {
            Text(
                text = "Tabel Pratinjau Jurnal Saldo Awal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Preview Journal Table Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("opening_balance_table_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Akun",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.4f)
                        )
                        Text(
                            text = "Debit (Rp)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kredit (Rp)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "",
                            modifier = Modifier.width(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Table Rows
                    rows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.4f)) {
                                Text(
                                    text = "${row.account.code} - ${row.account.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = row.account.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (row.debit > 0) FormatUtils.formatRupiah(row.debit) else "-",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (row.debit > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (row.credit > 0) FormatUtils.formatRupiah(row.credit) else "-",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (row.credit > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { rows.removeAt(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = RoseLoss, modifier = Modifier.size(16.dp))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // TOTAL ROW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.4f)
                        )
                        Text(
                            text = FormatUtils.formatRupiah(totalDebit),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = FormatUtils.formatRupiah(totalCredit),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(28.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Balance Status Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isBalanced) EmeraldGain.copy(alpha = 0.12f) else RoseLoss.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isBalanced) EmeraldGain else RoseLoss,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isBalanced) "Status: SEIMBANG (Klop)" else "Status: BELUM SEIMBANG",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBalanced) EmeraldGain else RoseLoss
                                    )
                                    if (!isBalanced) {
                                        Text(
                                            text = "Selisih Debit - Kredit: ${FormatUtils.formatRupiah(discrepancy)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RoseLoss
                                        )
                                    }
                                }
                            }

                            if (isBalanced) {
                                Text(
                                    text = FormatUtils.formatRupiah(totalDebit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGain
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons: Simpan Draft, Posting Saldo Awal
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (isBalanced) {
                            showConfirmDialog = true
                        }
                    },
                    enabled = isBalanced && latestPostedBatch == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_post_opening_balance"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (latestPostedBatch != null) "Sudah Diposting (Terkunci)" else "Posting Saldo Awal ke Buku Besar",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val items = rows.map {
                            AccountingRepository.OpeningBalanceItemInput(
                                accountId = it.account.id,
                                debitAmount = it.debit,
                                creditAmount = it.credit,
                                notes = it.notes
                            )
                        }
                        onPostOpeningBalance(
                            cutoffEpochMs,
                            startEpochMs,
                            "Draft Saldo Awal Cut-Off 31 Oktober 2026",
                            items,
                            true // isDraft = true
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_save_draft_opening_balance"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan Draft Saldo Awal")
                }
            }
        }

        // Lock & Anti-Duplicate Notice
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ketentuan Integritas Pembukuan",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Saldo awal hanya dapat diposting 1 kali untuk periode cut-off yang sama guna mencegah saldo ganda.\n2. Opening Balance Equity (3999) adalah akun penyeimbang khusus saldo awal.\n3. Setelah diposting, data otomatis masuk ke jurnal umum dan buku besar.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Add Account Row Dialog
    if (showAddAccountDialog) {
        var expandedAccountDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Tambah Akun ke Saldo Awal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedAccountDropdown,
                        onExpandedChange = { expandedAccountDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccountToAdd?.let { "${it.code} - ${it.name}" } ?: "Pilih Akun",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Akun") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccountDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAccountDropdown,
                            onDismissRequest = { expandedAccountDropdown = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.code} - ${acc.name} (${acc.category.displayName})") },
                                    onClick = {
                                        selectedAccountToAdd = acc
                                        expandedAccountDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isDebitPosition = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDebitPosition) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Debit (Dr)")
                        }
                        Button(
                            onClick = { isDebitPosition = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isDebitPosition) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kredit (Cr)")
                        }
                    }

                    OutlinedTextField(
                        value = addAmountInput,
                        onValueChange = { addAmountInput = it },
                        label = { Text("Nominal Saldo (Rp)") },
                        placeholder = { Text("Contoh: 100000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_opening_amount"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = FormatUtils.parseRupiahInput(addAmountInput) ?: 0.0
                        if (selectedAccountToAdd != null && amt > 0) {
                            val acc = selectedAccountToAdd!!
                            rows.add(
                                OpeningBalanceDraftRow(
                                    account = acc,
                                    debit = if (isDebitPosition) amt else 0.0,
                                    credit = if (!isDebitPosition) amt else 0.0,
                                    notes = "Saldo Awal ${acc.name}"
                                )
                            )
                            showAddAccountDialog = false
                            selectedAccountToAdd = null
                            addAmountInput = ""
                        }
                    }
                ) {
                    Text("Tambahkan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAccountDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Before Posting Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = EmeraldGain)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Posting Saldo Awal", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda yakin ingin memposting Saldo Awal ini ke Buku Besar?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Cut-off: 31 Oktober 2026\n• Total Debit = Total Kredit: ${FormatUtils.formatRupiah(totalDebit)}\n• Posting bersifat atomik dan akan memperbarui saldo buku besar secara permanen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val items = rows.map {
                            AccountingRepository.OpeningBalanceItemInput(
                                accountId = it.account.id,
                                debitAmount = it.debit,
                                creditAmount = it.credit,
                                notes = it.notes
                            )
                        }
                        onPostOpeningBalance(
                            cutoffEpochMs,
                            startEpochMs,
                            "Posting Saldo Awal Cut-Off 31 Oktober 2026",
                            items,
                            false
                        )
                        showConfirmDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_post_opening_balance")
                ) {
                    Text("Ya, Posting Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("Periksa Lagi")
                }
            }
        )
    }
}
