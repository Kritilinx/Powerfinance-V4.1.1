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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountCategory
import com.example.data.model.NormalBalance
import com.example.data.repository.AccountBalanceItem
import com.example.data.repository.BalanceSheetData
import com.example.data.repository.CashFlowStatementData
import com.example.data.repository.IncomeStatementData
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.VioletEquity
import com.example.util.FormatUtils
import kotlin.math.abs

@Composable
fun ReportsScreen(
    incomeStatement: IncomeStatementData,
    balanceSheet: BalanceSheetData,
    cashFlow: CashFlowStatementData,
    accountBalances: List<AccountBalanceItem>
) {
    var selectedReportTab by remember { mutableIntStateOf(0) }
    val reportTabs = listOf("Laba Rugi", "Neraca Keuangan", "Arus Kas", "Neraca Saldo")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedReportTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            reportTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedReportTab == index,
                    onClick = { selectedReportTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedReportTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("report_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedReportTab) {
            0 -> IncomeStatementView(incomeStatement = incomeStatement)
            1 -> BalanceSheetView(balanceSheet = balanceSheet)
            2 -> CashFlowView(cashFlow = cashFlow)
            3 -> TrialBalanceView(accountBalances = accountBalances)
        }
    }
}

@Composable
fun IncomeStatementView(incomeStatement: IncomeStatementData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_net_income_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (incomeStatement.netIncome >= 0) EmeraldGain.copy(alpha = 0.15f) else RoseLoss.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (incomeStatement.netIncome >= 0) "Laba Bersih Periode Berjalan" else "Rugi Bersih Periode Berjalan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (incomeStatement.netIncome >= 0) EmeraldGain else RoseLoss
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatRupiah(incomeStatement.netIncome),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (incomeStatement.netIncome >= 0) EmeraldGain else RoseLoss
                    )
                }
            }
        }

        // Revenues Section
        item {
            ReportSectionCard(
                title = "Pendapatan Usaha",
                totalLabel = "Total Pendapatan",
                totalAmount = incomeStatement.totalRevenue,
                items = incomeStatement.revenueItems
            )
        }

        // Expenses Section
        item {
            ReportSectionCard(
                title = "Beban Operasional & HPP",
                totalLabel = "Total Beban",
                totalAmount = incomeStatement.totalExpense,
                items = incomeStatement.expenseItems
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Catatan Akuntansi: Donasi pemilik TIDAK dimasukkan dalam beban operasional, melainkan mengurangi ekuitas pemilik di Neraca.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun BalanceSheetView(balanceSheet: BalanceSheetData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Balance Check Indicator Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_balance_sheet_status"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (balanceSheet.isBalanced) EmeraldGain.copy(alpha = 0.15f) else RoseLoss.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (balanceSheet.isBalanced) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (balanceSheet.isBalanced) EmeraldGain else RoseLoss,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (balanceSheet.isBalanced) "Neraca Seimbang (Klop)" else "Peringatan: Neraca Tidak Seimbang",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (balanceSheet.isBalanced) EmeraldGain else RoseLoss
                            )
                            Text(
                                text = "Total Aset = Total Kewajiban + Ekuitas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = FormatUtils.formatRupiah(balanceSheet.totalAssets),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (balanceSheet.isBalanced) EmeraldGain else RoseLoss
                    )
                }
            }
        }

        // ASET (Assets)
        item {
            ReportSectionCard(
                title = "1. Aset (Aktiva)",
                totalLabel = "Total Aset",
                totalAmount = balanceSheet.totalAssets,
                items = balanceSheet.assetItems
            )
        }

        // KEWAJIBAN (Liabilities)
        item {
            ReportSectionCard(
                title = "2. Kewajiban / Hutang",
                totalLabel = "Total Kewajiban",
                totalAmount = balanceSheet.totalLiabilities,
                items = balanceSheet.liabilityItems
            )
        }

        // EKUITAS (Equity)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "3. Ekuitas Pemilik",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    balanceSheet.equityItems.forEach { item ->
                        val isDeduction = item.account.normalBalance == NormalBalance.DEBIT
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${item.account.code} - ${item.account.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDeduction) VioletEquity else MaterialTheme.colorScheme.onSurface
                                )
                                if (isDeduction) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "(Pengurang)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VioletEquity
                                    )
                                }
                            }
                            Text(
                                text = if (isDeduction) "(${FormatUtils.formatRupiah(item.netBalance)})" else FormatUtils.formatRupiah(item.netBalance),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDeduction) VioletEquity else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Net Income line in Equity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Laba Bersih Periode Berjalan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = FormatUtils.formatRupiah(balanceSheet.netIncomeCurrentPeriod),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (balanceSheet.netIncomeCurrentPeriod >= 0) EmeraldGain else RoseLoss
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Ekuitas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = FormatUtils.formatRupiah(balanceSheet.totalEquity),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Total Pasiva (Liabilities + Equity)
        item {
            val totalPasiva = balanceSheet.totalLiabilities + balanceSheet.totalEquity
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
                    Text(
                        text = "Total Kewajiban & Ekuitas (Pasiva)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = FormatUtils.formatRupiah(totalPasiva),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun CashFlowView(cashFlow: CashFlowStatementData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (cashFlow.netCashChange >= 0) EmeraldGain.copy(alpha = 0.15f) else RoseLoss.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kenaikan / (Penurunan) Kas & Bank Bersih",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatRupiah(cashFlow.netCashChange),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cashFlow.netCashChange >= 0) EmeraldGain else RoseLoss
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Arus Kas dari Aktivitas Operasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReportLine("Penerimaan Kas Operasional", cashFlow.operatingIn)
                    ReportLine("Pengeluaran Kas Operasional & HPP", -cashFlow.operatingOut)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ReportLine("Arus Kas Bersih Operasi", cashFlow.netOperating, isBold = true)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Arus Kas dari Aktivitas Pendanaan & Ekuitas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ReportLine("Setoran Modal / Penerimaan Pendanaan", cashFlow.financingIn)
                    ReportLine("Pembayaran Hutang & Donasi/Prive", -cashFlow.financingOut)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ReportLine("Arus Kas Bersih Pendanaan", cashFlow.netFinancing, isBold = true)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun TrialBalanceView(accountBalances: List<AccountBalanceItem>) {
    val totalDebit = accountBalances.sumOf { it.totalDebit }
    val totalCredit = accountBalances.sumOf { it.totalCredit }
    val isBalanced = abs(totalDebit - totalCredit) < 0.01

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBalanced) EmeraldGain.copy(alpha = 0.15f) else RoseLoss.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Mutasi: Dr = Cr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = FormatUtils.formatRupiah(totalDebit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isBalanced) EmeraldGain else RoseLoss
                    )
                }
            }
        }

        items(accountBalances) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
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
                            text = "${item.account.code} - ${item.account.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.account.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Saldo: ${FormatUtils.formatRupiah(item.netBalance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Dr: ${FormatUtils.formatRupiah(item.totalDebit, false)} | Cr: ${FormatUtils.formatRupiah(item.totalCredit, false)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun ReportSectionCard(
    title: String,
    totalLabel: String,
    totalAmount: Double,
    items: List<AccountBalanceItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.account.code} - ${item.account.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = FormatUtils.formatRupiah(item.netBalance),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = totalLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = FormatUtils.formatRupiah(totalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReportLine(label: String, amount: Double, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = FormatUtils.formatRupiah(amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (amount >= 0) MaterialTheme.colorScheme.onSurface else RoseLoss
        )
    }
}
