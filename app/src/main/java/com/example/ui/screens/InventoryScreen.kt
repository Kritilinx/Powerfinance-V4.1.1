package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.InventoryLog
import com.example.data.model.InventoryMutationType
import com.example.data.repository.AccountBalanceItem
import com.example.data.repository.IncomeStatementData
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import com.example.util.FormatUtils

@Composable
fun InventoryScreen(
    accountBalances: List<AccountBalanceItem>,
    inventoryLogs: List<InventoryLog>,
    incomeStatement: IncomeStatementData? = null,
    onRecordMutation: (InventoryMutationType, Double, String) -> Unit
) {
    var showMutationDialog by remember { mutableStateOf(false) }
    var selectedMutationType by remember { mutableStateOf(InventoryMutationType.PURCHASE) }

    val accountBalance = accountBalances.find { it.account.code == "1201" || it.account.code == "1140" }?.netBalance
    val latestLogBalance = inventoryLogs.firstOrNull()?.endingBalance
    val inventoryAccountBalance = when {
        accountBalance != null && accountBalance != 0.0 -> accountBalance
        latestLogBalance != null -> latestLogBalance
        accountBalance != null -> accountBalance
        else -> 0.0
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedMutationType = InventoryMutationType.PURCHASE
                    showMutationDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_inventory")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Mutasi")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Balance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_balance_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Persediaan (Total Nilai Moneter)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = FormatUtils.formatRupiah(inventoryAccountBalance),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Metode Nilai Total: Tidak menggunakan SKU, kode, atau kuantitas unit.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Quick Mutation Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedMutationType = InventoryMutationType.PURCHASE
                            showMutationDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldGain)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Beli (Dr)", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            selectedMutationType = InventoryMutationType.USAGE
                            showMutationDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseLoss)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pakai (HPP)", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            selectedMutationType = InventoryMutationType.ADJUSTMENT
                            showMutationDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sesuaikan", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Mutation History List Header
            item {
                Text(
                    text = "Riwayat Mutasi Nilai Persediaan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (inventoryLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada log mutasi persediaan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(inventoryLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.notes.ifBlank { log.type.displayName },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${FormatUtils.formatDate(log.dateEpochMs)} • ${log.type.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val isAdd = log.type == InventoryMutationType.PURCHASE
                                Text(
                                    text = "${if (isAdd) "+" else "-"} ${FormatUtils.formatRupiah(log.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdd) EmeraldGain else RoseLoss
                                )
                                Text(
                                    text = "Saldo: ${FormatUtils.formatRupiah(log.endingBalance)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showMutationDialog) {
        InventoryMutationDialog(
            type = selectedMutationType,
            onDismiss = { showMutationDialog = false },
            onConfirm = { type, amount, desc ->
                onRecordMutation(type, amount, desc)
                showMutationDialog = false
            }
        )
    }
}

@Composable
fun InventoryMutationDialog(
    type: InventoryMutationType,
    onDismiss: () -> Unit,
    onConfirm: (InventoryMutationType, Double, String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (type) {
                    InventoryMutationType.PURCHASE -> "Catat Pembelian Persediaan"
                    InventoryMutationType.USAGE -> "Catat Pemakaian Persediaan (HPP)"
                    InventoryMutationType.ADJUSTMENT -> "Penyesuaian Nilai Persediaan"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (type) {
                                InventoryMutationType.PURCHASE -> "Membentuk jurnal: Debit Persediaan (1201), Kredit Bank BCA / Kas (1002 / 1001)"
                                InventoryMutationType.USAGE -> "Membentuk jurnal: Debit Beban HPP (5001), Kredit Persediaan (1201)"
                                InventoryMutationType.ADJUSTMENT -> "Menyesuaikan nilai total persediaan secara langsung ke laporan keuangan"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Nominal Nilai (Rp)") },
                    placeholder = { Text("Contoh: 75000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_inventory_amount"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Keterangan Mutasi") },
                    placeholder = { Text("Contoh: Pembelian kertas art paper atau Pemakaian tinta cetak") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = FormatUtils.parseRupiahInput(amountInput) ?: 0.0
                    if (amt > 0) {
                        onConfirm(type, amt, notes.trim())
                    }
                },
                modifier = Modifier.testTag("btn_confirm_inventory_mutation")
            ) {
                Text("Simpan & Buat Jurnal")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
