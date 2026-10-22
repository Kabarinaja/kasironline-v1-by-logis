package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Category
import com.example.data.local.entity.Product
import com.example.localization.LocalizedStrings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Formatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var productToAdjustStock by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val filteredList = remember(searchQuery, selectedCategory, showOnlyLowStock, uiState.allProducts) {
        uiState.allProducts.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "Semua" || p.category.equals(selectedCategory, ignoreCase = true)
            val matchLow = !showOnlyLowStock || p.stock <= p.minStock
            matchQuery && matchCat && matchLow
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manajemen Produk & Stok",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.exportCsv { file ->
                            viewModel.shareBackupFile(file, "text/csv", "Ekspor CSV Produk")
                        }
                    }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Ekspor CSV", tint = Color.White)
                    }
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Kelola Kategori", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddEditDialog = true
                },
                containerColor = BrandOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah Produk", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_product")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari berdasarkan nama, SKU, atau barcode...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandToska) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category & Low Stock Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = showOnlyLowStock,
                        onClick = { showOnlyLowStock = !showOnlyLowStock },
                        label = { Text("Stok Menipis (${uiState.lowStockProducts.size})") },
                        leadingIcon = {
                            if (showOnlyLowStock) Icon(Icons.Default.Check, contentDescription = null)
                            else Icon(Icons.Default.Warning, contentDescription = null, tint = BrandOrange)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedCategory == "Semua",
                        onClick = { selectedCategory = "Semua" },
                        label = { Text("Semua Kategori") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandToska,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(uiState.categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandToska,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Products list
            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    title = "Tidak Ada Produk Ditemukan",
                    description = if (showOnlyLowStock) "Semua produk memiliki stok yang aman." else "Belum ada produk yang cocok dengan pencarian atau filter Anda.",
                    actionButtonText = "Tambah Produk Baru",
                    onActionClick = {
                        productToEdit = null
                        showAddEditDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp)
                ) {
                    items(filteredList) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = {
                                productToEdit = product
                                showAddEditDialog = true
                            },
                            onAdjustStock = {
                                productToAdjustStock = product
                            },
                            onDelete = {
                                productToDelete = product
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            initialProduct = productToEdit,
            categories = uiState.categories,
            onDismiss = { showAddEditDialog = false },
            onSave = { product ->
                viewModel.saveProduct(product) {
                    showAddEditDialog = false
                }
            }
        )
    }

    // Adjust Stock Dialog
    if (productToAdjustStock != null) {
        val prod = productToAdjustStock!!
        AdjustStockDialog(
            product = prod,
            onDismiss = { productToAdjustStock = null },
            onConfirm = { qtyChange, reason, type ->
                viewModel.adjustProductStock(prod.id, qtyChange, reason, type) {
                    productToAdjustStock = null
                }
            }
        )
    }

    // Delete Product Confirmation
    if (productToDelete != null) {
        val prod = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk?") },
            text = {
                Text("Yakin ingin menghapus '${prod.name}'? Produk akan dinonaktifkan dari katalog namun riwayat transaksi lama tetap tersimpan secara aman.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod.id) {
                            productToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Batal") }
            }
        )
    }

    // Manage Categories Dialog
    if (showAddCategoryDialog) {
        ManageCategoriesDialog(
            categories = uiState.categories,
            onDismiss = { showAddCategoryDialog = false },
            onAddCategory = { name ->
                viewModel.saveCategory(Category(name = name)) {}
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stock <= product.minStock

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = BrandSageLight,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF33691E),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Jual: ${Formatters.formatRupiah(product.sellingPrice)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandToska
                    )
                    if (product.costPrice > 0) {
                        Text(
                            text = "Modal: ${Formatters.formatRupiah(product.costPrice)}",
                            fontSize = 12.sp,
                            color = BrandGray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.sku.isNotEmpty()) {
                        Text("SKU: ${product.sku} • ", fontSize = 11.sp, color = BrandGray)
                    }
                    Text(
                        text = "Stok: ${product.stock} ${product.unit}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) MaterialTheme.colorScheme.error else BrandGrayDark
                    )
                    if (isLowStock) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Menipis",
                            tint = BrandOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAdjustStock, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Ubah Stok", tint = BrandToska)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandGray)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    initialProduct: Product?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: (categories.firstOrNull()?.name ?: "Umum")) }
    var costPrice by remember { mutableStateOf(if ((initialProduct?.costPrice ?: 0.0) > 0) "${initialProduct!!.costPrice.toInt()}" else "") }
    var sellingPrice by remember { mutableStateOf(if ((initialProduct?.sellingPrice ?: 0.0) > 0) "${initialProduct!!.sellingPrice.toInt()}" else "") }
    var stock by remember { mutableStateOf("${initialProduct?.stock ?: 10}") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "Pcs") }
    var minStock by remember { mutableStateOf("${initialProduct?.minStock ?: 5}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProduct == null) "Tambah Produk Baru" else "Ubah Data Produk", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category selection dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = {
                            IconButton(onClick = { categoryExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat.name
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Jual (Rp) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_price_input")
                    )
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Modal (Rp)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { c -> c.isDigit() } },
                        label = { Text("Stok *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan (Pcs/Cup/dll)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = minStock,
                    onValueChange = { minStock = it.filter { c -> c.isDigit() } },
                    label = { Text("Batas Minimum Peringatan Stok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sellVal = sellingPrice.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && sellVal > 0.0) {
                        val product = Product(
                            id = initialProduct?.id ?: 0L,
                            name = name.trim(),
                            sku = sku.trim(),
                            barcode = barcode.trim(),
                            category = category,
                            costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                            sellingPrice = sellVal,
                            stock = stock.toIntOrNull() ?: 0,
                            unit = if (unit.isNotBlank()) unit.trim() else "Pcs",
                            minStock = minStock.toIntOrNull() ?: 5
                        )
                        onSave(product)
                    }
                },
                enabled = name.isNotBlank() && (sellingPrice.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Simpan Produk", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AdjustStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (qtyChange: Int, reason: String, type: String) -> Unit
) {
    var type by remember { mutableStateOf("IN") } // IN, OUT, ADJUSTMENT
    var qtyInput by remember { mutableStateOf("1") }
    var reasonInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Penyesuaian Stok: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Stok saat ini: ${product.stock} ${product.unit}", fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = type == "IN",
                        onClick = { type = "IN" },
                        label = { Text("Stok Masuk (+)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandToska, selectedLabelColor = Color.White)
                    )
                    FilterChip(
                        selected = type == "OUT",
                        onClick = { type = "OUT" },
                        label = { Text("Stok Keluar (-)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.error, selectedLabelColor = Color.White)
                    )
                }

                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { qtyInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Jumlah Perubahan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Alasan (misal: Pembelian dari Supplier, Rusak, dll)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawQty = qtyInput.toIntOrNull() ?: 0
                    if (rawQty > 0) {
                        val change = if (type == "OUT") -rawQty else rawQty
                        val reason = if (reasonInput.isNotBlank()) reasonInput.trim() else "Penyesuaian Stok Manual"
                        onConfirm(change, reason, type)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun ManageCategoriesDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategori Produk", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("Nama kategori baru...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
                    ) {
                        Text("Tambah")
                    }
                }

                Divider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { cat ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cat.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
