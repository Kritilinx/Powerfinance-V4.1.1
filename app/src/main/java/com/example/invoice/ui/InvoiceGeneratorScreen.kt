package com.example.invoice.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.invoice.data.model.InvoiceDraftItem
import com.example.invoice.data.model.InvoiceWithItems
import com.example.invoice.util.InvoicePdfGenerator
import com.example.receipt.data.model.ReceiptProduct
import kotlinx.coroutines.flow.collectLatest

private val EmeraldGreen = Color(0xFF047857)
private val LightEmeraldBg = Color(0xFFECFDF5)
private val SlateDark = Color(0xFF0F172A)
private val BorderColor = Color(0xFFCBD5E1)

// Colors matching exact A4 sample reference
private val ColorInvoiceNavy = Color(0xFF3B4E87)
private val ColorInvoiceTitleBlue = Color(0xFF7A8DC5)
private val ColorCommentsBar = Color(0xFFBFBFBF)
private val ColorCommentsBorder = Color(0xFFA6A6A6)
private val ColorTotalsCellBg = Color(0xFFF2F2F2)
private val ColorTotalRowHighlight = Color(0xFFA7B4D8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceGeneratorScreen(
    onClose: () -> Unit,
    viewModel: InvoiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val previewInvoice by viewModel.previewInvoice.collectAsStateWithLifecycle()

    var showProductPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { msg ->
            when (msg) {
                is InvoiceUiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is InvoiceUiMessage.Error -> snackbarHostState.showSnackbar("Kesalahan: ${msg.message}")
                is InvoiceUiMessage.Info -> snackbarHostState.showSnackbar(msg.message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Generate Invoice",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Format Resmi A4 • Ekspor PDF & WhatsApp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close_invoice")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (currentTab == 0) {
                        IconButton(
                            onClick = { viewModel.resetForm() },
                            modifier = Modifier.testTag("btn_reset_invoice_form")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Form")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    text = { Text("Buat Invoice", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_create_invoice")
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    text = { Text("Riwayat Invoice", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_invoice_history")
                )
            }

            when (currentTab) {
                0 -> CreateInvoiceTab(
                    viewModel = viewModel,
                    onOpenProductPicker = { showProductPickerDialog = true },
                    onPreviewDraft = {
                        viewModel.generateAndSaveInvoice(context)
                    }
                )
                1 -> InvoiceHistoryTab(
                    viewModel = viewModel,
                    onOpenPreview = { inv -> viewModel.openPreview(inv) }
                )
            }
        }
    }

    // Searchable Catalog Product Picker Dialog (Read-only from Receipt database)
    if (showProductPickerDialog) {
        val catalogProducts by viewModel.catalogProducts.collectAsStateWithLifecycle()
        ProductPickerDialog(
            products = catalogProducts,
            onDismiss = { showProductPickerDialog = false },
            onSelectProduct = { product ->
                viewModel.addProductFromCatalog(product)
                showProductPickerDialog = false
                Toast.makeText(context, "Produk '${product.name}' ditambahkan", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal Preview Invoice (High-Fidelity A4 preview)
    previewInvoice?.let { inv ->
        InvoicePreviewDialog(
            invoiceWithItems = inv,
            onDismiss = { viewModel.closePreview() },
            onDownloadPdf = { viewModel.downloadInvoicePdf(context, inv) },
            onShare = { targetApp -> viewModel.shareInvoice(context, inv, targetApp) }
        )
    }
}

@Composable
private fun CreateInvoiceTab(
    viewModel: InvoiceViewModel,
    onOpenProductPicker: () -> Unit,
    onPreviewDraft: () -> Unit
) {
    val context = LocalContext.current
    val invoiceNumber by viewModel.invoiceNumber.collectAsStateWithLifecycle()
    val isNumberDuplicate by viewModel.isNumberDuplicate.collectAsStateWithLifecycle()
    val dateFormatted by viewModel.dateFormatted.collectAsStateWithLifecycle()
    val customerName by viewModel.customerName.collectAsStateWithLifecycle()
    val customerPhone by viewModel.customerPhone.collectAsStateWithLifecycle()
    val customerAddress by viewModel.customerAddress.collectAsStateWithLifecycle()

    val items by viewModel.items.collectAsStateWithLifecycle()
    val subtotal by viewModel.subtotal.collectAsStateWithLifecycle()
    val discountText by viewModel.discountText.collectAsStateWithLifecycle()
    val taxText by viewModel.taxText.collectAsStateWithLifecycle()
    val shippingText by viewModel.shippingText.collectAsStateWithLifecycle()
    val otherText by viewModel.otherText.collectAsStateWithLifecycle()
    val totalAmount by viewModel.totalAmount.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // --- 1. NOMOR INVOICE & TANGGAL CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_invoice_meta"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Informasi Dokumen",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Standar A4",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { viewModel.updateInvoiceNumber(it) },
                        label = { Text("Nomor Invoice") },
                        isError = isNumberDuplicate,
                        supportingText = {
                            if (isNumberDuplicate) {
                                Text("Nomor sudah digunakan! Harap ganti.", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Contoh format: INV/NDP/2026/11/6/002513 (Otomatis & Editable)")
                            }
                        },
                        trailingIcon = {
                            if (isNumberDuplicate) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_invoice_number"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dateFormatted,
                        onValueChange = { viewModel.updateDate(it) },
                        label = { Text("Tanggal Invoice (DD/MM/YYYY)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_invoice_date"),
                        singleLine = true
                    )
                }
            }
        }

        // --- 2. BILL TO (CUSTOMER INFO) CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_bill_to"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "BILL TO (Pelanggan / Perusahaan)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.updateCustomerName(it) },
                        label = { Text("Nama Pelanggan / Perusahaan") },
                        placeholder = { Text("PT. Maju Bersama / Bapak Rudi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { viewModel.updateCustomerPhone(it) },
                        label = { Text("Nomor Telepon / WhatsApp") },
                        placeholder = { Text("0812-xxxx-xxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_phone"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customerAddress,
                        onValueChange = { viewModel.updateCustomerAddress(it) },
                        label = { Text("Alamat Pelanggan / Kota (Opsional)") },
                        placeholder = { Text("Jl. Sudirman No. 45, Jakarta") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_customer_address")
                    )
                }
            }
        }

        // --- 3. BARIS PRODUK / ITEMS TABLE CARD ---
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Daftar Item Produk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${items.size} Baris",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(items) { index, item ->
            ItemRowCard(
                item = item,
                onUpdate = { updated -> viewModel.updateItem(index, updated) },
                onDelete = { viewModel.removeItem(index) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenProductPicker,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_pick_catalog_product"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pilih Produk Database", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = { viewModel.addBlankItem() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_manual_item"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Baris Manual", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // --- 4. FINANCIAL BREAKDOWN & SPECIAL INSTRUCTIONS ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_financial_breakdown"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Rincian Biaya & Kalkulasi Otomatis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Subtotal Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SUBTOTAL", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = InvoicePdfGenerator.formatRupiah(subtotal),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Discount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discount (Rp)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { viewModel.updateDiscount(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("input_discount"),
                            singleLine = true
                        )
                    }

                    // TAX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TAX (Pajak Rp)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = taxText,
                            onValueChange = { viewModel.updateTax(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("input_tax"),
                            singleLine = true
                        )
                    }

                    // S&H
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("S&H (Ongkir Rp)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = shippingText,
                            onValueChange = { viewModel.updateShipping(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("input_shipping"),
                            singleLine = true
                        )
                    }

                    // OTHER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OTHER (Lainnya Rp)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = otherText,
                            onValueChange = { viewModel.updateOther(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("input_other"),
                            singleLine = true
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // TOTAL HIGHLIGHT
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL INVOICE",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = InvoicePdfGenerator.formatRupiah(totalAmount),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }

        // --- 5. OTHER COMMENTS OR SPECIAL INSTRUCTIONS ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_comments"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Other Comments or Special Instructions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = comments,
                        onValueChange = { viewModel.updateComments(it) },
                        placeholder = {
                            Text("1. Pembayaran via Transfer Rekening BCA 123-456-7890 a/n Diprint.\n2. Kirim bukti transfer via WA.")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("input_comments")
                    )
                }
            }
        }

        // --- 6. ACTION BUTTONS ---
        item {
            Button(
                onClick = onPreviewDraft,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_generate_and_preview_invoice"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                enabled = !isNumberDuplicate && items.isNotEmpty()
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate & Preview Invoice (A4)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ItemRowCard(
    item: InvoiceDraftItem,
    onUpdate: (InvoiceDraftItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.itemNumber}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldGreen
                    ) {
                        Text(
                            text = if (item.sku.isNotBlank()) "SKU: ${item.sku}" else "ITEM #${item.itemNumber}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.sku.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.sku,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            OutlinedTextField(
                value = item.description,
                onValueChange = { onUpdate(item.copy(description = it)) },
                label = { Text("Deskripsi Produk / Jasa") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = if (item.quantity == 0) "" else item.quantity.toString(),
                    onValueChange = { str ->
                        val q = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                        onUpdate(item.copy(quantity = q))
                    },
                    label = { Text("QTY") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = if (item.unitPrice == 0.0) "" else String.format("%.0f", item.unitPrice),
                    onValueChange = { str ->
                        val p = str.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(unitPrice = p))
                    },
                    label = { Text("Harga Satuan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Baris: ${InvoicePdfGenerator.formatRupiah(item.lineTotal)}",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun InvoiceHistoryTab(
    viewModel: InvoiceViewModel,
    onOpenPreview: (InvoiceWithItems) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredInvoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()

    var pendingExportJsonForSaf by remember { mutableStateOf<String?>(null) }
    var showConfirmImportDialog by remember { mutableStateOf<String?>(null) }
    var invoiceToDelete by remember { mutableStateOf<InvoiceWithItems?>(null) }

    // SAF Create Document launcher for saving export file to user-selected directory
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            val json = pendingExportJsonForSaf
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "Semua data invoice berhasil diekspor!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal mengekspor berkas: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    pendingExportJsonForSaf = null
                }
            }
        }
    }

    // SAF Open Document launcher for importing backup file from any user-selected directory
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(fileUri)?.use { stream ->
                        stream.bufferedReader().use { reader -> reader.readText() }
                    }
                    if (!content.isNullOrBlank()) {
                        showConfirmImportDialog = content
                    } else {
                        Toast.makeText(context, "Berkas yang dipilih kosong", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca berkas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showConfirmImportDialog != null) {
        AlertDialog(
            onDismissRequest = { showConfirmImportDialog = null },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = EmeraldGreen) },
            title = { Text("Konfirmasi Impor Invoice", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin memulihkan data invoice dari berkas cadangan ini? Data invoice yang sudah ada akan diperbarui dan data baru akan ditambahkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val contentToImport = showConfirmImportDialog
                        showConfirmImportDialog = null
                        if (contentToImport != null) {
                            scope.launch {
                                viewModel.importInvoicesFromJson(contentToImport)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.testTag("btn_confirm_import_invoices")
                ) {
                    Text("Impor Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmImportDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (invoiceToDelete != null) {
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Invoice", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus invoice ${invoiceToDelete?.invoice?.invoiceNumber}? Data ini akan dihapus secara permanen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        invoiceToDelete?.let { viewModel.deleteInvoice(it) }
                        invoiceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_invoice")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { invoiceToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Export & Import Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cadangan Data Invoice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ekspor dan impor semua data invoice yang tersimpan (format JSON, simpan ke memori atau Google Drive).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (allInvoices.isEmpty()) {
                                Toast.makeText(context, "Belum ada data invoice untuk diekspor", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    val json = viewModel.exportAllInvoicesJson()
                                    pendingExportJsonForSaf = json
                                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale("id", "ID")).format(Date())
                                    createDocumentLauncher.launch("backup_semua_invoice_$dateStr.json")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_all_invoices"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor Invoice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_import_all_invoices"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Impor Invoice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            label = { Text("Cari Invoice (Nomor / Nama Pelanggan / Telepon)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_invoices"),
            singleLine = true
        )

        if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Belum ada invoice yang dibuat." else "Tidak ada invoice yang cocok dengan pencarian.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredInvoices) { inv ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPreview(inv) }
                            .testTag("card_invoice_history_${inv.invoice.id}"),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inv.invoice.invoiceNumber,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EmeraldGreen
                                )
                                Text(
                                    text = inv.invoice.dateFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "Kepada: ${inv.invoice.customerName.ifBlank { "Pelanggan Umum" }}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (inv.invoice.customerPhone.isNotBlank()) {
                                Text(
                                    text = "Telp: ${inv.invoice.customerPhone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${inv.items.size} item produk",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = InvoicePdfGenerator.formatRupiah(inv.invoice.totalAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldGreen
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { onOpenPreview(inv) },
                                    modifier = Modifier.testTag("btn_preview_${inv.invoice.id}")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview")
                                }

                                TextButton(
                                    onClick = { viewModel.downloadInvoicePdf(context, inv) },
                                    modifier = Modifier.testTag("btn_download_${inv.invoice.id}")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download PDF")
                                }

                                TextButton(
                                    onClick = { viewModel.shareInvoice(context, inv) },
                                    modifier = Modifier.testTag("btn_share_${inv.invoice.id}")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }

                                TextButton(
                                    onClick = { invoiceToDelete = inv },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("btn_delete_${inv.invoice.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Product Picker Dialog reading master products from the receipt database
 */
@Composable
private fun ProductPickerDialog(
    products: List<ReceiptProduct>,
    onDismiss: () -> Unit,
    onSelectProduct: (ReceiptProduct) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, products) {
        if (query.isBlank()) products
        else products.filter {
            it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pilih Produk dari Database",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari produk percetakan / SKU...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_picker_search"),
                    singleLine = true
                )

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Tidak ada produk ditemukan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectProduct(product) }
                                    .testTag("picker_item_${product.id}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "SKU: ${product.sku}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = InvoicePdfGenerator.formatRupiah(product.unitPrice),
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * High-fidelity Dialog previewing the generated A4 Invoice layout
 */
@Composable
private fun InvoicePreviewDialog(
    invoiceWithItems: InvoiceWithItems,
    onDismiss: () -> Unit,
    onDownloadPdf: () -> Unit,
    onShare: (targetApp: String?) -> Unit
) {
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preview Invoice: ${inv.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Scrollable A4 Visual Sheet
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE2E8F0))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Simulated A4 Sheet (White Background, crisp borders)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(4.dp)),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Header: Diprint Logo on Left & Title/Date on Right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_diprint_invoice_logo),
                                    contentDescription = "Diprint Indonesia",
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(78.dp),
                                    contentScale = ContentScale.Fit
                                )

                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "INVOICE",
                                        fontWeight = FontWeight.Bold,
                                        color = ColorInvoiceTitleBlue,
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = inv.invoiceNumber,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "DATE:  ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = inv.dateFormatted,
                                            fontSize = 9.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            // 2. BILL TO Section
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .padding(top = 8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ColorInvoiceNavy
                                ) {
                                    Text(
                                        text = "BILL TO:",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = inv.customerName.ifBlank { "Pelanggan Umum" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color.Black
                                )
                                if (inv.customerPhone.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = inv.customerPhone,
                                        fontSize = 9.5.sp,
                                        color = Color.Black
                                    )
                                }
                            }

                            // 3. Items Table with Exact 6 Columns and Vertical Dividers
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Black)
                            ) {
                                // Table Header Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ColorInvoiceNavy)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SKU", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.5.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.5f)))
                                    Text("DESCRIPTION", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.5.sp, modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.5f)))
                                    Text("QTY", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.5.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.5f)))
                                    Text("UNIT PRICE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.5.sp, modifier = Modifier.width(64.dp).padding(end = 4.dp), textAlign = TextAlign.End)
                                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.5f)))
                                    Text("TAX", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.5.sp, modifier = Modifier.width(26.dp).padding(end = 2.dp), textAlign = TextAlign.End)
                                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.White.copy(alpha = 0.5f)))
                                    Text("TOTAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.5.sp, modifier = Modifier.width(66.dp).padding(end = 4.dp), textAlign = TextAlign.End)
                                }

                                // Table Item Rows (Divider lines only vertical, matching sample PDF)
                                Column(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                    items.forEach { itm ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(itm.sku.ifBlank { "SKU-${itm.itemNumber}" }, fontSize = 8.sp, color = Color.Black, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.Black.copy(alpha = 0.3f)))
                                            Text(itm.description, fontSize = 8.5.sp, color = Color.Black, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.Black.copy(alpha = 0.3f)))
                                            Text("${itm.quantity}", fontSize = 8.5.sp, color = Color.Black, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.Black.copy(alpha = 0.3f)))
                                            Text(InvoicePdfGenerator.formatRupiah(itm.unitPrice), fontSize = 8.5.sp, color = Color.Black, modifier = Modifier.width(64.dp).padding(end = 4.dp), textAlign = TextAlign.End)
                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.Black.copy(alpha = 0.3f)))
                                            Text(if (itm.tax > 0) InvoicePdfGenerator.formatRupiah(itm.tax) else "Rp0", fontSize = 8.5.sp, color = Color.Black, modifier = Modifier.width(26.dp).padding(end = 2.dp), textAlign = TextAlign.End)
                                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.Black.copy(alpha = 0.3f)))
                                            Text(InvoicePdfGenerator.formatRupiah(itm.lineTotal), fontWeight = FontWeight.SemiBold, fontSize = 8.5.sp, color = Color.Black, modifier = Modifier.width(66.dp).padding(end = 4.dp), textAlign = TextAlign.End)
                                        }
                                    }
                                }
                            }

                            // 4. Bottom Section: Instructions on Left & Financial Totals on Right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Comments Box (Left)
                                Box(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .border(1.dp, ColorCommentsBorder)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = ColorCommentsBar
                                        ) {
                                            Text(
                                                text = "Other Comments or Special Instructions",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = inv.comments.ifBlank {
                                                InvoiceViewModel.DEFAULT_INVOICE_COMMENTS
                                            },
                                            fontSize = 7.5.sp,
                                            color = Color.Black,
                                            lineHeight = 11.sp,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }

                                // Financial Breakdown (Right)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    @Composable
                                    fun totalRowCell(lbl: String, amt: Double) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lbl,
                                                fontSize = 8.5.sp,
                                                color = Color.Black,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .height(18.dp)
                                                    .background(ColorTotalsCellBg)
                                                    .border(0.5.dp, Color(0xFFD1D5DB))
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Text(
                                                    text = InvoicePdfGenerator.formatRupiah(amt),
                                                    fontSize = 8.sp,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }

                                    totalRowCell("SUBTOTAL", inv.subtotal)
                                    totalRowCell("Discount", inv.discount)
                                    totalRowCell("TAX", inv.tax)
                                    totalRowCell("S&H", inv.shippingAndHandling)
                                    totalRowCell("OTHER", inv.other)

                                    // Double line divider above TOTAL
                                    Column(modifier = Modifier.width(130.dp).padding(vertical = 2.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
                                    }

                                    // TOTAL Highlight Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "TOTAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(68.dp)
                                                .height(20.dp)
                                                .background(ColorTotalRowHighlight)
                                                .border(1.dp, Color(0xFF8B9DC3))
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Text(
                                                text = InvoicePdfGenerator.formatRupiah(inv.totalAmount),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }

                            // Footer Note
                            Text(
                                text = "If you have any questions about this invoice, please contact",
                                fontSize = 8.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            )
                            Text(
                                text = "Thank you for your business!",
                                fontWeight = FontWeight.Bold,
                                color = ColorInvoiceNavy,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom Action Bar: Download PDF & Share
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDownloadPdf,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_modal_download_pdf"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download PDF")
                            }

                            Button(
                                onClick = { onShare(null) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_modal_share_all"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share PDF")
                            }
                        }

                        // Targeted WhatsApp & WhatsApp Business Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onShare("com.whatsapp") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_share_whatsapp"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF25D366))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { onShare("com.whatsapp.w4b") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_share_whatsapp_business"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF128C7E))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WA Business", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
