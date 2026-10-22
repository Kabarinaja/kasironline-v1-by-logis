package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"]), Index(value = ["barcode"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val category: String = "Umum",
    val costPrice: Double = 0.0,
    val sellingPrice: Double,
    val stock: Int = 0,
    val unit: String = "Pcs",
    val minStock: Int = 5,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "crews")
data class Crew(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String = "Kasir", // Owner, Kasir, Karyawan
    val phone: String = "",
    val isActive: Boolean = true,
    val notes: String = "",
    val isDeleted: Boolean = false
)

@Entity(tableName = "cashier_sessions")
data class CashierSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val crewId: Long,
    val cashierName: String,
    val cashierRole: String,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val openingCash: Double = 0.0,
    val closingCashActual: Double? = null,
    val closingCashExpected: Double? = null,
    val cashSalesTotal: Double = 0.0,
    val qrisSalesTotal: Double = 0.0,
    val cashInTotal: Double = 0.0,
    val cashOutTotal: Double = 0.0,
    val status: String = "OPEN", // OPEN, CLOSED
    val openingNotes: String = "",
    val closingNotes: String = ""
)

@Entity(
    tableName = "cash_movements",
    indices = [Index(value = ["sessionId"])]
)
data class CashMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val type: String, // CASH_IN, CASH_OUT
    val amount: Double,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["invoiceNumber"], unique = true), Index(value = ["sessionId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val sessionId: Long? = null,
    val cashierName: String,
    val cashierRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val discountNote: String = "",
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String, // TUNAI, QRIS
    val cashPaid: Double = 0.0,
    val changeReturned: Double = 0.0,
    val qrisReference: String = "",
    val notes: String = "",
    val status: String = "COMPLETED", // COMPLETED, CANCELLED
    val cancelledAt: Long? = null,
    val cancelReason: String? = null
)

@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long? = null,
    val productName: String,
    val sku: String = "",
    val sellingPrice: Double,
    val costPrice: Double = 0.0,
    val quantity: Int,
    val discountPerItem: Double = 0.0,
    val subtotal: Double
)

@Entity(
    tableName = "stock_logs",
    indices = [Index(value = ["productId"])]
)
data class StockLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: String, // IN, OUT, ADJUSTMENT, SALE, SALE_CANCEL
    val changeQty: Int,
    val remainingStock: Int,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
