package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.preferences.BusinessSettings
import com.example.data.repository.SalesSummary
import com.example.data.repository.TransactionDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PdfReportGenerator(private val context: Context) {

    private val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))

    private fun formatRupiah(amount: Double): String = "Rp " + numberFormat.format(amount.toLong())

    suspend fun generateSalesReportPdf(
        settings: BusinessSettings,
        periodLabel: String,
        summary: SalesSummary,
        transactions: List<TransactionDetail>
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 (595 x 842 pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0D8B96")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            textSize = 10f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111111")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 40f

        // Top Brand Header Bar
        paint.color = Color.parseColor("#0D8B96")
        canvas.drawRect(30f, y, 565f, y + 4f, paint)
        y += 24f

        // Header Store Info
        canvas.drawText(settings.businessName, 30f, y, titlePaint)
        paint.color = Color.parseColor("#FF9024")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KASIR DUNIA — LAPORAN PENJUALAN OFFLINE", 340f, y - 4f, paint)

        y += 16f
        canvas.drawText("${settings.address}, ${settings.cityProvince} | Telp: ${settings.phone}", 30f, y, headerPaint)
        y += 14f
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Periode: $periodLabel | Dibuat pada: $nowStr", 30f, y, headerPaint)

        y += 20f
        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 18f

        // Metric Summary Cards (2 rows of 3 columns)
        paint.color = Color.parseColor("#F4F9F9")
        canvas.drawRoundRect(30f, y, 195f, y + 48f, 6f, 6f, paint)
        canvas.drawRoundRect(210f, y, 375f, y + 48f, 6f, 6f, paint)
        canvas.drawRoundRect(390f, y, 565f, y + 48f, 6f, 6f, paint)

        paint.color = Color.parseColor("#757575")
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("TOTAL PENJUALAN", 40f, y + 16f, paint)
        canvas.drawText("TOTAL TRANSAKSI", 220f, y + 16f, paint)
        canvas.drawText("ESTIMASI LABA KOTOR", 400f, y + 16f, paint)

        paint.color = Color.parseColor("#0D8B96")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatRupiah(summary.totalSales), 40f, y + 36f, paint)
        canvas.drawText("${summary.totalTransactions} Transaksi", 220f, y + 36f, paint)
        paint.color = Color.parseColor("#2E7D32")
        canvas.drawText(formatRupiah(summary.estimatedProfit), 40f + 360f, y + 36f, paint)

        y += 56f

        // Payment Breakdown (TUNAI vs QRIS)
        paint.color = Color.parseColor("#FFF8F0")
        canvas.drawRoundRect(30f, y, 280f, y + 48f, 6f, 6f, paint)
        paint.color = Color.parseColor("#F0F7F0")
        canvas.drawRoundRect(295f, y, 565f, y + 48f, 6f, 6f, paint)

        paint.color = Color.parseColor("#757575")
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("PEMBAYARAN TUNAI (CASH)", 40f, y + 16f, paint)
        canvas.drawText("PEMBAYARAN QRIS", 305f, y + 16f, paint)

        paint.color = Color.parseColor("#FF9024")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatRupiah(summary.cashSales), 40f, y + 36f, paint)
        paint.color = Color.parseColor("#0D8B96")
        canvas.drawText(formatRupiah(summary.qrisSales), 305f, y + 36f, paint)

        y += 64f

        // Section: Top 5 Selling Products
        paint.color = Color.parseColor("#0D8B96")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PRODUK TERLARIS (TOP 5)", 30f, y, paint)
        y += 10f

        // Table Header for Top Products
        paint.color = Color.parseColor("#0D8B96")
        canvas.drawRect(30f, y, 565f, y + 20f, paint)
        canvas.drawText("No", 40f, y + 14f, tableHeaderPaint)
        canvas.drawText("Nama Produk", 80f, y + 14f, tableHeaderPaint)
        canvas.drawText("Unit Terjual", 350f, y + 14f, tableHeaderPaint)
        canvas.drawText("Total Pendapatan", 450f, y + 14f, tableHeaderPaint)
        y += 20f

        val topList = summary.topSellingByQty.take(5)
        if (topList.isEmpty()) {
            paint.color = Color.parseColor("#F9F9F9")
            canvas.drawRect(30f, y, 565f, y + 20f, paint)
            paint.color = Color.GRAY
            paint.textSize = 9f
            canvas.drawText("Belum ada data penjualan produk.", 40f, y + 14f, paint)
            y += 20f
        } else {
            topList.forEachIndexed { index, item ->
                paint.color = if (index % 2 == 0) Color.WHITE else Color.parseColor("#F9FBFC")
                canvas.drawRect(30f, y, 565f, y + 18f, paint)
                paint.color = Color.parseColor("#222222")
                paint.textSize = 9f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText("${index + 1}", 40f, y + 13f, paint)
                canvas.drawText(item.productName.take(35), 80f, y + 13f, paint)
                canvas.drawText("${item.totalQty} unit", 350f, y + 13f, paint)
                canvas.drawText(formatRupiah(item.totalRevenue), 450f, y + 13f, paint)
                y += 18f
            }
        }

        y += 24f

        // Section: Recent Transactions Table
        paint.color = Color.parseColor("#0D8B96")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RINCIAN TRANSAKSI (Hingga 12 Terakhir)", 30f, y, paint)
        y += 10f

        // Table Header
        paint.color = Color.parseColor("#0D8B96")
        canvas.drawRect(30f, y, 565f, y + 20f, paint)
        canvas.drawText("No. Invoice", 40f, y + 14f, tableHeaderPaint)
        canvas.drawText("Waktu", 160f, y + 14f, tableHeaderPaint)
        canvas.drawText("Kasir", 260f, y + 14f, tableHeaderPaint)
        canvas.drawText("Metode", 370f, y + 14f, tableHeaderPaint)
        canvas.drawText("Total", 470f, y + 14f, tableHeaderPaint)
        y += 20f

        val txList = transactions.take(12)
        if (txList.isEmpty()) {
            paint.color = Color.parseColor("#F9F9F9")
            canvas.drawRect(30f, y, 565f, y + 20f, paint)
            paint.color = Color.GRAY
            paint.textSize = 9f
            canvas.drawText("Tidak ada transaksi pada periode ini.", 40f, y + 14f, paint)
            y += 20f
        } else {
            val timeFmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            txList.forEachIndexed { index, item ->
                val tx = item.transaction
                paint.color = if (index % 2 == 0) Color.WHITE else Color.parseColor("#F9FBFC")
                canvas.drawRect(30f, y, 565f, y + 18f, paint)

                paint.color = if (tx.status == "CANCELLED") Color.RED else Color.parseColor("#222222")
                paint.textSize = 8.5f
                paint.typeface = Typeface.DEFAULT

                val statusSuffix = if (tx.status == "CANCELLED") " (Batal)" else ""
                canvas.drawText(tx.invoiceNumber + statusSuffix, 40f, y + 13f, paint)
                canvas.drawText(timeFmt.format(Date(tx.timestamp)), 160f, y + 13f, paint)
                canvas.drawText(tx.cashierName.take(15), 260f, y + 13f, paint)
                canvas.drawText(tx.paymentMethod, 370f, y + 13f, paint)
                canvas.drawText(formatRupiah(tx.totalAmount), 470f, y + 13f, paint)
                y += 18f
            }
        }

        // Footer
        y = 810f
        paint.color = Color.parseColor("#CCCCCC")
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 14f
        paint.color = Color.GRAY
        paint.textSize = 8f
        canvas.drawText("Dicetak secara otomatis oleh KASIR DUNIA — Aplikasi Kasir Offline Profesional.", 30f, y, paint)
        canvas.drawText("Halaman 1 dari 1", 500f, y, paint)

        pdfDocument.finishPage(page)

        // Write to file
        val outputDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outputFile = File(outputDir, "Laporan_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        outputFile
    }

    fun openOrSharePdf(pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Penjualan KASIR DUNIA")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Bagikan Laporan PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
