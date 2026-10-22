package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Crew
import com.example.data.local.entity.Product
import com.example.localization.LocalizedStrings
import com.example.ui.components.Formatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onShowReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showMobileCartSheet by remember { mutableStateOf(false) }

    // Filtered products
    val filteredProducts = remember(searchQuery, selectedCategory, uiState.activeProducts) {
        uiState.activeProducts.filter { product ->
            val matchSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.sku.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "Semua" || product.category.equals(selectedCategory, ignoreCase = true)
            matchSearch && matchCat
        }
    }

    val cartTotal = remember(uiState.cart, uiState.transactionDiscount, uiState.settings.isTaxEnabled) {
        val subtotal = uiState.cart.sumOf { it.subtotal }
        val afterDiscount = (subtotal - uiState.transactionDiscount).coerceAtLeast(0.0)
        val tax = if (uiState.settings.isTaxEnabled) afterDiscount * (uiState.settings.taxPercentage / 100.0) else 0.0
        afterDiscount + tax
    }

    val cartItemCount = remember(uiState.cart) {
        uiState.cart.sumOf { it.quantity }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.pos,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        // Cashier selector dropdown button
                        CashierSelectorDropdown(
                            crews = uiState.activeCrew,
                            selectedCashier = uiState.selectedCashier,
                            onSelect = { viewModel.selectCashier(it) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        },
        bottomBar = {
            // Mobile cart bar if not tablet
            if (!isTabletOrLandscape && uiState.cart.isNotEmpty()) {
                Surface(
                    color = BrandToskaDark,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .clickable { showMobileCartSheet = true }
                        .testTag("pos_mobile_cart_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(
                                containerColor = BrandOrange,
                                contentColor = Color.White
                            ) {
                                Text("$cartItemCount", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Total Belanja", fontSize = 11.sp, color = BrandToskaLight)
                                Text(
                                    Formatters.formatRupiah(cartTotal),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { showCheckoutDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pos_mobile_checkout_btn")
                        ) {
                            Text("Bayar", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isTabletOrLandscape) {
            // Tablet Side-by-Side Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Left 60%: Product Catalog
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductSearchAndCategories(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onBarcodeScan = { code ->
                            viewModel.scanBarcodeOrSku(code) {}
                        },
                        categories = uiState.categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )

                    ProductGrid(
                        products = filteredProducts,
                        onProductClick = { viewModel.addToCart(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                VerticalDivider(color = BrandGrayBorder)

                // Right 40%: Order Cart & Quick Payment
                Card(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    CartPane(
                        cart = uiState.cart,
                        discount = uiState.transactionDiscount,
                        discountNote = uiState.transactionDiscountNote,
                        total = cartTotal,
                        taxEnabled = uiState.settings.isTaxEnabled,
                        taxRate = uiState.settings.taxPercentage,
                        onQuantityChange = { id, delta -> viewModel.updateCartQuantity(id, delta) },
                        onRemove = { viewModel.removeFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onOpenDiscountDialog = { showDiscountDialog = true },
                        onCheckoutClick = { showCheckoutDialog = true }
                    )
                }
            }
        } else {
            // Handheld Vertical Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductSearchAndCategories(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onBarcodeScan = { code ->
                        viewModel.scanBarcodeOrSku(code) {}
                    },
                    categories = uiState.categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it }
                )

                ProductGrid(
                    products = filteredProducts,
                    onProductClick = { viewModel.addToCart(it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Mobile Bottom Sheet for Cart
    if (showMobileCartSheet && !isTabletOrLandscape) {
        ModalBottomSheet(
            onDismissRequest = { showMobileCartSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CartPane(
                    cart = uiState.cart,
                    discount = uiState.transactionDiscount,
                    discountNote = uiState.transactionDiscountNote,
                    total = cartTotal,
                    taxEnabled = uiState.settings.isTaxEnabled,
                    taxRate = uiState.settings.taxPercentage,
                    onQuantityChange = { id, delta -> viewModel.updateCartQuantity(id, delta) },
                    onRemove = { viewModel.removeFromCart(it) },
                    onClearCart = {
                        viewModel.clearCart()
                        showMobileCartSheet = false
                    },
                    onOpenDiscountDialog = { showDiscountDialog = true },
                    onCheckoutClick = {
                        showMobileCartSheet = false
                        showCheckoutDialog = true
                    }
                )
            }
        }
    }

    // Checkout / Payment Dialog
    if (showCheckoutDialog) {
        CheckoutDialog(
            totalAmount = cartTotal,
            qrisPayload = uiState.settings.qrisPayload,
            onDismiss = { showCheckoutDialog = false },
            onPay = { method, cashPaid, qrisRef, notes ->
                viewModel.processPayment(
                    paymentMethod = method,
                    cashPaid = cashPaid,
                    qrisRef = qrisRef,
                    notes = notes,
                    onSuccess = {
                        showCheckoutDialog = false
                        onShowReceipt()
                    },
                    onError = { err ->
                        viewModel.showToast(err)
                    }
                )
            }
        )
    }

    // Transaction Discount Dialog
    if (showDiscountDialog) {
        DiscountDialog(
            currentDiscount = uiState.transactionDiscount,
            currentNote = uiState.transactionDiscountNote,
            onDismiss = { showDiscountDialog = false },
            onSave = { amount, note ->
                viewModel.setTransactionDiscount(amount, note)
                showDiscountDialog = false
            }
        )
    }
}

@Composable
fun ProductSearchAndCategories(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBarcodeScan: (String) -> Unit,
    categories: List<com.example.data.local.entity.Category>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari produk atau ketik barcode...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandToska) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pos_search_input"),
            singleLine = true
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == "Semua",
                    onClick = { onSelectCategory("Semua") },
                    label = { Text("Semua") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandToska,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat.name,
                    onClick = { onSelectCategory(cat.name) },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandToska,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun ProductGrid(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada produk ditemukan.", color = BrandGray)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
        ) {
            items(products) { product ->
                ProductCard(product = product, onClick = { onProductClick(product) })
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val isOutOfStock = product.stock <= 0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) { onClick() }
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandSageLight,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = product.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF386618),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = if (isOutOfStock) "Habis" else "${product.stock} ${product.unit}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOutOfStock) MaterialTheme.colorScheme.error else BrandGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = Formatters.formatRupiah(product.sellingPrice),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = BrandToska
            )
        }
    }
}

@Composable
fun CartPane(
    cart: List<CartItem>,
    discount: Double,
    discountNote: String,
    total: Double,
    taxEnabled: Boolean,
    taxRate: Double,
    onQuantityChange: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit,
    onClearCart: () -> Unit,
    onOpenDiscountDialog: () -> Unit,
    onCheckoutClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = BrandToska)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keranjang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (cart.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Kosongkan", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        Divider(color = BrandGrayBorder)

        // Cart items list
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = null,
                        tint = BrandGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Keranjang belanja kosong", color = BrandGray, fontSize = 13.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cart.forEach { item ->
                    CartItemRow(
                        item = item,
                        onQuantityChange = { delta -> onQuantityChange(item.product.id, delta) },
                        onRemove = { onRemove(item.product.id) }
                    )
                }
            }
        }

        // Summary and Checkout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Divider(color = BrandGrayBorder)

            val subtotal = cart.sumOf { it.subtotal }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", fontSize = 12.sp, color = BrandGray)
                Text(Formatters.formatRupiah(subtotal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDiscountDialog() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Diskon Transaksi", fontSize = 12.sp, color = BrandOrange)
                    Icon(Icons.Default.Edit, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(14.dp))
                }
                Text(
                    if (discount > 0) "-${Formatters.formatRupiah(discount)}" else "Rp 0",
                    fontSize = 12.sp,
                    color = BrandOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            if (taxEnabled) {
                val afterDisc = (subtotal - discount).coerceAtLeast(0.0)
                val taxAmt = afterDisc * (taxRate / 100.0)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pajak ($taxRate%)", fontSize = 12.sp, color = BrandGray)
                    Text(Formatters.formatRupiah(taxAmt), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TOTAL", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    Formatters.formatRupiah(total),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandToska
                )
            }

            Button(
                onClick = onCheckoutClick,
                enabled = cart.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pos_checkout_button")
            ) {
                Text("Lanjutkan Pembayaran", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                Text(
                    Formatters.formatRupiah(item.product.sellingPrice),
                    fontSize = 11.sp,
                    color = BrandGray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQuantityChange(-1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = { onQuantityChange(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    totalAmount: Double,
    qrisPayload: String,
    onDismiss: () -> Unit,
    onPay: (method: String, cashPaid: Double, qrisRef: String, notes: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("TUNAI") } // TUNAI, QRIS
    var cashPaidInput by remember { mutableStateOf("${totalAmount.toInt()}") }
    var qrisRefInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val cashPaidVal = cashPaidInput.toDoubleOrNull() ?: 0.0
    val changeAmt = (cashPaidVal - totalAmount).coerceAtLeast(0.0)
    val isInsufficient = selectedMethod == "TUNAI" && cashPaidVal < totalAmount

    // Generate QR Code bitmap for QRIS
    val qrisBitmap = remember(qrisPayload) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrisPayload, BarcodeFormat.QR_CODE, 300, 300)
            val bmp = Bitmap.createBitmap(300, 300, Bitmap.Config.RGB_565)
            for (x in 0 until 300) {
                for (y in 0 until 300) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pembayaran Kasir", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Total Box
                Surface(
                    color = BrandToskaLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Tagihan", fontSize = 12.sp, color = BrandToskaDark)
                        Text(
                            Formatters.formatRupiah(totalAmount),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandToskaDark
                        )
                    }
                }

                // Payment Method Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { selectedMethod = "TUNAI" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMethod == "TUNAI") BrandToska else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TUNAI", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedMethod = "QRIS" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMethod == "QRIS") BrandToska else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QRIS", fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedMethod == "TUNAI") {
                    OutlinedTextField(
                        value = cashPaidInput,
                        onValueChange = { cashPaidInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Uang Diterima (Rp)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_paid_input"),
                        singleLine = true
                    )

                    // Quick Cash Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Pas" to totalAmount,
                            "+10rb" to totalAmount + 10000,
                            "+20rb" to totalAmount + 20000,
                            "+50rb" to totalAmount + 50000,
                            "+100rb" to totalAmount + 100000
                        ).forEach { (label, amt) ->
                            SuggestionChip(
                                onClick = { cashPaidInput = "${amt.toInt()}" },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Change calculation card
                    Surface(
                        color = if (isInsufficient) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isInsufficient) "Kurang:" else "Kembalian:",
                                fontWeight = FontWeight.Bold,
                                color = if (isInsufficient) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                            Text(
                                if (isInsufficient)
                                    "-${Formatters.formatRupiah(totalAmount - cashPaidVal)}"
                                else
                                    Formatters.formatRupiah(changeAmt),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isInsufficient) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                        }
                    }
                } else {
                    // QRIS Display & Reference
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (qrisBitmap != null) {
                            Image(
                                bitmap = qrisBitmap.asImageBitmap(),
                                contentDescription = "QRIS Code",
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BrandGrayBorder, RoundedCornerShape(8.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Arahkan pembeli memindai QRIS di atas.",
                            fontSize = 12.sp,
                            color = BrandGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = qrisRefInput,
                            onValueChange = { qrisRefInput = it },
                            label = { Text("Nomor Referensi QRIS (RRN / Opsional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = "PENTING: Kasir wajib memastikan uang telah masuk ke rekening usaha sebelum menyelesaikan transaksi.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Catatan Transaksi (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPay(
                        selectedMethod,
                        if (selectedMethod == "TUNAI") cashPaidVal else totalAmount,
                        qrisRefInput.trim(),
                        notesInput.trim()
                    )
                },
                enabled = !isInsufficient,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Konfirmasi & Simpan", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun DiscountDialog(
    currentDiscount: Double,
    currentNote: String,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var discountInput by remember { mutableStateOf(if (currentDiscount > 0) "${currentDiscount.toInt()}" else "") }
    var noteInput by remember { mutableStateOf(currentNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diskon Transaksi", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal Potongan (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Keterangan Diskon (misal: Promo Pembukaan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val disc = discountInput.toDoubleOrNull() ?: 0.0
                    onSave(disc, noteInput.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
            ) {
                Text("Terapkan Diskon")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun CashierSelectorDropdown(
    crews: List<Crew>,
    selectedCashier: Crew?,
    onSelect: (Crew) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            color = BrandToskaDark,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedCashier?.name ?: "Pilih Kasir",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            crews.forEach { crew ->
                DropdownMenuItem(
                    text = { Text("${crew.name} (${crew.role})") },
                    onClick = {
                        onSelect(crew)
                        expanded = false
                    }
                )
            }
        }
    }
}
