package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND (barcode = :code OR sku = :code) LIMIT 1")
    suspend fun getProductByBarcodeOrSku(code: String): Product?

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND stock <= minStock ORDER BY stock ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0 AND isActive = 1")
    fun getActiveProductCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>): List<Long>

    @Update
    suspend fun update(product: Product)

    @Query("UPDATE products SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stock = stock + :qty, updatedAt = :timestamp WHERE id = :id")
    suspend fun adjustStock(id: Long, qty: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stock = :newStock, updatedAt = :timestamp WHERE id = :id")
    suspend fun setStock(id: Long, newStock: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface CrewDao {
    @Query("SELECT * FROM crews WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCrew(): Flow<List<Crew>>

    @Query("SELECT * FROM crews WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getActiveCrew(): Flow<List<Crew>>

    @Query("SELECT * FROM crews WHERE id = :id LIMIT 1")
    suspend fun getCrewById(id: Long): Crew?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crew: Crew): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crews: List<Crew>)

    @Update
    suspend fun update(crew: Crew)

    @Query("UPDATE crews SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("DELETE FROM crews")
    suspend fun clearAll()
}

@Dao
interface CashierSessionDao {
    @Query("SELECT * FROM cashier_sessions WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun getActiveSession(): Flow<CashierSession?>

    @Query("SELECT * FROM cashier_sessions WHERE status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun getActiveSessionSync(): CashierSession?

    @Query("SELECT * FROM cashier_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): CashierSession?

    @Query("SELECT * FROM cashier_sessions ORDER BY openedAt DESC")
    fun getAllSessions(): Flow<List<CashierSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: CashierSession): Long

    @Update
    suspend fun update(session: CashierSession)

    @Query("DELETE FROM cashier_sessions")
    suspend fun clearAll()
}

@Dao
interface CashMovementDao {
    @Query("SELECT * FROM cash_movements WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getMovementsForSession(sessionId: Long): Flow<List<CashMovement>>

    @Query("SELECT * FROM cash_movements WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getMovementsForSessionSync(sessionId: Long): List<CashMovement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: CashMovement): Long

    @Query("DELETE FROM cash_movements")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE invoiceNumber = :invoice LIMIT 1")
    suspend fun getTransactionByInvoice(invoice: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetweenFlow(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getTransactionsForSession(sessionId: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface TransactionItemDao {
    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsForTransactionFlow(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllItems(): List<TransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionItemEntity>)

    @Query("DELETE FROM transaction_items")
    suspend fun clearAll()
}

@Dao
interface StockLogDao {
    @Query("SELECT * FROM stock_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<StockLog>>

    @Query("SELECT * FROM stock_logs WHERE productId = :productId ORDER BY timestamp DESC")
    fun getLogsForProduct(productId: Long): Flow<List<StockLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StockLog): Long

    @Query("DELETE FROM stock_logs")
    suspend fun clearAll()
}
