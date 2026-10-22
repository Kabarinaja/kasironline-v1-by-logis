package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class TransactionDetail(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>
)

data class SalesSummary(
    val totalSales: Double,
    val totalTransactions: Int,
    val cashSales: Double,
    val qrisSales: Double,
    val totalDiscount: Double,
    val totalCancelledSales: Double,
    val cancelledCount: Int,
    val estimatedProfit: Double,
    val topSellingByQty: List<TopProductSummary>,
    val topSellingByRevenue: List<TopProductSummary>,
    val salesByCashier: Map<String, Double>
)

data class TopProductSummary(
    val productName: String,
    val sku: String,
    val totalQty: Int,
    val totalRevenue: Double
)

class PosRepository(private val database: AppDatabase) {

    private val productDao = database.productDao()
    private val categoryDao = database.categoryDao()
    private val crewDao = database.crewDao()
    private val cashierSessionDao = database.cashierSessionDao()
    private val cashMovementDao = database.cashMovementDao()
    private val transactionDao = database.transactionDao()
    private val transactionItemDao = database.transactionItemDao()
    private val stockLogDao = database.stockLogDao()

    // --- PRODUCTS ---
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val activeProducts: Flow<List<Product>> = productDao.getActiveProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val activeProductCount: Flow<Int> = productDao.getActiveProductCount()

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)

    suspend fun findProductByBarcodeOrSku(code: String): Product? =
        productDao.getProductByBarcodeOrSku(code.trim())

    suspend fun saveProduct(product: Product): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            val id = productDao.insert(product)
            stockLogDao.insert(
                StockLog(
                    productId = id,
                    productName = product.name,
                    type = "IN",
                    changeQty = product.stock,
                    remainingStock = product.stock,
                    reason = "Stok Awal Produk Baru"
                )
            )
            id
        } else {
            val existing = productDao.getProductById(product.id)
            productDao.update(product)
            if (existing != null && existing.stock != product.stock) {
                val diff = product.stock - existing.stock
                stockLogDao.insert(
                    StockLog(
                        productId = product.id,
                        productName = product.name,
                        type = if (diff > 0) "IN" else "OUT",
                        changeQty = diff,
                        remainingStock = product.stock,
                        reason = "Pembaruan Data Produk"
                    )
                )
            }
            product.id
        }
    }

    suspend fun softDeleteProduct(id: Long) = withContext(Dispatchers.IO) {
        productDao.softDelete(id)
    }

    suspend fun adjustStock(productId: Long, qtyChange: Int, reason: String, type: String) =
        withContext(Dispatchers.IO) {
            val product = productDao.getProductById(productId) ?: return@withContext
            val newStock = (product.stock + qtyChange).coerceAtLeast(0)
            productDao.setStock(productId, newStock)
            stockLogDao.insert(
                StockLog(
                    productId = productId,
                    productName = product.name,
                    type = type,
                    changeQty = qtyChange,
                    remainingStock = newStock,
                    reason = reason
                )
            )
        }

    val allStockLogs: Flow<List<StockLog>> = stockLogDao.getAllLogs()

    // --- CATEGORIES ---
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun saveCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.insert(category)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.delete(category)
    }

    // --- CREW ---
    val allCrew: Flow<List<Crew>> = crewDao.getAllCrew()
    val activeCrew: Flow<List<Crew>> = crewDao.getActiveCrew()

    suspend fun saveCrew(crew: Crew): Long = withContext(Dispatchers.IO) {
        if (crew.id == 0L) crewDao.insert(crew) else {
            crewDao.update(crew)
            crew.id
        }
    }

    suspend fun softDeleteCrew(id: Long) = withContext(Dispatchers.IO) {
        crewDao.softDelete(id)
    }

    // --- CASHIER SESSION ---
    val activeSession: Flow<CashierSession?> = cashierSessionDao.getActiveSession()
    val allSessions: Flow<List<CashierSession>> = cashierSessionDao.getAllSessions()

    suspend fun openSession(
        crew: Crew,
        openingCash: Double,
        notes: String
    ): Long = withContext(Dispatchers.IO) {
        val session = CashierSession(
            crewId = crew.id,
            cashierName = crew.name,
            cashierRole = crew.role,
            openingCash = openingCash,
            openingNotes = notes
        )
        cashierSessionDao.insert(session)
    }

    suspend fun recordCashMovement(
        sessionId: Long,
        type: String,
        amount: Double,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val session = cashierSessionDao.getSessionById(sessionId) ?: return@withContext
        cashMovementDao.insert(
            CashMovement(
                sessionId = sessionId,
                type = type,
                amount = amount,
                reason = reason
            )
        )
        val updatedSession = if (type == "CASH_IN") {
            session.copy(cashInTotal = session.cashInTotal + amount)
        } else {
            session.copy(cashOutTotal = session.cashOutTotal + amount)
        }
        cashierSessionDao.update(updatedSession)
    }

    fun getMovementsForSession(sessionId: Long): Flow<List<CashMovement>> =
        cashMovementDao.getMovementsForSession(sessionId)

    suspend fun closeSession(
        sessionId: Long,
        actualCash: Double,
        closingNotes: String
    ) = withContext(Dispatchers.IO) {
        val session = cashierSessionDao.getSessionById(sessionId) ?: return@withContext
        // Formula per prompt: modal awal + penjualan tunai + kas masuk - kas keluar
        val expected = session.openingCash + session.cashSalesTotal + session.cashInTotal - session.cashOutTotal
        val closedSession = session.copy(
            status = "CLOSED",
            closedAt = System.currentTimeMillis(),
            closingCashActual = actualCash,
            closingCashExpected = expected,
            closingNotes = closingNotes
        )
        cashierSessionDao.update(closedSession)
    }

    // --- TRANSACTIONS ---
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(10)

    suspend fun getTransactionDetail(transactionId: Long): TransactionDetail? = withContext(Dispatchers.IO) {
        val tx = transactionDao.getTransactionById(transactionId) ?: return@withContext null
        val items = transactionItemDao.getItemsForTransaction(transactionId)
        TransactionDetail(tx, items)
    }

    suspend fun generateInvoiceNumber(): String = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val randomSuffix = (1000..9999).random()
        "KD-$dateStr-$randomSuffix"
    }

    suspend fun completeTransaction(
        items: List<TransactionItemEntity>,
        subtotal: Double,
        discountAmount: Double,
        discountNote: String,
        taxRate: Double,
        taxAmount: Double,
        totalAmount: Double,
        paymentMethod: String,
        cashPaid: Double,
        changeReturned: Double,
        qrisReference: String,
        notes: String,
        cashierName: String,
        cashierRole: String
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val activeSession = cashierSessionDao.getActiveSessionSync()
            val invoiceNumber = generateInvoiceNumber()

            val txEntity = TransactionEntity(
                invoiceNumber = invoiceNumber,
                sessionId = activeSession?.id,
                cashierName = cashierName,
                cashierRole = cashierRole,
                timestamp = System.currentTimeMillis(),
                subtotal = subtotal,
                discountAmount = discountAmount,
                discountNote = discountNote,
                taxRate = taxRate,
                taxAmount = taxAmount,
                totalAmount = totalAmount,
                paymentMethod = paymentMethod,
                cashPaid = cashPaid,
                changeReturned = changeReturned,
                qrisReference = qrisReference,
                notes = notes,
                status = "COMPLETED"
            )

            val txId = transactionDao.insert(txEntity)

            val preparedItems = items.map { item ->
                item.copy(transactionId = txId)
            }
            transactionItemDao.insertAll(preparedItems)

            // Deduct stock atomically and log
            for (item in preparedItems) {
                if (item.productId != null) {
                    val product = productDao.getProductById(item.productId)
                    if (product != null) {
                        val newStock = (product.stock - item.quantity).coerceAtLeast(0)
                        productDao.setStock(product.id, newStock)
                        stockLogDao.insert(
                            StockLog(
                                productId = product.id,
                                productName = product.name,
                                type = "SALE",
                                changeQty = -item.quantity,
                                remainingStock = newStock,
                                reason = "Transaksi #$invoiceNumber"
                            )
                        )
                    }
                }
            }

            // Update session sales if session is active
            if (activeSession != null) {
                val updatedSession = if (paymentMethod == "TUNAI") {
                    activeSession.copy(cashSalesTotal = activeSession.cashSalesTotal + totalAmount)
                } else {
                    activeSession.copy(qrisSalesTotal = activeSession.qrisSalesTotal + totalAmount)
                }
                cashierSessionDao.update(updatedSession)
            }

            txId
        }
    }

    suspend fun cancelTransaction(transactionId: Long, reason: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId) ?: return@withTransaction
            if (tx.status == "CANCELLED") return@withTransaction

            // Mark transaction as cancelled
            val cancelledTx = tx.copy(
                status = "CANCELLED",
                cancelledAt = System.currentTimeMillis(),
                cancelReason = reason
            )
            transactionDao.update(cancelledTx)

            // Restore stock
            val items = transactionItemDao.getItemsForTransaction(transactionId)
            for (item in items) {
                if (item.productId != null) {
                    val product = productDao.getProductById(item.productId)
                    if (product != null) {
                        val restoredStock = product.stock + item.quantity
                        productDao.setStock(product.id, restoredStock)
                        stockLogDao.insert(
                            StockLog(
                                productId = product.id,
                                productName = product.name,
                                type = "SALE_CANCEL",
                                changeQty = item.quantity,
                                remainingStock = restoredStock,
                                reason = "Pembatalan #${tx.invoiceNumber} ($reason)"
                            )
                        )
                    }
                }
            }

            // Adjust session if transaction belonged to an active session
            if (tx.sessionId != null) {
                val session = cashierSessionDao.getSessionById(tx.sessionId)
                if (session != null) {
                    val updatedSession = if (tx.paymentMethod == "TUNAI") {
                        session.copy(cashSalesTotal = (session.cashSalesTotal - tx.totalAmount).coerceAtLeast(0.0))
                    } else {
                        session.copy(qrisSalesTotal = (session.qrisSalesTotal - tx.totalAmount).coerceAtLeast(0.0))
                    }
                    cashierSessionDao.update(updatedSession)
                }
            }
        }
    }

    // --- SALES REPORTS & SUMMARY ---
    suspend fun getSalesSummary(startTime: Long, endTime: Long): SalesSummary = withContext(Dispatchers.IO) {
        val txs = transactionDao.getTransactionsBetween(startTime, endTime)
        val completedTxs = txs.filter { it.status == "COMPLETED" }
        val cancelledTxs = txs.filter { it.status == "CANCELLED" }

        val totalSales = completedTxs.sumOf { it.totalAmount }
        val cashSales = completedTxs.filter { it.paymentMethod == "TUNAI" }.sumOf { it.totalAmount }
        val qrisSales = completedTxs.filter { it.paymentMethod == "QRIS" }.sumOf { it.totalAmount }
        val totalDiscount = completedTxs.sumOf { it.discountAmount }
        val totalCancelledSales = cancelledTxs.sumOf { it.totalAmount }

        // Fetch items for completed transactions to calculate top products and profit
        val allItems = mutableListOf<TransactionItemEntity>()
        for (tx in completedTxs) {
            allItems.addAll(transactionItemDao.getItemsForTransaction(tx.id))
        }

        var estimatedProfit = 0.0
        val productMap = mutableMapOf<String, Pair<Int, Double>>() // key: name, value: (qty, revenue)

        for (item in allItems) {
            val itemProfit = (item.sellingPrice - item.costPrice) * item.quantity - item.discountPerItem
            estimatedProfit += itemProfit

            val current = productMap[item.productName] ?: Pair(0, 0.0)
            productMap[item.productName] = Pair(
                current.first + item.quantity,
                current.second + item.subtotal
            )
        }

        val topByQty = productMap.entries
            .sortedByDescending { it.value.first }
            .take(10)
            .map { TopProductSummary(it.key, "", it.value.first, it.value.second) }

        val topByRevenue = productMap.entries
            .sortedByDescending { it.value.second }
            .take(10)
            .map { TopProductSummary(it.key, "", it.value.first, it.value.second) }

        val cashierMap = completedTxs.groupBy { it.cashierName }
            .mapValues { entry -> entry.value.sumOf { it.totalAmount } }

        SalesSummary(
            totalSales = totalSales,
            totalTransactions = completedTxs.size,
            cashSales = cashSales,
            qrisSales = qrisSales,
            totalDiscount = totalDiscount,
            totalCancelledSales = totalCancelledSales,
            cancelledCount = cancelledTxs.size,
            estimatedProfit = estimatedProfit,
            topSellingByQty = topByQty,
            topSellingByRevenue = topByRevenue,
            salesByCashier = cashierMap
        )
    }

    // --- BACKUP / RESTORE DUMP ---
    suspend fun getAllDataForBackup(): Map<String, Any> = withContext(Dispatchers.IO) {
        mapOf(
            "version" to 1,
            "app" to "KASIR_DUNIA",
            "exportedAt" to System.currentTimeMillis()
        )
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            productDao.clearAll()
            categoryDao.clearAll()
            crewDao.clearAll()
            cashierSessionDao.clearAll()
            cashMovementDao.clearAll()
            transactionDao.clearAll()
            transactionItemDao.clearAll()
            stockLogDao.clearAll()
        }
    }
}
