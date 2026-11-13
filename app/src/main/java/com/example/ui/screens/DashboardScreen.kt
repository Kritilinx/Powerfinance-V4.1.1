package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionStatus
import com.example.data.repository.AccountBalanceItem
import com.example.data.repository.BalanceSheetData
import com.example.data.repository.IncomeStatementData
import com.example.ui.components.CharityBoxCard
import com.example.ui.theme.AmberDraft
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.SlateCancelled
import com.example.ui.viewmodel.AppScreen
import com.example.util.FormatUtils

@Composable
fun DashboardScreen(
    balanceSheet: BalanceSheetData,
    incomeStatement: IncomeStatementData,
    accountBalances: List<AccountBalanceItem>,
    recentTransactions: List<TransactionRecord>,
    onNavigate: (AppScreen) -> Unit,
    onOpenNewTransaction: () -> Unit
) {
    var showGenerateDropdown by remember { mutableStateOf(false) }
    var selectedGenerateType by remember { mutableStateOf<String?>(null) }

    // Find Bank BCA & Persediaan balance (supporting Phase 2 codes 1002 & 1201)
    val bcaBalance = accountBalances.find { it.account.code == "1002" || it.account.code == "1110" }?.netBalance ?: 0.0
    val inventoryBalance = accountBalances.find { it.account.code == "1201" || it.account.code == "1140" }?.netBalance ?: 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            val isNetProfit = incomeStatement.netIncome >= 0
            val accentColor = if (isNetProfit) EmeraldGain else RoseLoss
            val containerColor = if (isNetProfit) EmeraldGain.copy(alpha = 0.15f) else RoseLoss.copy(alpha = 0.15f)

            // Top Card: Laporan - Laba Rugi - Laba Bersih Periode Berjalan
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                ),
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isNetProfit) "Laba Bersih Periode Berjalan" else "Rugi Bersih Periode Berjalan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = "Laporan Laba Rugi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = FormatUtils.formatRupiah(incomeStatement.netIncome),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Sub-Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Pendapatan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatRupiah(incomeStatement.totalRevenue, false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGain
                            )
                        }
                        Column {
                            Text(
                                text = "Beban & HPP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatRupiah(incomeStatement.totalExpense, false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = RoseLoss
                            )
                        }
                        Column {
                            Text(
                                text = "Saldo Bank BCA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatRupiah(bcaBalance, false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Charity Box Card (Placed directly under the green hero card)
        item {
            CharityBoxCard(netIncome = incomeStatement.netIncome)
        }

        // Quick Accounting Actions Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenNewTransaction,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_btn_new_tx"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Transaksi", fontWeight = FontWeight.Bold)
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showGenerateDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_btn_generate"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate")
                    }

                    DropdownMenu(
                        expanded = showGenerateDropdown,
                        onDismissRequest = { showGenerateDropdown = false },
                        modifier = Modifier.testTag("dropdown_menu_generate")
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Struk", fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                showGenerateDropdown = false
                                onNavigate(AppScreen.RECEIPT_GENERATOR)
                            },
                            modifier = Modifier.testTag("menu_item_generate_struk")
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Invoice", fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                showGenerateDropdown = false
                                onNavigate(AppScreen.INVOICE_GENERATOR)
                            },
                            modifier = Modifier.testTag("menu_item_generate_invoice")
                        )
                    }
                }
            }
        }

        // Core Accounting Modules Grid
        item {
            Text(
                text = "Modul Akuntansi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardModuleCard(
                        title = "Persediaan Nilai",
                        subtitle = FormatUtils.formatRupiah(inventoryBalance, false),
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.INVENTORY) }
                    )
                    DashboardModuleCard(
                        title = "Laporan Keuangan",
                        subtitle = "Laba/Rugi & Neraca",
                        icon = Icons.Default.Assessment,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.REPORTS) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardModuleCard(
                        title = "Daftar Akun (COA)",
                        subtitle = "${accountBalances.size} Akun Terdaftar",
                        icon = Icons.Default.TableChart,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.ACCOUNTS) }
                    )
                    DashboardModuleCard(
                        title = "Migrasi Excel",
                        subtitle = "Input & Arsip Data",
                        icon = Icons.Default.Receipt,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.EXCEL_MIGRATION) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardModuleCard(
                        title = "Kasir & Cetak Nota",
                        subtitle = "Generator Nota Thermal",
                        icon = Icons.Default.Print,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.RECEIPT_GENERATOR) }
                    )
                    DashboardModuleCard(
                        title = "Saldo Awal",
                        subtitle = "Cut-Off Pembukuan",
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.INITIAL_BALANCE) }
                    )
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terkini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lihat Semua",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigate(AppScreen.TRANSACTIONS) }
                        .padding(4.dp)
                )
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum ada transaksi tambahan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gunakan tombol '+ Catat Transaksi' untuk memulai pencatatan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions.take(5)) { tx ->
                DashboardTransactionItem(tx = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (selectedGenerateType != null) {
        if (selectedGenerateType == "Nota") {
            com.example.receipt.ui.ReceiptGeneratorScreen(
                onClose = { selectedGenerateType = null }
            )
        } else if (selectedGenerateType == "Invoice") {
            com.example.invoice.ui.InvoiceGeneratorScreen(
                onClose = { selectedGenerateType = null }
            )
        } else {
            StandaloneGenerateDialog(
                docType = selectedGenerateType!!,
                onDismiss = { selectedGenerateType = null }
            )
        }
    }
}


@Composable
private fun DashboardModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("mod_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DashboardTransactionItem(tx: TransactionRecord) {
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormatUtils.formatDate(tx.dateEpochMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ${tx.type.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatRupiah(tx.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (tx.status) {
                        TransactionStatus.CANCELLED -> SlateCancelled
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                TransactionStatusBadge(status = tx.status)
            }
        }
    }
}

@Composable
fun TransactionStatusBadge(status: TransactionStatus) {
    val (bgColor, textColor, label) = when (status) {
        TransactionStatus.POSTED -> Triple(EmeraldGain.copy(alpha = 0.15f), EmeraldGain, "Diposting")
        TransactionStatus.DRAFT -> Triple(AmberDraft.copy(alpha = 0.15f), AmberDraft, "Draft")
        TransactionStatus.CANCELLED -> Triple(RoseLoss.copy(alpha = 0.15f), RoseLoss, "Dibatalkan")
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StandaloneGenerateDialog(
    docType: String,
    onDismiss: () -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var docNumber by remember {
        mutableStateOf(if (docType == "Nota") "NOTA-${System.currentTimeMillis() % 100000}" else "INV-${System.currentTimeMillis() % 100000}")
    }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isGenerated by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (docType == "Nota") Icons.Default.Receipt else Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate $docType", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Fitur Mandiri: Dokumen $docType ini dibuat terpisah dan tidak terhubung ke alur pembukuan ataupun buku besar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (!isGenerated) {
                    OutlinedTextField(
                        value = docNumber,
                        onValueChange = { docNumber = it },
                        label = { Text("Nomor $docType") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nama Pelanggan / Kepada") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Barang / Jasa") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Nominal Total (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Pratinjau Dokumen $docType", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No: $docNumber", style = MaterialTheme.typography.bodySmall)
                            Text("Kepada: ${customerName.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Keterangan: ${description.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Total: Rp ${amountText.ifBlank { "0" }}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("✓ Dokumen $docType siap dicetak/disimpan secara mandiri.", color = EmeraldGain, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isGenerated) {
                Button(
                    onClick = { isGenerated = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Buat $docType")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Selesai")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

