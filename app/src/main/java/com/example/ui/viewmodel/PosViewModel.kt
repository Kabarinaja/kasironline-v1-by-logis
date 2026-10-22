package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.backup.BackupResult
import com.example.data.local.entity.*
import com.example.data.preferences.BusinessSettings
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PosRepository
import com.example.data.repository.SalesSummary
import com.example.data.repository.TransactionDetail
import com.example.pdf.PdfReportGenerator
import com.example.printer.BluetoothPrinterManager
import com.example.printer.EscPosBuilder
import com.example.printer.PrintResult
import com.example.printer.PrinterDeviceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val discount: Double = 0.0
) {
    val subtotal: Double
        get() = (product.sellingPrice * quantity - discount).coerceAtLeast(0.0)
}

data class PosUiState(
    val isPinSet: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isOnboarded: Boolean = false,
    val lockoutSeconds: Int = 0,
    val settings: BusinessSettings = BusinessSettings(),
    val language: String = "id",
    val activeSession: CashierSession? = null,
    val activeCrew: List<Crew> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val activeProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val lowStockProducts: List<Product> = emptyList(),
    val activeProductCount: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val transactionDiscount: Double = 0.0,
    val transactionDiscountNote: String = "",
    val selectedCashier: Crew? = null,
    val lastCompletedTransaction: TransactionDetail? = null,
    val salesReport: SalesSummary? = null,
    val reportTransactions: List<TransactionDetail> = emptyList(),
    val selectedReportPeriod: String = "today",
    val pairedPrinters: List<PrinterDeviceInfo> = emptyList(),
    val isPrinting: Boolean = false,
    val printMessage: String? = null,
    val toastMessage: String? = null
)

class PosViewModel(
    private val repository: PosRepository,
    private val preferences: UserPreferences,
    private val printerManager: BluetoothPrinterManager,
    private val pdfReportGenerator: PdfReportGenerator,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private var lockoutJob: Job? = null

    init {
        observeInitialState()
    }

    private fun observeInitialState() {
        viewModelScope.launch {
            preferences.isPinSetFlow.collect { isSet ->
                _uiState.update { it.copy(isPinSet = isSet) }
            }
        }
        viewModelScope.launch {
            preferences.isOnboardedFlow.collect { onboarded ->
                _uiState.update { it.copy(isOnboarded = onboarded) }
            }
        }
        viewModelScope.launch {
            preferences.languageFlow.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
        viewModelScope.launch {
            preferences.businessSettingsFlow.collect { s ->
                _uiState.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            repository.activeSession.collect { session ->
                _uiState.update { it.copy(activeSession = session) }
            }
        }
        viewModelScope.launch {
            repository.activeCrew.collect { crews ->
                _uiState.update {
                    it.copy(
                        activeCrew = crews,
                        selectedCashier = it.selectedCashier ?: crews.firstOrNull()
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.allProducts.collect { products ->
                _uiState.update { it.copy(allProducts = products) }
            }
        }
        viewModelScope.launch {
            repository.activeProducts.collect { products ->
                _uiState.update { it.copy(activeProducts = products) }
            }
        }
        viewModelScope.launch {
            repository.lowStockProducts.collect { low ->
                _uiState.update { it.copy(lowStockProducts = low) }
            }
        }
        viewModelScope.launch {
            repository.activeProductCount.collect { count ->
                _uiState.update { it.copy(activeProductCount = count) }
            }
        }
        viewModelScope.launch {
            repository.allCategories.collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            repository.recentTransactions.collect { txs ->
                _uiState.update { it.copy(recentTransactions = txs) }
            }
        }

        refreshSalesReport("today")
        refreshBluetoothPrinters()
    }

    // --- PIN AUTHENTICATION ---
    fun submitPin(pin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val lockout = preferences.getLockoutSeconds()
            if (lockout > 0) {
                startLockoutCountdown(lockout)
                onResult(false, "Terlalu banyak percobaan. Tunggu $lockout detik.")
                return@launch
            }

            val isValid = preferences.verifyPin(pin)
            if (isValid) {
                _uiState.update { it.copy(isAuthenticated = true, lockoutSeconds = 0) }
                onResult(true, null)
            } else {
                val newLockout = preferences.getLockoutSeconds()
                if (newLockout > 0) {
                    startLockoutCountdown(newLockout)
                    onResult(false, "Terlalu banyak percobaan salah. Tunggu $newLockout detik.")
                } else {
                    onResult(false, "PIN salah! Periksa kembali 6 digit PIN Anda.")
                }
            }
        }
    }

    fun setupInitialPin(pin: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            preferences.savePin(pin)
            _uiState.update { it.copy(isPinSet = true, isAuthenticated = true) }
            onComplete()
        }
    }

    fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val isOldValid = preferences.verifyPin(oldPin)
            if (!isOldValid) {
                onResult(false, "PIN lama salah!")
                return@launch
            }
            preferences.savePin(newPin)
            onResult(true, "PIN berhasil diubah!")
        }
    }

    fun lockTerminal() {
        _uiState.update { it.copy(isAuthenticated = false) }
    }

    private fun startLockoutCountdown(seconds: Int) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var s = seconds
            while (s > 0) {
                _uiState.update { it.copy(lockoutSeconds = s) }
                delay(1000)
                s--
            }
            _uiState.update { it.copy(lockoutSeconds = 0) }
        }
    }

    // --- ONBOARDING & SETTINGS ---
    fun completeOnboarding(settings: BusinessSettings) {
        viewModelScope.launch {
            preferences.updateBusinessSettings(settings)
            preferences.setOnboarded(true)
            _uiState.update { it.copy(settings = settings, isOnboarded = true) }
        }
    }

    fun updateSettings(settings: BusinessSettings) {
        viewModelScope.launch {
            preferences.updateBusinessSettings(settings)
            _uiState.update { it.copy(settings = settings) }
            showToast("Pengaturan berhasil disimpan.")
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferences.setLanguage(lang)
            _uiState.update { it.copy(language = lang) }
        }
    }

    // --- CART & TRANSACTIONS ---
    fun selectCashier(crew: Crew) {
        _uiState.update { it.copy(selectedCashier = crew) }
    }

    fun addToCart(product: Product) {
        _uiState.update { current ->
            val existing = current.cart.find { it.product.id == product.id }
            val newCart = if (existing != null) {
                current.cart.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                current.cart + CartItem(product = product, quantity = 1)
            }
            current.copy(cart = newCart)
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        _uiState.update { current ->
            val newCart = current.cart.mapNotNull { item ->
                if (item.product.id == productId) {
                    val newQty = item.quantity + delta
                    if (newQty > 0) item.copy(quantity = newQty) else null
                } else {
                    item
                }
            }
            current.copy(cart = newCart)
        }
    }

    fun removeFromCart(productId: Long) {
        _uiState.update { current ->
            current.copy(cart = current.cart.filter { it.product.id != productId })
        }
    }

    fun setItemDiscount(productId: Long, discount: Double) {
        _uiState.update { current ->
            val newCart = current.cart.map {
                if (it.product.id == productId) it.copy(discount = discount.coerceAtLeast(0.0)) else it
            }
            current.copy(cart = newCart)
        }
    }

    fun setTransactionDiscount(amount: Double, note: String = "") {
        _uiState.update { it.copy(transactionDiscount = amount, transactionDiscountNote = note) }
    }

    fun clearCart() {
        _uiState.update { it.copy(cart = emptyList(), transactionDiscount = 0.0, transactionDiscountNote = "") }
    }

    fun scanBarcodeOrSku(code: String, onFound: (Product?) -> Unit) {
        viewModelScope.launch {
            val product = repository.findProductByBarcodeOrSku(code)
            if (product != null) {
                addToCart(product)
            }
            onFound(product)
        }
    }

    fun processPayment(
        paymentMethod: String, // TUNAI, QRIS
        cashPaid: Double,
        qrisRef: String,
        notes: String,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value
        if (state.cart.isEmpty()) {
            onError("Keranjang belanja kosong.")
            return
        }

        val cashier = state.selectedCashier ?: state.activeCrew.firstOrNull()
        val cashierName = cashier?.name ?: state.settings.ownerName
        val cashierRole = cashier?.role ?: "Kasir"

        val subtotal = state.cart.sumOf { it.subtotal }
        val discount = state.transactionDiscount
        val afterDiscount = (subtotal - discount).coerceAtLeast(0.0)
        val taxRate = if (state.settings.isTaxEnabled) state.settings.taxPercentage else 0.0
        val taxAmount = afterDiscount * (taxRate / 100.0)
        val totalAmount = afterDiscount + taxAmount

        if (paymentMethod == "TUNAI" && cashPaid < totalAmount) {
            onError("Uang yang diterima kurang dari total belanja!")
            return
        }

        val changeReturned = if (paymentMethod == "TUNAI") (cashPaid - totalAmount).coerceAtLeast(0.0) else 0.0

        val txItems = state.cart.map { cartItem ->
            TransactionItemEntity(
                transactionId = 0L,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                sku = cartItem.product.sku,
                sellingPrice = cartItem.product.sellingPrice,
                costPrice = cartItem.product.costPrice,
                quantity = cartItem.quantity,
                discountPerItem = cartItem.discount,
                subtotal = cartItem.subtotal
            )
        }

        viewModelScope.launch {
            try {
                val txId = repository.completeTransaction(
                    items = txItems,
                    subtotal = subtotal,
                    discountAmount = discount,
                    discountNote = state.transactionDiscountNote,
                    taxRate = taxRate,
                    taxAmount = taxAmount,
                    totalAmount = totalAmount,
                    paymentMethod = paymentMethod,
                    cashPaid = cashPaid,
                    changeReturned = changeReturned,
                    qrisReference = qrisRef,
                    notes = notes,
                    cashierName = cashierName,
                    cashierRole = cashierRole
                )

                val detail = repository.getTransactionDetail(txId)
                _uiState.update {
                    it.copy(
                        lastCompletedTransaction = detail,
                        cart = emptyList(),
                        transactionDiscount = 0.0,
                        transactionDiscountNote = ""
                    )
                }
                refreshSalesReport(state.selectedReportPeriod)
                onSuccess(txId)
            } catch (e: Exception) {
                onError("Gagal memproses transaksi: ${e.message}")
            }
        }
    }

    fun cancelTransaction(txId: Long, reason: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.cancelTransaction(txId, reason)
            refreshSalesReport(_uiState.value.selectedReportPeriod)
            showToast("Transaksi berhasil dibatalkan dan stok dikembalikan.")
            onDone()
        }
    }

    // --- CASHIER SESSIONS ---
    fun openSession(crew: Crew, openingCash: Double, notes: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.openSession(crew, openingCash, notes)
            showToast("Sesi kasir berhasil dibuka.")
            onComplete()
        }
    }

    fun closeSession(sessionId: Long, actualCash: Double, closingNotes: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.closeSession(sessionId, actualCash, closingNotes)
            showToast("Sesi kasir berhasil ditutup.")
            onComplete()
        }
    }

    fun recordCashMovement(type: String, amount: Double, reason: String, onComplete: () -> Unit) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            repository.recordCashMovement(session.id, type, amount, reason)
            showToast(if (type == "CASH_IN") "Kas masuk dicatat." else "Kas keluar dicatat.")
            onComplete()
        }
    }

    // --- PRODUCTS & CATEGORIES ---
    fun saveProduct(product: Product, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showToast("Produk '${product.name}' berhasil disimpan.")
            onComplete()
        }
    }

    fun deleteProduct(id: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.softDeleteProduct(id)
            showToast("Produk berhasil dihapus.")
            onComplete()
        }
    }

    fun adjustProductStock(productId: Long, qtyChange: Int, reason: String, type: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.adjustStock(productId, qtyChange, reason, type)
            showToast("Stok produk berhasil disesuaikan.")
            onComplete()
        }
    }

    fun saveCategory(category: Category, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveCategory(category)
            showToast("Kategori disimpan.")
            onComplete()
        }
    }

    fun saveCrew(crew: Crew, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.saveCrew(crew)
            showToast("Data crew berhasil disimpan.")
            onComplete()
        }
    }

    fun deleteCrew(id: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.softDeleteCrew(id)
            showToast("Crew dinonaktifkan.")
            onComplete()
        }
    }

    // --- REPORTS ---
    fun refreshSalesReport(period: String) {
        _uiState.update { it.copy(selectedReportPeriod = period) }
        viewModelScope.launch {
            val (start, end) = calculateDateRange(period)
            val summary = repository.getSalesSummary(start, end)
            _uiState.update { it.copy(salesReport = summary) }
        }
    }

    private fun calculateDateRange(period: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val start = when (period) {
            "yesterday" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.timeInMillis
            }
            "7days" -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.timeInMillis
            }
            "month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            else -> cal.timeInMillis // "today"
        }

        val actualEnd = if (period == "yesterday") {
            val yCal = Calendar.getInstance()
            yCal.add(Calendar.DAY_OF_YEAR, -1)
            yCal.set(Calendar.HOUR_OF_DAY, 23)
            yCal.set(Calendar.MINUTE, 59)
            yCal.set(Calendar.SECOND, 59)
            yCal.timeInMillis
        } else end

        return Pair(start, actualEnd)
    }

    fun exportPdfReport(onComplete: (File?) -> Unit) {
        val state = _uiState.value
        val summary = state.salesReport ?: return
        viewModelScope.launch {
            try {
                val periodLabel = when (state.selectedReportPeriod) {
                    "yesterday" -> "Kemarin"
                    "7days" -> "7 Hari Terakhir"
                    "month" -> "Bulan Ini"
                    else -> "Hari Ini"
                }
                val (start, end) = calculateDateRange(state.selectedReportPeriod)
                val txs = repository.allTransactions.first().filter { it.timestamp in start..end }
                val txDetails = txs.mapNotNull { repository.getTransactionDetail(it.id) }
                val pdfFile = pdfReportGenerator.generateSalesReportPdf(
                    settings = state.settings,
                    periodLabel = periodLabel,
                    summary = summary,
                    transactions = txDetails
                )
                onComplete(pdfFile)
            } catch (e: Exception) {
                showToast("Gagal membuat PDF: ${e.message}")
                onComplete(null)
            }
        }
    }

    fun sharePdf(file: File) {
        pdfReportGenerator.openOrSharePdf(file)
    }

    // --- PRINTER & PRINTING ---
    fun refreshBluetoothPrinters() {
        val devices = printerManager.getPairedDevices()
        _uiState.update { it.copy(pairedPrinters = devices) }
    }

    fun printCurrentReceipt(detail: TransactionDetail, onResult: (Boolean, String) -> Unit) {
        val state = _uiState.value
        val printerAddress = state.settings.preferredPrinterAddress
        if (printerAddress.isEmpty()) {
            onResult(false, "Printer belum dipilih di menu Pengaturan Printer.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true) }
            val builder = buildReceiptEscPos(detail, state.settings)
            val result = printerManager.printReceipt(printerAddress, builder)
            _uiState.update { it.copy(isPrinting = false) }

            when (result) {
                is PrintResult.Success -> onResult(true, "Struk berhasil dicetak.")
                is PrintResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun testPrint(macAddress: String, onResult: (Boolean, String) -> Unit) {
        val settings = _uiState.value.settings
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true) }
            val result = printerManager.testPrint(macAddress, settings)
            _uiState.update { it.copy(isPrinting = false) }

            when (result) {
                is PrintResult.Success -> onResult(true, "Uji cetak berhasil terkirim.")
                is PrintResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun buildReceiptEscPos(detail: TransactionDetail, settings: BusinessSettings): EscPosBuilder {
        val tx = detail.transaction
        val items = detail.items
        val builder = EscPosBuilder(settings.paperSize).init()

        // Header
        builder.alignCenter()
        if (settings.showHeaderInfo) {
            builder.doubleSize(true)
                .textLine(settings.businessName)
                .doubleSize(false)
            if (settings.address.isNotEmpty()) builder.textLine(settings.address)
            if (settings.phone.isNotEmpty()) builder.textLine("Telp: ${settings.phone}")
            builder.divider('-')
        }

        // Meta info
        builder.alignLeft()
            .doubleRow("No:", tx.invoiceNumber)
            .doubleRow(
                "Waktu:",
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
            )
        if (settings.showCashierInfo) {
            builder.doubleRow("Kasir:", "${tx.cashierName} (${tx.cashierRole})")
        }
        builder.divider('-')

        // Itemized list
        for (item in items) {
            val qtyPrice = "${item.quantity}x @${formatRupiah(item.sellingPrice)}"
            builder.itemRow(item.productName, qtyPrice, formatRupiah(item.subtotal))
            if (item.discountPerItem > 0) {
                builder.doubleRow("  (Diskon Item)", "-${formatRupiah(item.discountPerItem)}")
            }
        }
        builder.divider('-')

        // Totals
        builder.doubleRow("Subtotal:", formatRupiah(tx.subtotal))
        if (tx.discountAmount > 0) {
            builder.doubleRow("Diskon Transaksi:", "-${formatRupiah(tx.discountAmount)}")
        }
        if (tx.taxAmount > 0) {
            builder.doubleRow("Pajak (${tx.taxRate}%):", formatRupiah(tx.taxAmount))
        }
        builder.bold(true)
            .doubleSize(true)
            .doubleRow("TOTAL:", formatRupiah(tx.totalAmount))
            .bold(false)
            .doubleSize(false)
        builder.divider('-')

        // Payment info
        builder.doubleRow("Metode:", tx.paymentMethod)
        if (tx.paymentMethod == "TUNAI") {
            builder.doubleRow("Tunai Diterima:", formatRupiah(tx.cashPaid))
            builder.doubleRow("Kembalian:", formatRupiah(tx.changeReturned))
        } else {
            if (tx.qrisReference.isNotEmpty()) {
                builder.doubleRow("Ref QRIS:", tx.qrisReference)
            }
            builder.doubleRow("Status:", "LUNAS (VERIFIED)")
        }

        // Promo QR or Footer
        if (settings.showPromoQr && settings.promoQrText.isNotEmpty()) {
            builder.divider('-')
                .alignCenter()
                .textLine("Scan Disini:")
                .qrCode(settings.promoQrText)
        }

        if (settings.receiptFooter.isNotEmpty()) {
            builder.divider('-')
                .alignCenter()
                .textLine(settings.receiptFooter)
        }

        builder.lineFeed(2).cutPaper()
        return builder
    }

    private fun formatRupiah(amount: Double): String {
        return "Rp " + java.text.NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount.toLong())
    }

    // --- BACKUP & RESTORE ---
    fun backupData(onComplete: (BackupResult) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.createFullBackup()
            onComplete(result)
        }
    }

    fun restoreData(json: String, onComplete: (BackupResult) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreFromBackupJson(json)
            refreshSalesReport("today")
            onComplete(result)
        }
    }

    fun exportCsv(onComplete: (File) -> Unit) {
        viewModelScope.launch {
            val file = backupManager.exportProductsToCsv()
            onComplete(file)
        }
    }

    fun importCsv(csvContent: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = backupManager.importProductsFromCsv(csvContent)
            showToast("Berhasil mengimpor $count produk.")
            onComplete(count)
        }
    }

    fun shareBackupFile(file: File, mimeType: String, title: String) {
        backupManager.shareFile(file, mimeType, title)
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}

class PosViewModelFactory(
    private val repository: PosRepository,
    private val preferences: UserPreferences,
    private val printerManager: BluetoothPrinterManager,
    private val pdfReportGenerator: PdfReportGenerator,
    private val backupManager: BackupManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PosViewModel(
            repository,
            preferences,
            printerManager,
            pdfReportGenerator,
            backupManager
        ) as T
    }
}
