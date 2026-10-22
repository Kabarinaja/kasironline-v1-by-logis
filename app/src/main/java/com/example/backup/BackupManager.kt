package com.example.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.preferences.BusinessSettings
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

sealed class BackupResult {
    data class Success(val file: File, val message: String) : BackupResult()
    data class Error(val error: String) : BackupResult()
}

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val preferences: UserPreferences
) {

    suspend fun createFullBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("app", "KASIR_DUNIA")
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            // 1. Settings
            val settings = preferences.businessSettingsFlow.first()
            val settingsObj = JSONObject().apply {
                put("ownerName", settings.ownerName)
                put("businessName", settings.businessName)
                put("businessType", settings.businessType)
                put("address", settings.address)
                put("cityProvince", settings.cityProvince)
                put("phone", settings.phone)
                put("email", settings.email)
                put("currency", settings.currency)
                put("timezone", settings.timezone)
                put("language", settings.language)
                put("receiptFooter", settings.receiptFooter)
                put("paperSize", settings.paperSize)
                put("isTaxEnabled", settings.isTaxEnabled)
                put("taxPercentage", settings.taxPercentage)
                put("showHeaderInfo", settings.showHeaderInfo)
                put("showCashierInfo", settings.showCashierInfo)
                put("showPromoQr", settings.showPromoQr)
                put("promoQrText", settings.promoQrText)
                put("qrisPayload", settings.qrisPayload)
            }
            root.put("settings", settingsObj)

            // 2. Categories
            val categories = database.categoryDao().getAllCategories().first()
            val categoriesArray = JSONArray()
            for (c in categories) {
                categoriesArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                })
            }
            root.put("categories", categoriesArray)

            // 3. Crews
            val crews = database.crewDao().getAllCrew().first()
            val crewsArray = JSONArray()
            for (cr in crews) {
                crewsArray.put(JSONObject().apply {
                    put("id", cr.id)
                    put("name", cr.name)
                    put("role", cr.role)
                    put("phone", cr.phone)
                    put("isActive", cr.isActive)
                    put("notes", cr.notes)
                    put("isDeleted", cr.isDeleted)
                })
            }
            root.put("crews", crewsArray)

            // 4. Products
            val products = database.productDao().getAllProducts().first()
            val productsArray = JSONArray()
            for (p in products) {
                productsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("barcode", p.barcode)
                    put("category", p.category)
                    put("costPrice", p.costPrice)
                    put("sellingPrice", p.sellingPrice)
                    put("stock", p.stock)
                    put("unit", p.unit)
                    put("minStock", p.minStock)
                    put("isActive", p.isActive)
                    put("isDeleted", p.isDeleted)
                    put("createdAt", p.createdAt)
                    put("updatedAt", p.updatedAt)
                })
            }
            root.put("products", productsArray)

            // 5. Transactions & Items
            val transactions = database.transactionDao().getAllTransactions().first()
            val transactionsArray = JSONArray()
            for (t in transactions) {
                val tObj = JSONObject().apply {
                    put("id", t.id)
                    put("invoiceNumber", t.invoiceNumber)
                    put("sessionId", t.sessionId ?: -1L)
                    put("cashierName", t.cashierName)
                    put("cashierRole", t.cashierRole)
                    put("timestamp", t.timestamp)
                    put("subtotal", t.subtotal)
                    put("discountAmount", t.discountAmount)
                    put("discountNote", t.discountNote)
                    put("taxRate", t.taxRate)
                    put("taxAmount", t.taxAmount)
                    put("totalAmount", t.totalAmount)
                    put("paymentMethod", t.paymentMethod)
                    put("cashPaid", t.cashPaid)
                    put("changeReturned", t.changeReturned)
                    put("qrisReference", t.qrisReference)
                    put("notes", t.notes)
                    put("status", t.status)
                    put("cancelledAt", t.cancelledAt ?: -1L)
                    put("cancelReason", t.cancelReason ?: "")
                }
                val items = database.transactionItemDao().getItemsForTransaction(t.id)
                val itemsArray = JSONArray()
                for (it in items) {
                    itemsArray.put(JSONObject().apply {
                        put("id", it.id)
                        put("transactionId", it.transactionId)
                        put("productId", it.productId ?: -1L)
                        put("productName", it.productName)
                        put("sku", it.sku)
                        put("sellingPrice", it.sellingPrice)
                        put("costPrice", it.costPrice)
                        put("quantity", it.quantity)
                        put("discountPerItem", it.discountPerItem)
                        put("subtotal", it.subtotal)
                    })
                }
                tObj.put("items", itemsArray)
                transactionsArray.put(tObj)
            }
            root.put("transactions", transactionsArray)

            // Save JSON to cache/backup
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val timeFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "KasirDunia_Backup_$timeFmt.json")
            FileOutputStream(file).use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }

            BackupResult.Success(file, "Pencadangan berhasil (${file.length() / 1024} KB)")
        } catch (e: Exception) {
            BackupResult.Error("Gagal mencadangkan data: ${e.message}")
        }
    }

    suspend fun restoreFromBackupJson(jsonString: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.optString("app") != "KASIR_DUNIA") {
                return@withContext BackupResult.Error("Format file tidak valid. Bukan file backup resmi KASIR DUNIA.")
            }

            val version = root.optInt("version", 0)
            if (version < 1) {
                return@withContext BackupResult.Error("Versi backup tidak didukung.")
            }

            // Restore settings if present
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val current = preferences.businessSettingsFlow.first()
                val restoredSettings = current.copy(
                    ownerName = sObj.optString("ownerName", current.ownerName),
                    businessName = sObj.optString("businessName", current.businessName),
                    businessType = sObj.optString("businessType", current.businessType),
                    address = sObj.optString("address", current.address),
                    cityProvince = sObj.optString("cityProvince", current.cityProvince),
                    phone = sObj.optString("phone", current.phone),
                    email = sObj.optString("email", current.email),
                    currency = sObj.optString("currency", current.currency),
                    timezone = sObj.optString("timezone", current.timezone),
                    language = sObj.optString("language", current.language),
                    receiptFooter = sObj.optString("receiptFooter", current.receiptFooter),
                    paperSize = sObj.optString("paperSize", current.paperSize),
                    isTaxEnabled = sObj.optBoolean("isTaxEnabled", current.isTaxEnabled),
                    taxPercentage = sObj.optDouble("taxPercentage", current.taxPercentage),
                    showHeaderInfo = sObj.optBoolean("showHeaderInfo", current.showHeaderInfo),
                    showCashierInfo = sObj.optBoolean("showCashierInfo", current.showCashierInfo),
                    showPromoQr = sObj.optBoolean("showPromoQr", current.showPromoQr),
                    promoQrText = sObj.optString("promoQrText", current.promoQrText),
                    qrisPayload = sObj.optString("qrisPayload", current.qrisPayload)
                )
                preferences.updateBusinessSettings(restoredSettings)
            }

            // Restore Categories
            if (root.has("categories")) {
                val arr = root.getJSONArray("categories")
                val categories = mutableListOf<Category>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    categories.add(Category(id = o.optLong("id", 0L), name = o.getString("name")))
                }
                database.categoryDao().clearAll()
                database.categoryDao().insertAll(categories)
            }

            // Restore Crews
            if (root.has("crews")) {
                val arr = root.getJSONArray("crews")
                val crews = mutableListOf<Crew>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    crews.add(
                        Crew(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            role = o.optString("role", "Kasir"),
                            phone = o.optString("phone", ""),
                            isActive = o.optBoolean("isActive", true),
                            notes = o.optString("notes", ""),
                            isDeleted = o.optBoolean("isDeleted", false)
                        )
                    )
                }
                database.crewDao().clearAll()
                database.crewDao().insertAll(crews)
            }

            // Restore Products
            if (root.has("products")) {
                val arr = root.getJSONArray("products")
                val products = mutableListOf<Product>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    products.add(
                        Product(
                            id = o.optLong("id", 0L),
                            name = o.getString("name"),
                            sku = o.optString("sku", ""),
                            barcode = o.optString("barcode", ""),
                            category = o.optString("category", "Umum"),
                            costPrice = o.optDouble("costPrice", 0.0),
                            sellingPrice = o.getDouble("sellingPrice"),
                            stock = o.optInt("stock", 0),
                            unit = o.optString("unit", "Pcs"),
                            minStock = o.optInt("minStock", 5),
                            isActive = o.optBoolean("isActive", true),
                            isDeleted = o.optBoolean("isDeleted", false),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                database.productDao().clearAll()
                database.productDao().insertAll(products)
            }

            BackupResult.Success(File(""), "Pemulihan data berhasil diselesaikan.")
        } catch (e: Exception) {
            BackupResult.Error("Gagal membaca atau memulihkan data: ${e.message}")
        }
    }

    suspend fun exportProductsToCsv(): File = withContext(Dispatchers.IO) {
        val products = database.productDao().getAllProducts().first()
        val dir = File(context.cacheDir, "csv").apply { mkdirs() }
        val file = File(dir, "Produk_KasirDunia_${System.currentTimeMillis()}.csv")

        FileOutputStream(file).use { out ->
            val header = "Nama,SKU,Barcode,Kategori,Harga Modal,Harga Jual,Stok,Satuan,Batas Minimum\n"
            out.write(header.toByteArray(Charsets.UTF_8))
            for (p in products) {
                val line = "\"${p.name.replace("\"", "\"\"")}\",\"${p.sku}\",\"${p.barcode}\",\"${p.category}\",${p.costPrice},${p.sellingPrice},${p.stock},\"${p.unit}\",${p.minStock}\n"
                out.write(line.toByteArray(Charsets.UTF_8))
            }
        }
        file
    }

    suspend fun importProductsFromCsv(csvContent: String): Int = withContext(Dispatchers.IO) {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return@withContext 0

        var count = 0
        val newProducts = mutableListOf<Product>()

        // skip header
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            if (parts.size >= 6) {
                try {
                    val name = parts[0].trim().removeSurrounding("\"")
                    val sku = parts[1].trim().removeSurrounding("\"")
                    val barcode = if (parts.size > 2) parts[2].trim().removeSurrounding("\"") else ""
                    val category = if (parts.size > 3) parts[3].trim().removeSurrounding("\"") else "Umum"
                    val costPrice = parts.getOrNull(4)?.trim()?.toDoubleOrNull() ?: 0.0
                    val sellingPrice = parts.getOrNull(5)?.trim()?.toDoubleOrNull() ?: 0.0
                    val stock = parts.getOrNull(6)?.trim()?.toIntOrNull() ?: 0
                    val unit = if (parts.size > 7) parts[7].trim().removeSurrounding("\"") else "Pcs"
                    val minStock = parts.getOrNull(8)?.trim()?.toIntOrNull() ?: 5

                    if (name.isNotEmpty() && sellingPrice > 0.0) {
                        newProducts.add(
                            Product(
                                name = name,
                                sku = sku,
                                barcode = barcode,
                                category = category,
                                costPrice = costPrice,
                                sellingPrice = sellingPrice,
                                stock = stock,
                                unit = unit,
                                minStock = minStock
                            )
                        )
                        count++
                    }
                } catch (_: Exception) {}
            }
        }

        if (newProducts.isNotEmpty()) {
            database.productDao().insertAll(newProducts)
        }
        count
    }

    fun shareFile(file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
