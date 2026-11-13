package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.FormatUtils

@Composable
fun QuickTransactionScreen(
    onPostBatch: (
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
        pemakaianDesc: String
    ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Penjualan State
    var penjualanNominalText by remember { mutableStateOf("") }
    var penjualanOption by remember { mutableStateOf("Kas") } // "Kas", "Bank", "Piutang"
    var penjualanDesc by remember { mutableStateOf("") }

    // 2. Ongkir State
    var ongkirNominalText by remember { mutableStateOf("") }
    var ongkirOption by remember { mutableStateOf("Kas") } // "Kas", "Bank", "Hutang"
    var ongkirHutangCategory by remember { mutableStateOf("K") } // "K", "R"
    var ongkirDesc by remember { mutableStateOf("") }

    // 3. Pembelian Persediaan State
    var pembelianNominalText by remember { mutableStateOf("") }
    var pembelianOption by remember { mutableStateOf("Kas") } // "Kas", "Bank", "Hutang"
    var pembelianHutangCategory by remember { mutableStateOf("K") } // "K", "R"
    var pembelianDesc by remember { mutableStateOf("") }

    // 4. Pemakaian Persediaan (HPP) State
    var pemakaianNominalText by remember { mutableStateOf("") }
    var pemakaianDesc by remember { mutableStateOf("") }

    // Parse double values safely
    fun parseAmount(text: String): Double {
        return text.replace(".", "").replace(",", ".").trim().toDoubleOrNull() ?: 0.0
    }

    val pNom = parseAmount(penjualanNominalText)
    val oNom = parseAmount(ongkirNominalText)
    val bNom = parseAmount(pembelianNominalText)
    val uNom = parseAmount(pemakaianNominalText)
    val hasValidEntry = pNom > 0 || oNom > 0 || bNom > 0 || uNom > 0

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Transaksi Cepat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Isi nominal yang relevan. Sistem akan mencatat jurnal otomatis untuk setiap bagian yang diisi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ==========================================
        // 1. PENJUALAN
        // ==========================================
        QuickSectionCard(
            title = "1. Penjualan",
            subtitle = "Pendapatan kas/bank/piutang dari penjualan produk atau jasa",
            icon = Icons.Default.PointOfSale,
            iconTint = Color(0xFF10B981)
        ) {
            Text(
                text = "Metode Pembayaran:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kas", "Bank", "Piutang").forEach { opt ->
                    FilterChip(
                        selected = penjualanOption == opt,
                        onClick = { penjualanOption = opt },
                        label = { Text(opt) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("penjualan_chip_$opt")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = penjualanNominalText,
                onValueChange = { penjualanNominalText = it },
                label = { Text("Nominal Penjualan") },
                placeholder = { Text("0") },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_penjualan_nominal"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = penjualanDesc,
                onValueChange = { penjualanDesc = it },
                label = { Text("Keterangan Penjualan") },
                placeholder = { Text("Contoh: Penjualan Banner / Cetak Stiker") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_penjualan_desc"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // ==========================================
        // 2. ONGKIR
        // ==========================================
        QuickSectionCard(
            title = "2. Ongkir",
            subtitle = "Biaya kurir, ongkir ekspedisi, atau pengantaran barang",
            icon = Icons.Default.LocalShipping,
            iconTint = Color(0xFF38BDF8)
        ) {
            Text(
                text = "Metode Pembayaran Ongkir:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kas", "Bank", "Hutang").forEach { opt ->
                    FilterChip(
                        selected = ongkirOption == opt,
                        onClick = { ongkirOption = opt },
                        label = { Text(opt) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("ongkir_chip_$opt")
                    )
                }
            }

            // Opsi Kategori K & Kategori R saat Hutang dipilih
            AnimatedVisibility(
                visible = ongkirOption == "Hutang",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Pilih Kategori Hutang:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("K", "R").forEach { cat ->
                            FilterChip(
                                selected = ongkirHutangCategory == cat,
                                onClick = { ongkirHutangCategory = cat },
                                label = { Text("Kategori $cat") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("ongkir_cat_$cat")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = ongkirNominalText,
                onValueChange = { ongkirNominalText = it },
                label = { Text("Nominal Ongkir") },
                placeholder = { Text("0") },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_ongkir_nominal"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ongkirDesc,
                onValueChange = { ongkirDesc = it },
                label = { Text("Keterangan Ongkir") },
                placeholder = { Text("Contoh: Ongkir J&T Express / Kurir Toko") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_ongkir_desc"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // ==========================================
        // 3. PEMBELIAN PERSEDIAAN
        // ==========================================
        QuickSectionCard(
            title = "3. Pembelian Persediaan",
            subtitle = "Pengadaan stok bahan cetak, kertas, tinta, dan perlengkapan",
            icon = Icons.Default.AddShoppingCart,
            iconTint = Color(0xFFF59E0B)
        ) {
            Text(
                text = "Metode Pembayaran Pembelian:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kas", "Bank", "Hutang").forEach { opt ->
                    FilterChip(
                        selected = pembelianOption == opt,
                        onClick = { pembelianOption = opt },
                        label = { Text(opt) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("pembelian_chip_$opt")
                    )
                }
            }

            // Opsi Kategori K & Kategori R saat Hutang dipilih
            AnimatedVisibility(
                visible = pembelianOption == "Hutang",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Pilih Kategori Hutang:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("K", "R").forEach { cat ->
                            FilterChip(
                                selected = pembelianHutangCategory == cat,
                                onClick = { pembelianHutangCategory = cat },
                                label = { Text("Kategori $cat") },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("pembelian_cat_$cat")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pembelianNominalText,
                onValueChange = { pembelianNominalText = it },
                label = { Text("Nominal Pembelian Persediaan") },
                placeholder = { Text("0") },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_pembelian_nominal"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pembelianDesc,
                onValueChange = { pembelianDesc = it },
                label = { Text("Keterangan Pembelian") },
                placeholder = { Text("Contoh: Beli Flexi Korea 1 Roll & Tinta CMYK") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_pembelian_desc"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // ==========================================
        // 4. PEMAKAIAN PERSEDIAAN (HPP)
        // ==========================================
        QuickSectionCard(
            title = "4. Pemakaian Persediaan (HPP)",
            subtitle = "Pencatatan beban pokok bahan baku yang telah terpakai untuk produksi",
            icon = Icons.Default.Layers,
            iconTint = Color(0xFF8B5CF6)
        ) {
            OutlinedTextField(
                value = pemakaianNominalText,
                onValueChange = { pemakaianNominalText = it },
                label = { Text("Nominal Pemakaian Persediaan (HPP)") },
                placeholder = { Text("0") },
                prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_pemakaian_nominal"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pemakaianDesc,
                onValueChange = { pemakaianDesc = it },
                label = { Text("Keterangan Pemakaian Persediaan") },
                placeholder = { Text("Contoh: Pemakaian Bahan Cetak Flexi 20 Meter") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_pemakaian_desc"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Ringkasan Total
        if (hasValidEntry) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Transaksi Yang Akan Diposting:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pNom > 0) {
                        SummaryRow("Penjualan (${penjualanOption})", FormatUtils.formatRupiah(pNom), Color(0xFF10B981))
                    }
                    if (oNom > 0) {
                        val oHutangTag = if (ongkirOption == "Hutang") " Kat. $ongkirHutangCategory" else ""
                        SummaryRow("Ongkir (${ongkirOption}$oHutangTag)", FormatUtils.formatRupiah(oNom), Color(0xFF38BDF8))
                    }
                    if (bNom > 0) {
                        val bHutangTag = if (pembelianOption == "Hutang") " Kat. $pembelianHutangCategory" else ""
                        SummaryRow("Pembelian (${pembelianOption}$bHutangTag)", FormatUtils.formatRupiah(bNom), Color(0xFFF59E0B))
                    }
                    if (uNom > 0) {
                        SummaryRow("Pemakaian HPP", FormatUtils.formatRupiah(uNom), Color(0xFF8B5CF6))
                    }
                }
            }
        }

        // Tombol Simpan & Batal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("btn_cancel_quick_tx"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Batal")
            }

            Button(
                onClick = {
                    onPostBatch(
                        pNom, penjualanOption, penjualanDesc,
                        oNom, ongkirOption, ongkirHutangCategory, ongkirDesc,
                        bNom, pembelianOption, pembelianHutangCategory, pembelianDesc,
                        uNom, pemakaianDesc
                    )
                },
                enabled = hasValidEntry,
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp)
                    .testTag("btn_save_quick_tx"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Transaksi Cepat", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun QuickSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
