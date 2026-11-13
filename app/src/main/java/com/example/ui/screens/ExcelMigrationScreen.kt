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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Account
import com.example.data.model.ExcelArchive
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.model.TransactionType
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import com.example.util.FormatUtils

data class MigrationQueueItem(
    val description: String,
    val amount: Double,
    val type: TransactionType,
    val debitAccount: Account,
    val creditAccount: Account,
    val dateEpochMs: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelMigrationScreen(
    accounts: List<Account>,
    archives: List<ExcelArchive>,
    onCommitMigration: (String, String, String, List<Pair<TransactionRecord, Pair<Long, Long>>>, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Formulir Migrasi", "Arsip Excel (Read-Only)")

    // Queue of rows to migrate
    val queue = remember { mutableStateListOf<MigrationQueueItem>() }

    // Form inputs
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.REVENUE) }

    val defaultDr = accounts.find { it.code == "1110" } ?: accounts.firstOrNull()
    val defaultCr = accounts.find { it.code == "4110" } ?: accounts.firstOrNull()

    var selectedDebitAccount by remember { mutableStateOf(defaultDr) }
    var selectedCreditAccount by remember { mutableStateOf(defaultCr) }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var drDropdownExpanded by remember { mutableStateOf(false) }
    var crDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_migration_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gunakan formulir ini untuk memindahkan data historis Excel secara bertahap. Sistem akan memvalidasi keseimbangan jurnal sebelum diposting.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Input Form Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Input Baris Transaksi Excel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Type Selector
                                ExposedDropdownMenuBox(
                                    expanded = typeDropdownExpanded,
                                    onExpandedChange = { typeDropdownExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedType.displayName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Jenis Transaksi") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = typeDropdownExpanded,
                                        onDismissRequest = { typeDropdownExpanded = false }
                                    ) {
                                        TransactionType.values().forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text(t.displayName) },
                                                onClick = {
                                                    selectedType = t
                                                    typeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Keterangan dari Excel") },
                                    placeholder = { Text("Contoh: Saldo Pendapatan Cetak Minggu ke-1") },
                                    modifier = Modifier.fillMaxWidth().testTag("migration_input_desc"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = amountInput,
                                    onValueChange = { amountInput = it },
                                    label = { Text("Nominal (Rp)") },
                                    placeholder = { Text("Contoh: 150000") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("migration_input_amt"),
                                    singleLine = true
                                )

                                // Account Debit & Credit Selectors
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Debit Account
                                    ExposedDropdownMenuBox(
                                        expanded = drDropdownExpanded,
                                        onExpandedChange = { drDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedDebitAccount?.code ?: "Debit",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Akun Debit (Dr)") },
                                            modifier = Modifier.fillMaxWidth().menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = drDropdownExpanded,
                                            onDismissRequest = { drDropdownExpanded = false }
                                        ) {
                                            accounts.forEach { acc ->
                                                DropdownMenuItem(
                                                    text = { Text("${acc.code} ${acc.name}") },
                                                    onClick = {
                                                        selectedDebitAccount = acc
                                                        drDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Credit Account
                                    ExposedDropdownMenuBox(
                                        expanded = crDropdownExpanded,
                                        onExpandedChange = { crDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedCreditAccount?.code ?: "Kredit",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Akun Kredit (Cr)") },
                                            modifier = Modifier.fillMaxWidth().menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = crDropdownExpanded,
                                            onDismissRequest = { crDropdownExpanded = false }
                                        ) {
                                            accounts.forEach { acc ->
                                                DropdownMenuItem(
                                                    text = { Text("${acc.code} ${acc.name}") },
                                                    onClick = {
                                                        selectedCreditAccount = acc
                                                        crDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val amt = FormatUtils.parseRupiahInput(amountInput) ?: 0.0
                                        if (amt > 0 && description.isNotBlank() && selectedDebitAccount != null && selectedCreditAccount != null) {
                                            queue.add(
                                                MigrationQueueItem(
                                                    description = description.trim(),
                                                    amount = amt,
                                                    type = selectedType,
                                                    debitAccount = selectedDebitAccount!!,
                                                    creditAccount = selectedCreditAccount!!
                                                )
                                            )
                                            description = ""
                                            amountInput = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("btn_add_to_migration_queue"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambahkan ke Antrean Migrasi")
                                }
                            }
                        }
                    }

                    // Queue Header & Commit Card
                    if (queue.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    val totalQueueAmount = queue.sumOf { it.amount }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Antrean Migrasi (${queue.size} Transaksi)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(totalQueueAmount),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            val rowsToPost = queue.map { item ->
                                                val tx = TransactionRecord(
                                                    txNumber = "MIG-${System.currentTimeMillis() % 1000000}",
                                                    dateEpochMs = item.dateEpochMs,
                                                    description = "[Migrasi Excel] ${item.description}",
                                                    type = item.type,
                                                    status = TransactionStatus.POSTED,
                                                    amount = item.amount,
                                                    notes = "Migrasi data Excel bertahap"
                                                )
                                                Pair(tx, Pair(item.debitAccount.id, item.creditAccount.id))
                                            }

                                            val archiveContent = queue.joinToString("\n") {
                                                "${it.description},${it.amount},${it.debitAccount.code},${it.creditAccount.code}"
                                            }

                                            onCommitMigration(
                                                "Arsip_Excel_${System.currentTimeMillis()}.csv",
                                                "Migrasi Data Excel (${queue.size} baris)",
                                                "Batch migrasi tanggal ${FormatUtils.formatDate(System.currentTimeMillis())}",
                                                rowsToPost,
                                                archiveContent
                                            )
                                            queue.clear()
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("btn_commit_migration"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Konfirmasi & Posting ke Jurnal Umum")
                                    }
                                }
                            }
                        }

                        itemsIndexed(queue) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Dr: ${item.debitAccount.code} • Cr: ${item.creditAccount.code}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = FormatUtils.formatRupiah(item.amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { queue.removeAt(index) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = RoseLoss)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }

            1 -> {
                // Read-Only Archived Files
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ketentuan Sistem: File Excel yang telah diimpor disimpan sebagai arsip permanen berstatus Read-Only dan tidak dapat diubah oleh sistem.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (archives.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Belum ada arsip Excel yang tersimpan.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(archives) { arc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(arc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "READ-ONLY",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Berkas: ${arc.fileName} • ${FormatUtils.formatDate(arc.dateEpochMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${arc.totalRows} baris transaksi • Total: ${FormatUtils.formatRupiah(arc.totalDebit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (arc.notes.isNotBlank()) {
                                        Text(
                                            text = arc.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}
