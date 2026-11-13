package com.example.receipt.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.receipt.data.model.ReceiptCartItem
import com.example.receipt.data.model.ReceiptProduct
import com.example.receipt.data.model.ReceiptStoreSettings
import com.example.receipt.data.model.ReceiptWithItems
import com.example.ui.theme.EmeraldGain
import com.example.ui.theme.RoseLoss
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptGeneratorScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceiptViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val allReceipts by viewModel.allReceipts.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val receiptNumberInput by viewModel.receiptNumberInput.collectAsStateWithLifecycle()
    val paymentMethodInput by viewModel.paymentMethodInput.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val selectedReceiptForPreview by viewModel.selectedReceiptForPreview.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Buat Struk", "Pratinjau", "Produk Master", "Riwayat Struk", "Toko")

    // Collect UI messages
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is ReceiptUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.text)
                }
                is ReceiptUiEvent.ReceiptGenerated -> {
                    // Switch to preview tab when receipt is generated
                    selectedTabIndex = 1
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.testTag("receipt_generator_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Struk & Nota",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("btn_close_receipt_screen")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("receipt_tab_$index")
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> GenerateReceiptTab(
                    receiptNumber = receiptNumberInput,
                    onReceiptNumberChange = { viewModel.setReceiptNumber(it) },
                    paymentMethod = paymentMethodInput,
                    onPaymentMethodChange = { viewModel.setPaymentMethod(it) },
                    cartItems = cartItems,
                    products = allProducts,
                    onAddToCart = { prod, qty, price -> viewModel.addProductToCart(prod, qty, price) },
                    onUpdateQty = { idx, qty -> viewModel.updateCartItemQuantity(idx, qty) },
                    onUpdatePrice = { idx, prc -> viewModel.updateCartItemCustomPrice(idx, prc) },
                    onRemoveItem = { idx -> viewModel.removeCartItem(idx) },
                    onClearCart = { viewModel.clearCart() },
                    onGenerate = { viewModel.generateReceipt() }
                )
                1 -> PreviewReceiptTab(
                    receiptWithItems = selectedReceiptForPreview,
                    whatsAppText = selectedReceiptForPreview?.let { viewModel.getWhatsAppShareText(it) } ?: "",
                    onNavigateToGenerate = { selectedTabIndex = 0 }
                )
                2 -> ProductMasterTab(
                    products = allProducts,
                    onAddProduct = { sku, name, price, done -> viewModel.addNewProduct(sku, name, price, done) },
                    onUpdateProduct = { prod, done -> viewModel.updateProduct(prod, done) },
                    onDeleteProduct = { prod -> viewModel.deleteProduct(prod) },
                    onImportCsv = { csv, done -> viewModel.importCsv(csv, done) },
                    onExportCsv = { viewModel.exportCsv() },
                    getTemplateCsv = { viewModel.getTemplateCsv() }
                )
                3 -> ReceiptHistoryTab(
                    receipts = allReceipts,
                    onSelectReceipt = { receipt ->
                        viewModel.selectReceiptForPreview(receipt)
                        selectedTabIndex = 1 // Switch to preview
                    },
                    onDeleteReceipt = { receipt -> viewModel.deleteReceipt(receipt) },
                    onExportAllReceipts = { viewModel.exportAllReceiptsJson() },
                    onImportAllReceipts = { json -> viewModel.importReceiptsFromJson(json) }
                )
                4 -> StoreSettingsTab(
                    settings = storeSettings,
                    onSaveSettings = { updated ->
                        viewModel.updateStoreSettings(updated)
                        Toast.makeText(context, "Pengaturan toko berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// =========================================================================
// TAB 1: GENERATE STRUK BARU
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateReceiptTab(
    receiptNumber: String,
    onReceiptNumberChange: (String) -> Unit,
    paymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    cartItems: List<ReceiptCartItem>,
    products: List<ReceiptProduct>,
    onAddToCart: (ReceiptProduct, Int, Double?) -> Unit,
    onUpdateQty: (Int, Int) -> Unit,
    onUpdatePrice: (Int, Double) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearCart: () -> Unit,
    onGenerate: () -> Unit
) {
    var showProductPicker by remember { mutableStateOf(false) }
    var expandedPaymentDropdown by remember { mutableStateOf(false) }
    val paymentOptions = listOf("Lunas", "Belum Dibayar")

    val nf = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val totalAmount = cartItems.sumOf { it.subtotal }
    val totalQty = cartItems.sumOf { it.quantity }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Receipt Header Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Parameter Struk Baru",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = receiptNumber,
                            onValueChange = onReceiptNumberChange,
                            label = { Text("Nomor Struk (Otomatis / Manual)") },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("input_receipt_number"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold)
                        )

                        // Payment Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedPaymentDropdown,
                            onExpandedChange = { expandedPaymentDropdown = !expandedPaymentDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = paymentMethod,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaymentDropdown) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedPaymentDropdown,
                                onDismissRequest = { expandedPaymentDropdown = false }
                            ) {
                                paymentOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            onPaymentMethodChange(opt)
                                            expandedPaymentDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action to Add Product
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Produk Struk (${cartItems.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { showProductPicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_open_product_picker")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pilih Produk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cart Items Empty or List
        if (cartItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Belum ada produk yang dimasukkan. Tekan '+ Pilih Produk' di atas.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            itemsIndexed(cartItems) { index, item ->
                ReceiptCartItemRow(
                    item = item,
                    onUpdateQty = { qty -> onUpdateQty(index, qty) },
                    onUpdatePrice = { prc -> onUpdatePrice(index, prc) },
                    onRemove = { onRemoveItem(index) }
                )
            }
        }

        // Summary Bar & Generate Button
        if (cartItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Produk:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${cartItems.size} jenis (${totalQty} pcs)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL RUPIAH:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = nf.format(totalAmount).replace("Rp", "Rp "),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onClearCart,
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Kosongkan", fontSize = 12.sp, color = RoseLoss)
                            }

                            Button(
                                onClick = onGenerate,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("btn_generate_receipt"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Struk", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Product Picker
    if (showProductPicker) {
        ProductPickerModal(
            products = products,
            onDismiss = { showProductPicker = false },
            onProductSelected = { prod, qty, customPrice ->
                onAddToCart(prod, qty, customPrice)
                showProductPicker = false
            }
        )
    }
}

@Composable
fun ReceiptCartItemRow(
    item: ReceiptCartItem,
    onUpdateQty: (Int) -> Unit,
    onUpdatePrice: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    var isEditingPrice by remember { mutableStateOf(false) }
    var priceInput by remember { mutableStateOf(item.customPrice.toLong().toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = "SKU: ${item.sku}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus", tint = RoseLoss, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity changer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { onUpdateQty(item.quantity - 1) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "  ${item.quantity}  ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    OutlinedButton(
                        onClick = { onUpdateQty(item.quantity + 1) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }

                // Custom Price display & edit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditingPrice) {
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.width(110.dp),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            val prc = priceInput.toDoubleOrNull() ?: item.customPrice
                            onUpdatePrice(prc)
                            isEditingPrice = false
                        }) {
                            Text("OK", fontWeight = FontWeight.Bold, color = EmeraldGain)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "@ ${nf.format(item.customPrice).replace("Rp", "Rp ")}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Ubah harga",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { isEditingPrice = true },
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Sub: ${nf.format(item.subtotal).replace("Rp", "Rp ")}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// MODAL: PRODUCT PICKER
// =========================================================================

@Composable
fun ProductPickerModal(
    products: List<ReceiptProduct>,
    onDismiss: () -> Unit,
    onProductSelected: (ReceiptProduct, Int, Double?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ReceiptProduct?>(null) }
    var quantityText by remember { mutableStateOf("1") }
    var customPriceText by remember { mutableStateOf("") }

    val filtered = products.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true)
    }

    val nf = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pilih Produk Dari Master", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari produk atau SKU...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedProduct == null) {
                    LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered) { prod ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProduct = prod
                                        customPriceText = prod.unitPrice.toLong().toString()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = prod.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(text = "SKU: ${prod.sku}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = nf.format(prod.unitPrice).replace("Rp", "Rp "),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Selected product customization
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = selectedProduct!!.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "Ganti",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { selectedProduct = null }
                            )
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah (Kuantitas)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customPriceText,
                            onValueChange = { customPriceText = it.filter { c -> c.isDigit() } },
                            label = { Text("Harga Satuan Khusus Struk Ini (Rp)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            text = "* Mengubah harga di sini tidak akan mengubah harga di database produk master.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedProduct?.let { prod ->
                        val qty = quantityText.toIntOrNull() ?: 1
                        val customPrice = customPriceText.toDoubleOrNull()
                        onProductSelected(prod, qty.coerceAtLeast(1), customPrice)
                    }
                },
                enabled = selectedProduct != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Tambahkan ke Struk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// =========================================================================
// TAB 2: PRATINJAU STRUK THERMAL
// =========================================================================

@Composable
fun PreviewReceiptTab(
    receiptWithItems: ReceiptWithItems?,
    whatsAppText: String,
    onNavigateToGenerate: () -> Unit
) {
    if (receiptWithItems == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Belum ada struk yang dipilih atau di-generate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onNavigateToGenerate,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Buat Struk Sekarang")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ReceiptActionButtons(
                    receiptWithItems = receiptWithItems,
                    whatsAppText = whatsAppText
                )
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ThermalReceiptCard(receiptWithItems = receiptWithItems)
                }
            }
        }
    }
}

// =========================================================================
// TAB 3: PRODUK MASTER
// =========================================================================

@Composable
fun ProductMasterTab(
    products: List<ReceiptProduct>,
    onAddProduct: (String, String, Double, (Boolean) -> Unit) -> Unit,
    onUpdateProduct: (ReceiptProduct, (Boolean) -> Unit) -> Unit,
    onDeleteProduct: (ReceiptProduct) -> Unit,
    onImportCsv: (String, () -> Unit) -> Unit,
    onExportCsv: suspend () -> String,
    getTemplateCsv: () -> String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ReceiptProduct?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCsvText by remember { mutableStateOf("") }
    var showTemplateDialog by remember { mutableStateOf(false) }

    val filtered = products.filter {
        searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true)
    }

    val nf = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Actions Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f).testTag("btn_add_master_product"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Produk", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Impor CSV", fontSize = 12.sp)
                }
            }
        }

        // CSV Quick Tools Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showTemplateDialog = true }) {
                    Text("Unduh Template CSV", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = {
                    coroutineScope.launch {
                        val csv = onExportCsv()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Export Produk", csv))
                        Toast.makeText(context, "Data CSV produk disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Ekspor CSV Produk", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari SKU atau nama produk...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        items(filtered, key = { it.id }) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = product.sku,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Harga Satuan: ${nf.format(product.unitPrice).replace("Rp", "Rp ")}",
                            fontSize = 12.sp,
                            color = EmeraldGain,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row {
                        IconButton(onClick = { editingProduct = product }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { onDeleteProduct(product) }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = RoseLoss, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal Tambah Produk
    if (showAddDialog) {
        ProductFormDialog(
            title = "Tambah Produk Baru",
            initialProduct = null,
            onDismiss = { showAddDialog = false },
            onSave = { sku, name, price ->
                onAddProduct(sku, name, price) { success ->
                    if (success) showAddDialog = false
                }
            }
        )
    }

    // Modal Edit Produk
    editingProduct?.let { product ->
        ProductFormDialog(
            title = "Edit Produk ${product.sku}",
            initialProduct = product,
            onDismiss = { editingProduct = null },
            onSave = { sku, name, price ->
                onUpdateProduct(product.copy(sku = sku, name = name, unitPrice = price)) { success ->
                    if (success) editingProduct = null
                }
            }
        )
    }

    // Modal Import CSV
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Impor Produk CSV", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tempelkan isi file CSV dengan format:\nSKU,Nama Produk,Harga Satuan", fontSize = 12.sp)
                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCsv(importCsvText) {
                            showImportDialog = false
                            importCsvText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Proses Impor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Batal") }
            }
        )
    }

    // Modal Download Template CSV
    if (showTemplateDialog) {
        val tpl = getTemplateCsv()
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("Template File CSV", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tpl,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Template CSV", tpl))
                        Toast.makeText(context, "Template CSV disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        showTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Salin Template")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    initialProduct: ReceiptProduct?,
    onDismiss: () -> Unit,
    onSave: (sku: String, name: String, price: Double) -> Unit
) {
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var priceText by remember { mutableStateOf(initialProduct?.unitPrice?.toLong()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it.uppercase() },
                    label = { Text("SKU Produk (Unik)") },
                    placeholder = { Text("Contoh: BNR-280") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    placeholder = { Text("Contoh: Banner Flexi 280gsm") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Harga Satuan Master (Rp)") },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prc = priceText.toDoubleOrNull() ?: 0.0
                    onSave(sku.trim(), name.trim(), prc)
                },
                enabled = sku.isNotBlank() && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// =========================================================================
// TAB 4: RIWAYAT STRUK
// =========================================================================

@Composable
fun ReceiptHistoryTab(
    receipts: List<ReceiptWithItems>,
    onSelectReceipt: (ReceiptWithItems) -> Unit,
    onDeleteReceipt: (ReceiptWithItems) -> Unit,
    onExportAllReceipts: suspend () -> String,
    onImportAllReceipts: suspend (String) -> Result<Int>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val nf = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    var deletingReceipt by remember { mutableStateOf<ReceiptWithItems?>(null) }
    var pendingExportJsonForSaf by remember { mutableStateOf<String?>(null) }

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
                    Toast.makeText(context, "Semua data struk berhasil diekspor ke direktori tujuan!", Toast.LENGTH_LONG).show()
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
                        val result = onImportAllReceipts(content)
                        result.onSuccess { count ->
                            Toast.makeText(context, "Berhasil mengimpor $count data struk!", Toast.LENGTH_LONG).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "Gagal impor: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Berkas yang dipilih kosong", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca berkas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Export & Import Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cadangan Data Struk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ekspor dan impor semua data struk yang tersimpan (bisa pilih direktori tujuan penyimpanan).",
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
                                if (receipts.isEmpty()) {
                                    Toast.makeText(context, "Belum ada data struk untuk diekspor", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        val json = onExportAllReceipts()
                                        pendingExportJsonForSaf = json
                                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale("id", "ID")).format(Date())
                                        createDocumentLauncher.launch("backup_semua_struk_$dateStr.json")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_all_receipts"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor Struk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_import_all_receipts"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor Struk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (receipts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, bottom = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada riwayat struk yang tersimpan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(receipts, key = { it.receipt.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectReceipt(item) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.receipt.receiptNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { deletingReceipt = item },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = RoseLoss, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.receipt.formattedDateTime} • ${item.receipt.paymentMethod}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.items.size} jenis (${item.receipt.totalQuantity} pcs)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = nf.format(item.receipt.totalAmount).replace("Rp", "Rp "),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = EmeraldGain
                            )
                        }
                    }
                }
            }
        }
    }

    deletingReceipt?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingReceipt = null },
            title = { Text("Hapus Struk ${target.receipt.receiptNumber}?") },
            text = { Text("Struk ini akan dihapus dari riwayat. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReceipt(target)
                        deletingReceipt = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseLoss)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReceipt = null }) { Text("Batal") }
            }
        )
    }
}

// =========================================================================
// TAB 5: PENGATURAN TOKO
// =========================================================================

@Composable
fun StoreSettingsTab(
    settings: ReceiptStoreSettings,
    onSaveSettings: (ReceiptStoreSettings) -> Unit
) {
    var storeName by remember(settings.storeName) { mutableStateOf(settings.storeName) }
    var address by remember(settings.address) { mutableStateOf(settings.address) }
    var phone by remember(settings.phone) { mutableStateOf(settings.phone) }
    var prefix by remember(settings.receiptPrefix) { mutableStateOf(settings.receiptPrefix) }
    var startNum by remember(settings.startNumber) { mutableStateOf(settings.startNumber.toString()) }
    var footer1 by remember(settings.footerNote1) { mutableStateOf(settings.footerNote1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Profil Toko & Format Struk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = {
                            storeName = it
                            val startInt = startNum.toIntOrNull() ?: 1
                            onSaveSettings(
                                settings.copy(
                                    storeName = it.trim(),
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    receiptPrefix = prefix.trim(),
                                    startNumber = startInt.coerceAtLeast(1),
                                    footerNote1 = footer1.trim()
                                )
                            )
                        },
                        label = { Text("Nama Toko") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            val startInt = startNum.toIntOrNull() ?: 1
                            onSaveSettings(
                                settings.copy(
                                    storeName = storeName.trim(),
                                    address = it.trim(),
                                    phone = phone.trim(),
                                    receiptPrefix = prefix.trim(),
                                    startNumber = startInt.coerceAtLeast(1),
                                    footerNote1 = footer1.trim()
                                )
                            )
                        },
                        label = { Text("Alamat Toko") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            val startInt = startNum.toIntOrNull() ?: 1
                            onSaveSettings(
                                settings.copy(
                                    storeName = storeName.trim(),
                                    address = address.trim(),
                                    phone = it.trim(),
                                    receiptPrefix = prefix.trim(),
                                    startNumber = startInt.coerceAtLeast(1),
                                    footerNote1 = footer1.trim()
                                )
                            )
                        },
                        label = { Text("Nomor Telepon") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prefix,
                            onValueChange = {
                                prefix = it
                                val startInt = startNum.toIntOrNull() ?: 1
                                onSaveSettings(
                                    settings.copy(
                                        storeName = storeName.trim(),
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        receiptPrefix = it.trim(),
                                        startNumber = startInt.coerceAtLeast(1),
                                        footerNote1 = footer1.trim()
                                    )
                                )
                            },
                            label = { Text("Prefix Nomor") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = startNum,
                            onValueChange = {
                                val clean = it.filter { c -> c.isDigit() }
                                startNum = clean
                                val startInt = clean.toIntOrNull() ?: 1
                                onSaveSettings(
                                    settings.copy(
                                        storeName = storeName.trim(),
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        receiptPrefix = prefix.trim(),
                                        startNumber = startInt.coerceAtLeast(1),
                                        footerNote1 = footer1.trim()
                                    )
                                )
                            },
                            label = { Text("Nomor Awal") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = footer1,
                        onValueChange = {
                            footer1 = it
                            val startInt = startNum.toIntOrNull() ?: 1
                            onSaveSettings(
                                settings.copy(
                                    storeName = storeName.trim(),
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    receiptPrefix = prefix.trim(),
                                    startNumber = startInt.coerceAtLeast(1),
                                    footerNote1 = it.trim()
                                )
                            )
                        },
                        label = { Text("Pesan Penutup Struk") },
                        modifier = Modifier.fillMaxWidth().testTag("input_store_footer_note")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val startInt = startNum.toIntOrNull() ?: 1
                            onSaveSettings(
                                settings.copy(
                                    storeName = storeName.trim(),
                                    address = address.trim(),
                                    phone = phone.trim(),
                                    receiptPrefix = prefix.trim(),
                                    startNumber = startInt.coerceAtLeast(1),
                                    footerNote1 = footer1.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Simpan Pengaturan Struk", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
