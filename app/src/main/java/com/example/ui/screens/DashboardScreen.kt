package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
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
fun DashboardScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    onNavigateToPos: () -> Unit,
    onNavigateToSession: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val todayReport = uiState.salesReport

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.settings.businessName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Kasir Aktif: ${uiState.selectedCashier?.name ?: uiState.settings.ownerName}",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrandSageContainer)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSession,
                        modifier = Modifier.testTag("dashboard_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = "Sesi Kasir",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToPos,
                containerColor = BrandOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null) },
                text = { Text(strings.startTransaction, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_start_transaction")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
        ) {
            // Sesi Kasir Status Banner
            item {
                CashierSessionCard(
                    uiState = uiState,
                    onOpenCloseClick = onNavigateToSession
                )
            }

            // Sales Metrics Grid (Hari Ini)
            item {
                Text(
                    text = strings.todaySales,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = strings.todaySales,
                        value = Formatters.formatRupiah(todayReport?.totalSales ?: 0.0),
                        icon = Icons.Default.MonetizationOn,
                        iconTint = BrandToska,
                        containerColor = BrandToskaLight,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = strings.todayTransactions,
                        value = "${todayReport?.totalTransactions ?: 0} Order",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = BrandOrange,
                        containerColor = BrandOrangeLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = strings.cashTotal,
                        value = Formatters.formatRupiah(todayReport?.cashSales ?: 0.0),
                        icon = Icons.Default.Payments,
                        iconTint = Color(0xFF2E7D32),
                        containerColor = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = strings.qrisTotal,
                        value = Formatters.formatRupiah(todayReport?.qrisSales ?: 0.0),
                        icon = Icons.Default.QrCode2,
                        iconTint = BrandToska,
                        containerColor = BrandToskaLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Low Stock Alert Banner (if any)
            if (uiState.lowStockProducts.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToProducts() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Peringatan Stok Menipis!",
                                    fontWeight = FontWeight.Bold,
                                    color = BrandOrangeDark,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Ada ${uiState.lowStockProducts.size} produk mencapai atau di bawah batas minimum stok.",
                                    color = BrandGrayDark,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = BrandOrange
                            )
                        }
                    }
                }
            }

            // Quick Product Count Info
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Katalog Produk",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onNavigateToProducts) {
                        Text("Kelola (${uiState.activeProductCount})", color = BrandToska, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentTransactions,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (uiState.recentTransactions.isNotEmpty()) {
                        TextButton(onClick = onNavigateToHistory) {
                            Text("Lihat Semua", color = BrandToska, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Empty state or list
            if (uiState.recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.ReceiptLong,
                        title = strings.emptyTransactionsNotice,
                        description = strings.emptyTransactionsTip,
                        actionButtonText = strings.startTransaction,
                        onActionClick = onNavigateToPos
                    )
                }
            } else {
                items(uiState.recentTransactions.take(5)) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        onClick = onNavigateToHistory
                    )
                }
            }
        }
    }
}

@Composable
fun CashierSessionCard(
    uiState: PosUiState,
    onOpenCloseClick: () -> Unit
) {
    val session = uiState.activeSession
    val isOpen = session != null

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOpen) Color(0xFF2E7D32) else BrandGray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOpen) "SESI KASIR AKTIF" else "SESI KASIR BELUM DIBUKA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isOpen) Color(0xFF2E7D32) else BrandGray
                    )
                }

                Button(
                    onClick = onOpenCloseClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOpen) MaterialTheme.colorScheme.error else BrandToska
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (isOpen) "Tutup Sesi" else "Buka Sesi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isOpen && session != null) {
                Divider(color = BrandGrayBorder, thickness = 0.8.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Kasir Bertugas", fontSize = 11.sp, color = BrandGray)
                        Text(session.cashierName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Modal Awal Laci", fontSize = 11.sp, color = BrandGray)
                        Text(Formatters.formatRupiah(session.openingCash), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Text(
                    text = "Buka sesi kasir untuk mencatat modal laci dan memantau uang tunai secara akurat.",
                    fontSize = 12.sp,
                    color = BrandGray
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
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
                    text = title,
                    fontSize = 11.sp,
                    color = BrandGrayDark,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGrayDark
            )
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                        isActive = transaction.status == "COMPLETED"
                    )
                }
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)) + " • " + transaction.cashierName,
                    fontSize = 11.sp,
                    color = BrandGray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatRupiah(transaction.totalAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (transaction.status == "CANCELLED") MaterialTheme.colorScheme.error else BrandToska
                )
                if (transaction.status == "CANCELLED") {
                    Text("Dibatalkan", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
