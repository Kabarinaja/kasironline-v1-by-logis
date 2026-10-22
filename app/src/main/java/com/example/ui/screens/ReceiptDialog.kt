package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.TransactionDetail
import com.example.ui.components.Formatters
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosUiState
import com.example.ui.viewmodel.PosViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptDialog(
    detail: TransactionDetail,
    viewModel: PosViewModel,
    uiState: PosUiState,
    onDismiss: () -> Unit
) {
    val tx = detail.transaction
    val items = detail.items
    val settings = uiState.settings

    var printStatus by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    // Optional Promo QR
    val promoQrBitmap = remember(settings.showPromoQr, settings.promoQrText) {
        if (settings.showPromoQr && settings.promoQrText.isNotEmpty()) {
            try {
                val writer = QRCodeWriter()
                val matrix = writer.encode(settings.promoQrText, BarcodeFormat.QR_CODE, 200, 200)
                val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565)
                for (x in 0 until 200) {
                    for (y in 0 until 200) {
                        bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                bmp
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = BrandToska,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pratinjau Struk Pembayaran",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (printStatus != null) {
                            Text(
                                text = printStatus ?: "",
                                color = BrandOrange,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    isPrinting = true
                                    printStatus = "Mengirim data ke printer..."
                                    viewModel.printCurrentReceipt(detail) { success, msg ->
                                        isPrinting = false
                                        printStatus = msg
                                    }
                                },
                                enabled = !isPrinting,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandToska),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("receipt_print_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cetak Struk", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("receipt_new_order_button")
                            ) {
                                Text("Transaksi Baru", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFECEFF1))
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Thermal receipt paper card
                Card(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Header
                        if (settings.showHeaderInfo) {
                            Text(
                                text = settings.businessName,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            if (settings.address.isNotEmpty()) {
                                Text(
                                    text = settings.address,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (settings.phone.isNotEmpty()) {
                                Text(
                                    text = "Telp: ${settings.phone}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                            ReceiptDivider()
                        }

                        // Meta
                        ReceiptRow("No. Transaksi", tx.invoiceNumber)
                        ReceiptRow(
                            "Waktu",
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                        )
                        if (settings.showCashierInfo) {
                            ReceiptRow("Kasir", "${tx.cashierName} (${tx.cashierRole})")
                        }
                        ReceiptDivider()

                        // Items
                        items.forEach { item ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    text = item.productName,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "  ${item.quantity} x ${Formatters.formatRupiah(item.sellingPrice)}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(item.subtotal),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        ReceiptDivider()

                        // Totals
                        ReceiptRow("Subtotal", Formatters.formatRupiah(tx.subtotal))
                        if (tx.discountAmount > 0) {
                            ReceiptRow("Diskon", "-${Formatters.formatRupiah(tx.discountAmount)}")
                        }
                        if (tx.taxAmount > 0) {
                            ReceiptRow("Pajak (${tx.taxRate}%)", Formatters.formatRupiah(tx.taxAmount))
                        }
                        ReceiptDivider()

                        // Grand Total
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = Formatters.formatRupiah(tx.totalAmount),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        ReceiptDivider()

                        // Payment Details
                        ReceiptRow("Metode Bayar", tx.paymentMethod)
                        if (tx.paymentMethod == "TUNAI") {
                            ReceiptRow("Tunai Diterima", Formatters.formatRupiah(tx.cashPaid))
                            ReceiptRow("Kembalian", Formatters.formatRupiah(tx.changeReturned))
                        } else {
                            if (tx.qrisReference.isNotEmpty()) {
                                ReceiptRow("Ref QRIS", tx.qrisReference)
                            }
                            ReceiptRow("Status", "LUNAS (VERIFIKASI)")
                        }

                        // Promo QR
                        if (promoQrBitmap != null) {
                            ReceiptDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pindai Promo / Website:",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Image(
                                bitmap = promoQrBitmap.asImageBitmap(),
                                contentDescription = "Promo QR",
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(vertical = 4.dp)
                            )
                        }

                        // Footer note
                        if (settings.receiptFooter.isNotEmpty()) {
                            ReceiptDivider()
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = settings.receiptFooter,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color.DarkGray
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ReceiptDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - -",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = Color.LightGray,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}
