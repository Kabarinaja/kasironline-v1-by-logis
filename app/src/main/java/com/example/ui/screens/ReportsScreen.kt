package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.repository.TopProductSummary
import com.example.localization.LocalizedStrings
import com.example.ui.components.Formatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    uiState: PosUiState,
    modifier: Modifier = Modifier
) {
    val strings = LocalizedStrings.get(uiState.language)
    val summary = uiState.salesReport
    var isGeneratingPdf by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Laporan Penjualan",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    Button(
                        onClick = {
                            isGeneratingPdf = true
                            viewModel.exportPdfReport { file ->
                                isGeneratingPdf = false
                                if (file != null) {
                                    viewModel.sharePdf(file)
                                }
                            }
                        },
                        enabled = !isGeneratingPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ekspor PDF", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandToska)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Period Selector
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val periods = listOf(
                        "today" to "Hari Ini",
                        "yesterday" to "Kemarin",
                        "7days" to "7 Hari Terakhir",
                        "month" to "Bulan Ini"
                    )
                    items(periods) { (id, label) ->
                        val selected = uiState.selectedReportPeriod == id
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.refreshSalesReport(id) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandToska,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Summary Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "TOTAL PENJUALAN",
                        value = Formatters.formatRupiah(summary?.totalSales ?: 0.0),
                        icon = Icons.Default.MonetizationOn,
                        iconTint = BrandToska,
                        containerColor = BrandToskaLight,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "JUMLAH TRANSAKSI",
                        value = "${summary?.totalTransactions ?: 0} Transaksi",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = BrandOrange,
                        containerColor = BrandOrangeLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tunai vs QRIS Breakdown Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Pemisahan Metode Pembayaran",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("TUNAI (CASH)", fontSize = 11.sp, color = BrandOrangeDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        Formatters.formatRupiah(summary?.cashSales ?: 0.0),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOrangeDark
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFFE0F2F1),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("QRIS (NON-TUNAI)", fontSize = 11.sp, color = BrandToskaDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        Formatters.formatRupiah(summary?.qrisSales ?: 0.0),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandToskaDark
                                    )
                                }
                            }
                        }

                        Divider(color = BrandGrayBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Potongan Diskon Diberikan:", fontSize = 12.sp, color = BrandGray)
                            Text(Formatters.formatRupiah(summary?.totalDiscount ?: 0.0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimasi Laba Kotor (Jika modal diisi):", fontSize = 12.sp, color = BrandGray)
                            Text(
                                Formatters.formatRupiah(summary?.estimatedProfit ?: 0.0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // Top Selling Products
            item {
                Text(
                    text = "Produk Terlaris",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            val topProducts = summary?.topSellingByQty ?: emptyList()
            if (topProducts.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada transaksi penjualan pada periode ini.",
                        fontSize = 13.sp,
                        color = BrandGray
                    )
                }
            } else {
                items(topProducts.take(5)) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${item.totalQty} unit terjual", fontSize = 11.sp, color = BrandGray)
                            }
                            Text(
                                Formatters.formatRupiah(item.totalRevenue),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandToska
                            )
                        }
                    }
                }
            }

            // Sales by Cashier Breakdown
            if (summary != null && summary.salesByCashier.isNotEmpty()) {
                item {
                    Text(
                        text = "Penjualan per Kasir",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(summary.salesByCashier.entries.toList()) { (cashier, amount) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cashier, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(Formatters.formatRupiah(amount), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandToska)
                        }
                    }
                }
            }
        }
    }
}
