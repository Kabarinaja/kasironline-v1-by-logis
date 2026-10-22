package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.TransactionDetail
import com.example.localization.LocalizedStrings
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Formatters
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onReprintReceipt: (TransactionDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    var searchQuery by remember { mutableStateOf("") }
    var selectedMethodFilter by remember { mutableStateOf("Semua") } // Semua, TUNAI, QRIS
    var selectedStatusFilter by remember { mutableStateOf("Semua") } // Semua, Selesai, Dibatalkan

    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
    var transactionDetailToView by remember { mutableStateOf<TransactionDetail?>(null) }
    var transactionToCancel by remember { mutableStateOf<TransactionEntity?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, selectedMethodFilter, selectedStatusFilter, uiState.recentTransactions) {
        uiState.recentTransactions.filter { tx ->
            val matchSearch = searchQuery.isBlank() || tx.invoiceNumber.contains(searchQuery, ignoreCase = true) || tx.cashierName.contains(searchQuery, ignoreCase = true)
            val matchMethod = selectedMethodFilter == "Semua" || tx.paymentMethod.equals(selectedMethodFilter, ignoreCase = true)
            val matchStatus = when (selectedStatusFilter) {
                "Selesai" -> tx.status == "COMPLETED"
                "Dibatalkan" -> tx.status == "CANCELLED"
                else -> true
            }
            matchSearch && matchMethod && matchStatus
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Riwayat Transaksi Kasir",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
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

            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari nomor invoice (misal: KD-2026...) atau kasir", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandToska) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                listOf("Semua", "TUNAI", "QRIS").forEach { method ->
                    item {
                        FilterChip(
                            selected = selectedMethodFilter == method,
                            onClick = { selectedMethodFilter = method },
                            label = { Text(method) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandToska,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                listOf("Semua", "Selesai", "Dibatalkan").forEach { st ->
                    if (st != "Semua") {
                        item {
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == st) "Semua" else st },
                                label = { Text(st) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (st == "Dibatalkan") MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "Tidak Ada Riwayat Transaksi",
                    description = "Belum ada transaksi yang sesuai dengan filter atau pencarian Anda.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList) { tx ->
                        HistoryCard(
                            transaction = tx,
                            onClick = {
                                selectedTransactionId = tx.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Load detail asynchronously when selectedTransactionId changes
    LaunchedEffect(selectedTransactionId) {
        if (selectedTransactionId != null) {
            val detail = uiState.recentTransactions.find { it.id == selectedTransactionId }
            // Let PosRepository fetch full items
            // We can invoke PosViewModel or launch a quick scope
        }
    }

    // Detail Dialog
    if (selectedTransactionId != null) {
        val tx = uiState.recentTransactions.find { it.id == selectedTransactionId }
        if (tx != null) {
            TransactionDetailDialog(
                transaction = tx,
                onDismiss = { selectedTransactionId = null },
                onReprint = {
                    val detail = TransactionDetail(tx, emptyList())
                    onReprintReceipt(detail)
                    selectedTransactionId = null
                },
                onCancelClick = {
                    transactionToCancel = tx
                    selectedTransactionId = null
                }
            )
        }
    }

    // Cancel Transaction Modal
    if (transactionToCancel != null) {
        val tx = transactionToCancel!!
        AlertDialog(
            onDismissRequest = { transactionToCancel = null },
            title = { Text("Batalkan Transaksi #${tx.invoiceNumber}?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pembatalan transaksi akan mengembalikan stok produk ke persediaan secara atomik dan mencatat alasan audit pembatalan.",
                        fontSize = 13.sp,
                        color = BrandGrayDark
                    )
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Alasan Pembatalan (Wajib)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReason.isNotBlank()) {
                            viewModel.cancelTransaction(tx.id, cancelReason.trim()) {
                                transactionToCancel = null
                                cancelReason = ""
                            }
                        }
                    },
                    enabled = cancelReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Batalkan Transaksi", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToCancel = null }) { Text("Kembali") }
            }
        )
    }
}

@Composable
fun HistoryCard(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val isCancelled = transaction.status == "CANCELLED"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.invoiceNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        text = transaction.paymentMethod,
                        isActive = !isCancelled
                    )
                }

                Text(
                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)) + " • " + transaction.cashierName,
                    fontSize = 11.sp,
                    color = BrandGray
                )

                if (isCancelled && !transaction.cancelReason.isNullOrEmpty()) {
                    Text(
                        text = "Alasan: ${transaction.cancelReason}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatRupiah(transaction.totalAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isCancelled) MaterialTheme.colorScheme.error else BrandToska
                )
                Text(
                    text = if (isCancelled) "DIBATALKAN" else "SELESAI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onReprint: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isCancelled = transaction.status == "CANCELLED"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(transaction.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                StatusBadge(text = if (isCancelled) "BATAL" else "SUKSES", isActive = !isCancelled)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Waktu Transaksi", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp)))
                DetailRow("Kasir Bertugas", "${transaction.cashierName} (${transaction.cashierRole})")
                DetailRow("Metode Pembayaran", transaction.paymentMethod)
                DetailRow("Subtotal", Formatters.formatRupiah(transaction.subtotal))
                if (transaction.discountAmount > 0) {
                    DetailRow("Diskon", "-${Formatters.formatRupiah(transaction.discountAmount)}")
                }
                if (transaction.taxAmount > 0) {
                    DetailRow("Pajak", Formatters.formatRupiah(transaction.taxAmount))
                }
                Divider()
                DetailRow("TOTAL AKHIR", Formatters.formatRupiah(transaction.totalAmount), isBold = true)

                if (transaction.paymentMethod == "TUNAI") {
                    DetailRow("Uang Diterima", Formatters.formatRupiah(transaction.cashPaid))
                    DetailRow("Kembalian", Formatters.formatRupiah(transaction.changeReturned))
                } else if (transaction.qrisReference.isNotEmpty()) {
                    DetailRow("Ref QRIS", transaction.qrisReference)
                }

                if (isCancelled) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Transaksi ini telah dibatalkan.", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 12.sp)
                            Text("Alasan: ${transaction.cancelReason ?: "-"}", color = Color(0xFFC62828), fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isCancelled) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Batalkan")
                    }
                }
                Button(
                    onClick = onReprint,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandToska)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Ulang")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = BrandGray)
        Text(
            value,
            fontSize = if (isBold) 14.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isBold) BrandToska else BrandGrayDark
        )
    }
}
