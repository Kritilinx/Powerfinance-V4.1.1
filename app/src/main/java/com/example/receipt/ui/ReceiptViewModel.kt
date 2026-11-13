package com.example.receipt.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.receipt.data.ReceiptDatabase
import com.example.receipt.data.ReceiptRepository
import com.example.receipt.data.model.ReceiptCartItem
import com.example.receipt.data.model.ReceiptProduct
import com.example.receipt.data.model.ReceiptRecord
import com.example.receipt.data.model.ReceiptStoreSettings
import com.example.receipt.data.model.ReceiptWithItems
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ReceiptUiEvent {
    data class ShowMessage(val text: String, val isError: Boolean = false) : ReceiptUiEvent()
    data class ReceiptGenerated(val receiptWithItems: ReceiptWithItems) : ReceiptUiEvent()
}

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ReceiptDatabase.getDatabase(application, viewModelScope)
    val repository = ReceiptRepository(database)

    private val prefs = application.getSharedPreferences("diprint_receipt_store_settings", android.content.Context.MODE_PRIVATE)

    private val _uiEvents = MutableSharedFlow<ReceiptUiEvent>()
    val uiEvents: SharedFlow<ReceiptUiEvent> = _uiEvents.asSharedFlow()

    // Store Settings persisted in SharedPreferences
    private val _storeSettings = MutableStateFlow(loadStoreSettings())
    val storeSettings: StateFlow<ReceiptStoreSettings> = _storeSettings.asStateFlow()

    private fun loadStoreSettings(): ReceiptStoreSettings {
        val def = ReceiptStoreSettings()
        return ReceiptStoreSettings(
            storeName = prefs.getString("store_name", def.storeName) ?: def.storeName,
            address = prefs.getString("store_address", def.address) ?: def.address,
            phone = prefs.getString("store_phone", def.phone) ?: def.phone,
            receiptPrefix = prefs.getString("receipt_prefix", def.receiptPrefix) ?: def.receiptPrefix,
            startNumber = prefs.getInt("start_number", def.startNumber),
            footerNote1 = prefs.getString("footer_note_1", def.footerNote1) ?: def.footerNote1,
            footerNote2 = prefs.getString("footer_note_2", def.footerNote2) ?: def.footerNote2
        )
    }

    // Products & Receipts from isolated database
    val allProducts: StateFlow<List<ReceiptProduct>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceipts: StateFlow<List<ReceiptWithItems>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart / Receipt Composer State
    private val _cartItems = MutableStateFlow<List<ReceiptCartItem>>(emptyList())
    val cartItems: StateFlow<List<ReceiptCartItem>> = _cartItems.asStateFlow()

    private val _receiptNumberInput = MutableStateFlow("")
    val receiptNumberInput: StateFlow<String> = _receiptNumberInput.asStateFlow()

    private val _paymentMethodInput = MutableStateFlow("Lunas")
    val paymentMethodInput: StateFlow<String> = _paymentMethodInput.asStateFlow()

    private val _selectedReceiptForPreview = MutableStateFlow<ReceiptWithItems?>(null)
    val selectedReceiptForPreview: StateFlow<ReceiptWithItems?> = _selectedReceiptForPreview.asStateFlow()

    init {
        refreshNextReceiptNumber()
    }

    fun refreshNextReceiptNumber() {
        viewModelScope.launch {
            val settings = _storeSettings.value
            val nextNum = repository.getNextReceiptNumber(settings.receiptPrefix, settings.startNumber)
            _receiptNumberInput.value = nextNum
        }
    }

    fun setReceiptNumber(number: String) {
        _receiptNumberInput.value = number
    }

    fun setPaymentMethod(method: String) {
        _paymentMethodInput.value = method
    }

    fun updateStoreSettings(settings: ReceiptStoreSettings) {
        _storeSettings.value = settings
        prefs.edit()
            .putString("store_name", settings.storeName)
            .putString("store_address", settings.address)
            .putString("store_phone", settings.phone)
            .putString("receipt_prefix", settings.receiptPrefix)
            .putInt("start_number", settings.startNumber)
            .putString("footer_note_1", settings.footerNote1)
            .putString("footer_note_2", settings.footerNote2)
            .apply()
        refreshNextReceiptNumber()
    }

    // --- Cart Actions ---

    fun addProductToCart(product: ReceiptProduct, quantity: Int = 1, customPrice: Double? = null) {
        val current = _cartItems.value.toMutableList()
        val effectivePrice = customPrice ?: product.unitPrice
        val existingIndex = current.indexOfFirst { it.sku == product.sku && it.customPrice == effectivePrice }

        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(
                ReceiptCartItem(
                    productId = product.id,
                    sku = product.sku,
                    productName = product.name,
                    originalPrice = product.unitPrice,
                    customPrice = effectivePrice,
                    quantity = quantity
                )
            )
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(index: Int, newQuantity: Int) {
        if (index in _cartItems.value.indices) {
            val current = _cartItems.value.toMutableList()
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQuantity)
            }
            _cartItems.value = current
        }
    }

    fun updateCartItemCustomPrice(index: Int, newCustomPrice: Double) {
        if (index in _cartItems.value.indices) {
            val current = _cartItems.value.toMutableList()
            current[index] = current[index].copy(customPrice = newCustomPrice.coerceAtLeast(0.0))
            _cartItems.value = current
        }
    }

    fun removeCartItem(index: Int) {
        if (index in _cartItems.value.indices) {
            val current = _cartItems.value.toMutableList()
            current.removeAt(index)
            _cartItems.value = current
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        refreshNextReceiptNumber()
    }

    // --- Generate Struk ---

    fun generateReceipt() {
        val number = _receiptNumberInput.value.trim()
        val items = _cartItems.value
        val settings = _storeSettings.value
        val paymentMethod = _paymentMethodInput.value

        if (number.isBlank()) {
            emitMessage("Nomor struk tidak boleh kosong", isError = true)
            return
        }

        if (items.isEmpty()) {
            emitMessage("Pilih minimal satu produk untuk dicetak pada struk", isError = true)
            return
        }

        viewModelScope.launch {
            val isExists = repository.isReceiptNumberExists(number)
            if (isExists) {
                emitMessage("Nomor struk '$number' sudah terdaftar! Gunakan nomor lain.", isError = true)
                return@launch
            }

            val result = repository.generateAndSaveReceipt(
                receiptNumber = number,
                storeSettings = settings,
                paymentMethod = paymentMethod,
                items = items
            )

            result.onSuccess { receiptId ->
                emitMessage("Struk $number berhasil di-generate dan disimpan ke riwayat!")
                // Clear cart & advance receipt number
                _cartItems.value = emptyList()
                refreshNextReceiptNumber()

                // Load preview
                database.receiptRecordDao().getReceiptWithItemsById(receiptId)?.let { created ->
                    _selectedReceiptForPreview.value = created
                    _uiEvents.emit(ReceiptUiEvent.ReceiptGenerated(created))
                }
            }.onFailure { err ->
                emitMessage("Gagal generate struk: ${err.message}", isError = true)
            }
        }
    }

    fun selectReceiptForPreview(receipt: ReceiptWithItems?) {
        _selectedReceiptForPreview.value = receipt
    }

    fun deleteReceipt(receiptWithItems: ReceiptWithItems) {
        viewModelScope.launch {
            repository.deleteReceipt(receiptWithItems.receipt.id).onSuccess {
                emitMessage("Struk #${receiptWithItems.receipt.receiptNumber} dihapus dari riwayat")
                if (_selectedReceiptForPreview.value?.receipt?.id == receiptWithItems.receipt.id) {
                    _selectedReceiptForPreview.value = null
                }
            }
        }
    }

    // --- Product CRUD ---

    fun addNewProduct(sku: String, name: String, price: Double, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.addProduct(sku, name, price)
                .onSuccess {
                    emitMessage("Produk $sku berhasil ditambahkan")
                    onDone(true)
                }
                .onFailure { err ->
                    emitMessage(err.message ?: "Gagal menambah produk", isError = true)
                    onDone(false)
                }
        }
    }

    fun updateProduct(product: ReceiptProduct, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.updateProduct(product.id, product.sku, product.name, product.unitPrice)
                .onSuccess {
                    emitMessage("Produk ${product.sku} berhasil diperbarui")
                    onDone(true)
                }
                .onFailure { err ->
                    emitMessage(err.message ?: "Gagal memperbarui produk", isError = true)
                    onDone(false)
                }
        }
    }

    fun deleteProduct(product: ReceiptProduct) {
        viewModelScope.launch {
            repository.deleteProduct(product)
                .onSuccess { emitMessage("Produk ${product.sku} dihapus") }
                .onFailure { err -> emitMessage("Gagal menghapus produk: ${err.message}", isError = true) }
        }
    }

    fun importCsv(csvContent: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.importProductsFromCsv(csvContent)
                .onSuccess { count ->
                    emitMessage("Berhasil mengimpor $count produk dari file CSV")
                    onDone()
                }
                .onFailure { err ->
                    emitMessage("Gagal impor CSV: ${err.message}", isError = true)
                }
        }
    }

    suspend fun exportCsv(): String = repository.exportProductsToCsv()

    fun getTemplateCsv(): String = repository.getSampleCsvTemplate()

    fun getWhatsAppShareText(receiptWithItems: ReceiptWithItems): String {
        return repository.formatReceiptForWhatsApp(receiptWithItems.receipt, receiptWithItems.items)
    }

    suspend fun exportAllReceiptsJson(): String = repository.exportAllReceiptsJson()

    suspend fun importReceiptsFromJson(jsonText: String): Result<Int> {
        val res = repository.importReceiptsFromJson(jsonText)
        res.onSuccess { count ->
            emitMessage("Berhasil mengimpor $count struk tersimpan")
        }.onFailure { err ->
            emitMessage("Gagal mengimpor data struk: ${err.message}", isError = true)
        }
        return res
    }

    private fun emitMessage(text: String, isError: Boolean = false) {
        viewModelScope.launch {
            _uiEvents.emit(ReceiptUiEvent.ShowMessage(text, isError))
        }
    }
}
