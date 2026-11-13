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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Account
import com.example.data.model.JournalLineDetail
import com.example.data.model.JournalWithLines
import com.example.data.model.NormalBalance
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import com.example.util.FormatUtils
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerJournalScreen(
    accounts: List<Account>,
    journals: List<JournalWithLines>,
    postedLines: List<JournalLineDetail>
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Jurnal Umum", "Buku Besar (Ledger)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_ledger_${index}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTabIndex) {
            0 -> GeneralJournalTab(journals = journals, accounts = accounts)
            1 -> GeneralLedgerTab(accounts = accounts, postedLines = postedLines)
        }
    }
}

@Composable
fun GeneralJournalTab(
    journals: List<JournalWithLines>,
    accounts: List<Account>
) {
    val accountMap = accounts.associateBy { it.id }

    if (journals.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada entri jurnal akuntansi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(journals) { j ->
                val sumDr = j.lines.sumOf { it.debitAmount }
                val sumCr = j.lines.sumOf { it.creditAmount }
                val isBalanced = abs(sumDr - sumCr) < 0.001

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("journal_card_${j.entry.entryNumber}"),
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
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = j.entry.entryNumber,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (j.entry.isReversed) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = RoseLoss.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Dibalik",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = RoseLoss,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = FormatUtils.formatDate(j.entry.dateEpochMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Balance Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBalanced) EmeraldGain.copy(alpha = 0.12f) else RoseLoss.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isBalanced) EmeraldGain else RoseLoss,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Σ ${FormatUtils.formatRupiah(sumDr)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBalanced) EmeraldGain else RoseLoss
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = j.entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Lines Table
                        j.lines.forEach { line ->
                            val acc = accountMap[line.accountId]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${acc?.code ?: ""} - ${acc?.name ?: "Akun #${line.accountId}"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (line.debitAmount > 0) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (line.memo.isNotBlank() && line.memo != j.entry.description) {
                                        Text(
                                            text = line.memo,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (line.debitAmount > 0) {
                                        Text(
                                            text = FormatUtils.formatRupiah(line.debitAmount),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (line.creditAmount > 0) {
                                        Text(
                                            text = FormatUtils.formatRupiah(line.creditAmount),
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
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralLedgerTab(
    accounts: List<Account>,
    postedLines: List<JournalLineDetail>
) {
    var selectedAccountId by remember {
        mutableStateOf(accounts.find { it.code == "1002" }?.id ?: accounts.find { it.code == "1110" }?.id ?: accounts.firstOrNull()?.id)
    }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDebtCategoryFilter by remember(selectedAccountId) { mutableStateOf<String?>(null) }

    val currentAccount = accounts.find { it.id == selectedAccountId }
    val linesForAccount = postedLines.filter { it.accountId == selectedAccountId }

    // Helper to extract category K or R
    fun extractDebtCategory(line: JournalLineDetail): String? {
        val text = "${line.journalDescription} ${line.memo}"
        return when {
            text.contains("[R]", ignoreCase = true) || text.contains("Kategori \"R\"", ignoreCase = true) || text.contains("Kategori R", ignoreCase = true) -> "R"
            text.contains("[K]", ignoreCase = true) || text.contains("Kategori \"K\"", ignoreCase = true) || text.contains("Kategori K", ignoreCase = true) -> "K"
            else -> null
        }
    }

    val isHutangUsaha = currentAccount?.let {
        it.code in listOf("2001", "2110") || it.category == com.example.data.model.AccountCategory.LIABILITY || it.name.contains("Hutang", ignoreCase = true)
    } ?: false

    val linesR = if (isHutangUsaha) linesForAccount.filter { extractDebtCategory(it) == "R" } else emptyList()
    val totalHutangBaruR = linesR.sumOf { it.creditAmount }
    val totalBayarHutangR = linesR.sumOf { it.debitAmount }
    val saldoSisaR = totalHutangBaruR - totalBayarHutangR

    val linesK = if (isHutangUsaha) linesForAccount.filter { extractDebtCategory(it) == "K" } else emptyList()
    val totalHutangBaruK = linesK.sumOf { it.creditAmount }
    val totalBayarHutangK = linesK.sumOf { it.debitAmount }
    val saldoSisaK = totalHutangBaruK - totalBayarHutangK

    val linesToDisplay = if (isHutangUsaha && selectedDebtCategoryFilter != null) {
        linesForAccount.filter { extractDebtCategory(it) == selectedDebtCategoryFilter }
    } else {
        linesForAccount
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Account Selector Dropdown
        ExposedDropdownMenuBox(
            expanded = accountDropdownExpanded,
            onExpandedChange = { accountDropdownExpanded = it }
        ) {
            OutlinedTextField(
                value = currentAccount?.let { "${it.code} - ${it.name}" } ?: "Pilih Akun",
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih Akun Buku Besar") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("ledger_account_dropdown"),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(
                expanded = accountDropdownExpanded,
                onDismissRequest = { accountDropdownExpanded = false }
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${acc.code} - ${acc.name}")
                                Text(
                                    text = acc.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            selectedAccountId = acc.id
                            accountDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Account Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(accounts.take(6)) { acc ->
                FilterChip(
                    selected = selectedAccountId == acc.id,
                    onClick = { selectedAccountId = acc.id },
                    label = { Text(acc.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentAccount != null) {
            val accountTotalEndingBalance = linesForAccount.fold(0.0) { acc, line ->
                if (currentAccount.normalBalance == NormalBalance.DEBIT) {
                    acc + (line.debitAmount - line.creditAmount)
                } else {
                    acc + (line.creditAmount - line.debitAmount)
                }
            }

            // Running Balance Ledger Table
            var running = 0.0
            val ledgerRows = linesToDisplay.map { line ->
                if (currentAccount.normalBalance == NormalBalance.DEBIT) {
                    running += (line.debitAmount - line.creditAmount)
                } else {
                    running += (line.creditAmount - line.debitAmount)
                }
                Pair(line, running)
            }

            // Account Header Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${currentAccount.code} - ${currentAccount.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Kategori: ${currentAccount.category.displayName} • Saldo Normal: ${currentAccount.normalBalance.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Saldo Akhir Akun",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = FormatUtils.formatRupiah(accountTotalEndingBalance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Special Section for Hutang Usaha: Rekapitulasi Kategori Hutang "R" dan "K"
            if (isHutangUsaha) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_category_summary_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pengelompokan Kategori Hutang",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sinkronisasi Alur K & R",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Panel Kategori R
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDebtCategoryFilter = if (selectedDebtCategoryFilter == "R") null else "R"
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedDebtCategoryFilter == "R") Color(0xFFFFE0B2) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (selectedDebtCategoryFilter == "R") 2.dp else 1.dp,
                                    color = if (selectedDebtCategoryFilter == "R") Color(0xFFE65100) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE65100)
                                        ) {
                                            Text(
                                                text = "Kategori \"R\"",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${linesR.size} tx",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sisa Hutang R:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormatUtils.formatRupiah(saldoSisaR),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Hutang (+):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(totalHutangBaruR),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Bayar (-):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(totalBayarHutangR),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Panel Kategori K
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedDebtCategoryFilter = if (selectedDebtCategoryFilter == "K") null else "K"
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedDebtCategoryFilter == "K") Color(0xFFBBDEFB) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (selectedDebtCategoryFilter == "K") 2.dp else 1.dp,
                                    color = if (selectedDebtCategoryFilter == "K") Color(0xFF0D47A1) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF0D47A1)
                                        ) {
                                            Text(
                                                text = "Kategori \"K\"",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${linesK.size} tx",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sisa Hutang K:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = FormatUtils.formatRupiah(saldoSisaK),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D47A1)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Hutang (+):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(totalHutangBaruK),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Bayar (-):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = FormatUtils.formatRupiah(totalBayarHutangK),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter Chips for Hutang Usaha ledger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedDebtCategoryFilter == null,
                                onClick = { selectedDebtCategoryFilter = null },
                                label = { Text("Semua (${linesForAccount.size})") }
                            )
                            FilterChip(
                                selected = selectedDebtCategoryFilter == "R",
                                onClick = { selectedDebtCategoryFilter = if (selectedDebtCategoryFilter == "R") null else "R" },
                                label = { Text("Kategori \"R\" (${linesR.size})") }
                            )
                            FilterChip(
                                selected = selectedDebtCategoryFilter == "K",
                                onClick = { selectedDebtCategoryFilter = if (selectedDebtCategoryFilter == "K") null else "K" },
                                label = { Text("Kategori \"K\" (${linesK.size})") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (ledgerRows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedDebtCategoryFilter != null) "Tidak ada transaksi untuk Kategori \"$selectedDebtCategoryFilter\"" else "Belum ada transaksi pada akun ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ledgerRows) { (line, currentBalance) ->
                        val lineDebtCat = extractDebtCategory(line)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${FormatUtils.formatDate(line.dateEpochMs)} • ${line.entryNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (lineDebtCat != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (lineDebtCat == "R") Color(0xFFFFE0B2) else Color(0xFFBBDEFB)
                                            ) {
                                                Text(
                                                    text = "Kat. \"$lineDebtCat\"",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (lineDebtCat == "R") Color(0xFFE65100) else Color(0xFF0D47A1),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Saldo: ${FormatUtils.formatRupiah(currentBalance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = line.journalDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (line.debitAmount > 0) {
                                        Text(
                                            text = "Debit: ${FormatUtils.formatRupiah(line.debitAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    if (line.creditAmount > 0) {
                                        Text(
                                            text = "Kredit: ${FormatUtils.formatRupiah(line.creditAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
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
