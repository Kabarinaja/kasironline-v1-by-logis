package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Category::class,
        Crew::class,
        CashierSession::class,
        CashMovement::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StockLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun crewDao(): CrewDao
    abstract fun cashierSessionDao(): CashierSessionDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun stockLogDao(): StockLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasirdunia_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                val categoryDao = database.categoryDao()
                val crewDao = database.crewDao()
                val productDao = database.productDao()

                // Default Categories
                categoryDao.insertAll(
                    listOf(
                        Category(name = "Makanan"),
                        Category(name = "Minuman"),
                        Category(name = "Camilan"),
                        Category(name = "Jasa"),
                        Category(name = "Umum")
                    )
                )

                // Default Crews from Prompt Brief
                crewDao.insertAll(
                    listOf(
                        Crew(name = "Logis Denis Prabowo", role = "Owner", phone = "0823-7947-4173"),
                        Crew(name = "Denis", role = "Karyawan", phone = "")
                    )
                )

                // Starter Products so new users immediately have functional items to test
                val sampleProducts = listOf(
                    Product(
                        name = "Kopi Susu Gula Aren",
                        sku = "KOP-001",
                        barcode = "8991001001",
                        category = "Minuman",
                        costPrice = 8000.0,
                        sellingPrice = 18000.0,
                        stock = 45,
                        unit = "Cup",
                        minStock = 10
                    ),
                    Product(
                        name = "Teh Manis Dingin",
                        sku = "TEH-002",
                        barcode = "8991001002",
                        category = "Minuman",
                        costPrice = 2000.0,
                        sellingPrice = 6000.0,
                        stock = 80,
                        unit = "Gelas",
                        minStock = 15
                    ),
                    Product(
                        name = "Nasi Goreng Spesial",
                        sku = "NAS-003",
                        barcode = "8991001003",
                        category = "Makanan",
                        costPrice = 12000.0,
                        sellingPrice = 25000.0,
                        stock = 30,
                        unit = "Porsi",
                        minStock = 5
                    ),
                    Product(
                        name = "Roti Bakar Keju Cokelat",
                        sku = "ROT-004",
                        barcode = "8991001004",
                        category = "Camilan",
                        costPrice = 7000.0,
                        sellingPrice = 15000.0,
                        stock = 25,
                        unit = "Porsi",
                        minStock = 5
                    ),
                    Product(
                        name = "Air Mineral 600ml",
                        sku = "AIR-005",
                        barcode = "8991001005",
                        category = "Minuman",
                        costPrice = 2500.0,
                        sellingPrice = 5000.0,
                        stock = 4, // Intentionally low to show low-stock indicator on first run
                        unit = "Botol",
                        minStock = 10
                    )
                )
                productDao.insertAll(sampleProducts)
            }
        }
    }
}
