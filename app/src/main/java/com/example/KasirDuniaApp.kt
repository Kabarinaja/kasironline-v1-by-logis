package com.example

import android.app.Application
import com.example.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PosRepository
import com.example.pdf.PdfReportGenerator
import com.example.printer.BluetoothPrinterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KasirDuniaApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val preferences by lazy { UserPreferences(this) }
    val repository by lazy { PosRepository(database) }
    val printerManager by lazy { BluetoothPrinterManager(this) }
    val pdfReportGenerator by lazy { PdfReportGenerator(this) }
    val backupManager by lazy { BackupManager(this, database, preferences) }

    override fun onCreate() {
        super.onCreate()
    }
}
