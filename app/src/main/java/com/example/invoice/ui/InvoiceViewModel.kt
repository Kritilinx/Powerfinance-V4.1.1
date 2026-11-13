package com.example.invoice.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.invoice.data.InvoiceDatabase
import com.example.invoice.data.InvoiceRepository
import com.example.invoice.data.model.InvoiceDraftItem
import com.example.invoice.data.model.InvoiceWithItems
import com.example.invoice.util.InvoicePdfGenerator
import com.example.receipt.data.model.ReceiptProduct
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

sealed class InvoiceUiMessage {
    data class Success(val message: String) : InvoiceUiMessage()
    data class Error(val message: String) : InvoiceUiMessage()
    data class Info(val message: String) : InvoiceUiMessage()
}

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val invoiceDb = InvoiceDatabase.getDatabase(application)
    private val repository = InvoiceRepository(
        context = application,
        invoiceDao = invoiceDb.invoiceDao(),
        coroutineScope = viewModelScope
    )

    // UI Feedback
    private val _uiMessage = MutableSharedFlow<InvoiceUiMessage>()
    val uiMessage: SharedFlow<InvoiceUiMessage> = _uiMessage.asSharedFlow()

    // Navigation Tab: 0 = Form, 1 = History
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Form State
    private val _invoiceNumber = MutableStateFlow("")
    val invoiceNumber: StateFlow<String> = _invoiceNumber.asStateFlow()

    private val _isNumberDuplicate = MutableStateFlow(false)
    val isNumberDuplicate: StateFlow<Boolean> = _isNumberDuplicate.asStateFlow()

    private val _dateFormatted = MutableStateFlow(getTodayFormatted())
    val dateFormatted: StateFlow<String> = _dateFormatted.asStateFlow()

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _customerAddress = MutableStateFlow("")
    val customerAddress: StateFlow<String> = _customerAddress.asStateFlow()

    private val _items = MutableStateFlow<List<InvoiceDraftItem>>(emptyList())
    val items: StateFlow<List<InvoiceDraftItem>> = _items.asStateFlow()

    private val _discountText = MutableStateFlow("0")
    val discountText: StateFlow<String> = _discountText.asStateFlow()

    private val _taxText = MutableStateFlow("0")
    val taxText: StateFlow<String> = _taxText.asStateFlow()

    private val _shippingText = MutableStateFlow("0")
    val shippingText: StateFlow<String> = _shippingText.asStateFlow()

    private val _otherText = MutableStateFlow("0")
    val otherText: StateFlow<String> = _otherText.asStateFlow()

    companion object {
        const val DEFAULT_INVOICE_COMMENTS = "1. Proses pengerjaan akan dilakukan setelah pembayaran uang muka minimal 60% dari total harga yang tercantum.\n2. Pembayaran dapat dilakukan ke :\nBCA Cab. Taman Palem Lestari : 7570689503 A/n Richi Wibowo\n3. Batas pembayaran 7 hari setelah tanggal invoice"
    }

    private val _comments = MutableStateFlow(repository.getLastSavedComments(DEFAULT_INVOICE_COMMENTS))
    val comments: StateFlow<String> = _comments.asStateFlow()

    // Calculations
    val subtotal: StateFlow<Double> = _items.combine(MutableStateFlow(0)) { list, _ ->
        list.sumOf { it.quantity * it.unitPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalAmount: StateFlow<Double> = combine(
        subtotal,
        _discountText,
        _taxText,
        _shippingText,
        _otherText
    ) { sub, disc, tx, sh, oth ->
        val d = disc.toDoubleOrNull() ?: 0.0
        val t = tx.toDoubleOrNull() ?: 0.0
        val s = sh.toDoubleOrNull() ?: 0.0
        val o = oth.toDoubleOrNull() ?: 0.0
        maxOf(0.0, sub - d + t + s + o)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Catalog Products (strictly read-only from Receipt database)
    val catalogProducts: StateFlow<List<ReceiptProduct>> = repository.getAvailableCatalogProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History and Search
    val allInvoices: StateFlow<List<InvoiceWithItems>> = repository.getAllInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredInvoices: StateFlow<List<InvoiceWithItems>> = combine(allInvoices, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter {
                it.invoice.invoiceNumber.lowercase().contains(q) ||
                it.invoice.customerName.lowercase().contains(q) ||
                it.invoice.customerPhone.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Preview Modal
    private val _previewInvoice = MutableStateFlow<InvoiceWithItems?>(null)
    val previewInvoice: StateFlow<InvoiceWithItems?> = _previewInvoice.asStateFlow()

    init {
        loadNextInvoiceNumber()
        addInitialItem()
    }

    private fun addInitialItem() {
        if (_items.value.isEmpty()) {
            _items.value = listOf(
                InvoiceDraftItem(
                    itemNumber = 1,
                    sku = "ITEM-1",
                    description = "Cetak Banner Outdoor 280gsm High-Res",
                    quantity = 2,
                    unitPrice = 25000.0,
                    tax = 0.0
                )
            )
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadNextInvoiceNumber() {
        viewModelScope.launch {
            val next = repository.getNextInvoiceNumber()
            _invoiceNumber.value = next
            checkDuplicate(next)
        }
    }

    fun updateInvoiceNumber(number: String) {
        _invoiceNumber.value = number
        checkDuplicate(number)
    }

    private fun checkDuplicate(number: String) {
        viewModelScope.launch {
            _isNumberDuplicate.value = repository.isInvoiceNumberExists(number)
        }
    }

    fun updateDate(date: String) {
        _dateFormatted.value = date
    }

    fun updateCustomerName(name: String) {
        _customerName.value = name
    }

    fun updateCustomerPhone(phone: String) {
        _customerPhone.value = phone
    }

    fun updateCustomerAddress(address: String) {
        _customerAddress.value = address
    }

    fun updateDiscount(value: String) {
        _discountText.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateTax(value: String) {
        _taxText.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateShipping(value: String) {
        _shippingText.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateOther(value: String) {
        _otherText.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateComments(value: String) {
        _comments.value = value
        repository.saveLastComments(value)
    }

    // --- Product Selection & Line Items Management ---

    fun addProductFromCatalog(product: ReceiptProduct) {
        val currentList = _items.value.toMutableList()
        val nextItemNumber = currentList.size + 1
        val newItem = InvoiceDraftItem(
            itemNumber = nextItemNumber,
            sku = product.sku,
            description = product.name,
            quantity = 1,
            unitPrice = product.unitPrice,
            tax = 0.0
        )
        currentList.add(newItem)
        _items.value = currentList
    }

    fun addBlankItem() {
        val currentList = _items.value.toMutableList()
        val nextItemNumber = currentList.size + 1
        val newItem = InvoiceDraftItem(
            itemNumber = nextItemNumber,
            sku = "ITEM-$nextItemNumber",
            description = "",
            quantity = 1,
            unitPrice = 0.0,
            tax = 0.0
        )
        currentList.add(newItem)
        _items.value = currentList
    }

    fun updateItem(index: Int, updatedItem: InvoiceDraftItem) {
        val currentList = _items.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = updatedItem
            _items.value = currentList
        }
    }

    fun removeItem(index: Int) {
        val currentList = _items.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            // Re-index item numbers
            val reindexed = currentList.mapIndexed { idx, itm ->
                itm.copy(itemNumber = idx + 1)
            }
            _items.value = reindexed
        }
    }

    // --- Generate & Save Invoice ---

    fun generateAndSaveInvoice(context: Context, onSuccess: (InvoiceWithItems) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val number = _invoiceNumber.value.trim()
                if (number.isBlank()) {
                    _uiMessage.emit(InvoiceUiMessage.Error("Nomor invoice tidak boleh kosong."))
                    return@launch
                }
                if (repository.isInvoiceNumberExists(number)) {
                    _uiMessage.emit(InvoiceUiMessage.Error("Nomor invoice '$number' sudah terdaftar! Harap ubah nomor invoice."))
                    return@launch
                }
                if (_items.value.isEmpty()) {
                    _uiMessage.emit(InvoiceUiMessage.Error("Tambahkan minimal 1 baris item produk pada invoice."))
                    return@launch
                }

                val discount = _discountText.value.toDoubleOrNull() ?: 0.0
                val tax = _taxText.value.toDoubleOrNull() ?: 0.0
                val shipping = _shippingText.value.toDoubleOrNull() ?: 0.0
                val other = _otherText.value.toDoubleOrNull() ?: 0.0

                // 1. Save to isolated invoice database
                val savedInvoiceWithItems = repository.saveInvoice(
                    invoiceNumber = number,
                    dateFormatted = _dateFormatted.value,
                    customerName = _customerName.value,
                    customerPhone = _customerPhone.value,
                    customerAddress = _customerAddress.value,
                    draftItems = _items.value,
                    discount = discount,
                    tax = tax,
                    shippingAndHandling = shipping,
                    other = other,
                    comments = _comments.value
                )

                // 2. Generate actual A4 PDF
                val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, savedInvoiceWithItems)

                _previewInvoice.value = savedInvoiceWithItems
                _uiMessage.emit(InvoiceUiMessage.Success("Invoice $number berhasil dibuat & disimpan!"))

                // Prepare next sequence for subsequent invoice
                loadNextInvoiceNumber()

                onSuccess(savedInvoiceWithItems)
            } catch (e: Exception) {
                _uiMessage.emit(InvoiceUiMessage.Error(e.message ?: "Gagal membuat invoice"))
            }
        }
    }

    fun openPreview(invoiceWithItems: InvoiceWithItems) {
        _previewInvoice.value = invoiceWithItems
    }

    fun closePreview() {
        _previewInvoice.value = null
    }

    fun downloadInvoicePdf(context: Context, invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            try {
                val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, invoiceWithItems)
                val success = InvoicePdfGenerator.downloadPdfToStorage(context, pdfFile, invoiceWithItems.invoice.invoiceNumber)
                if (success) {
                    _uiMessage.emit(InvoiceUiMessage.Success("PDF Invoice berhasil diunduh ke folder Download/Diprint_Invoices!"))
                } else {
                    _uiMessage.emit(InvoiceUiMessage.Error("Gagal menyimpan PDF ke folder Download."))
                }
            } catch (e: Exception) {
                _uiMessage.emit(InvoiceUiMessage.Error("Kesalahan saat mengunduh PDF: ${e.message}"))
            }
        }
    }

    fun shareInvoice(context: Context, invoiceWithItems: InvoiceWithItems, targetApp: String? = null) {
        viewModelScope.launch {
            try {
                val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, invoiceWithItems)
                InvoicePdfGenerator.shareInvoicePdf(
                    context = context,
                    pdfFile = pdfFile,
                    invoiceNumber = invoiceWithItems.invoice.invoiceNumber,
                    targetPackage = targetApp
                )
            } catch (e: Exception) {
                _uiMessage.emit(InvoiceUiMessage.Error("Gagal membagikan invoice: ${e.message}"))
            }
        }
    }

    fun resetForm() {
        loadNextInvoiceNumber()
        _dateFormatted.value = getTodayFormatted()
        _customerName.value = ""
        _customerPhone.value = ""
        _customerAddress.value = ""
        _discountText.value = "0"
        _taxText.value = "0"
        _shippingText.value = "0"
        _otherText.value = "0"
        _comments.value = repository.getLastSavedComments(DEFAULT_INVOICE_COMMENTS)
        _items.value = emptyList()
        addInitialItem()
    }

    private fun getTodayFormatted(): String {
        val cal = Calendar.getInstance()
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH) + 1
        val y = cal.get(Calendar.YEAR)
        return "$d/$m/$y"
    }

    fun deleteInvoice(invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch {
            try {
                repository.deleteInvoice(invoiceWithItems.invoice)
                _uiMessage.emit(InvoiceUiMessage.Success("Invoice ${invoiceWithItems.invoice.invoiceNumber} berhasil dihapus."))
            } catch (e: Exception) {
                _uiMessage.emit(InvoiceUiMessage.Error("Gagal menghapus invoice: ${e.message}"))
            }
        }
    }

    suspend fun exportAllInvoicesJson(): String {
        return repository.exportAllInvoicesJson()
    }

    suspend fun importInvoicesFromJson(jsonText: String): Result<Int> {
        val result = repository.importInvoicesFromJson(jsonText)
        if (result.isSuccess) {
            _uiMessage.emit(InvoiceUiMessage.Success("Berhasil mengimpor ${result.getOrNull()} data invoice!"))
        } else {
            _uiMessage.emit(InvoiceUiMessage.Error("Gagal impor: ${result.exceptionOrNull()?.message}"))
        }
        return result
    }
}
