package com.example.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import com.example.data.preferences.BusinessSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class PrinterDeviceInfo(
    val name: String,
    val address: String
)

sealed class PrintResult {
    data object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
}

class BluetoothPrinterManager(private val context: Context) {

    // Standard SPP UUID for Bluetooth Serial Printers
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<PrinterDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices.map { device ->
                PrinterDeviceInfo(
                    name = device.name ?: "Unknown Printer",
                    address = device.address
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytes(macAddress: String, data: ByteArray): PrintResult = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
            ?: return@withContext PrintResult.Error("Bluetooth tidak didukung pada perangkat ini.")

        if (!adapter.isEnabled) {
            return@withContext PrintResult.Error("Bluetooth dalam keadaan mati. Silakan aktifkan Bluetooth.")
        }

        var socket: BluetoothSocket? = null
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            adapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()

            val outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()

            // Small delay to ensure printer buffer processes before socket closure
            Thread.sleep(600)

            PrintResult.Success
        } catch (e: SecurityException) {
            PrintResult.Error("Izin Bluetooth belum diberikan: ${e.message}")
        } catch (e: IOException) {
            PrintResult.Error("Gagal menghubungkan ke printer (${e.message}). Pastikan printer menyala dan dalam jangkauan.")
        } catch (e: Exception) {
            PrintResult.Error("Terjadi kesalahan pencetakan: ${e.message}")
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    suspend fun printReceipt(
        macAddress: String,
        builder: EscPosBuilder
    ): PrintResult {
        return printBytes(macAddress, builder.build())
    }

    suspend fun testPrint(
        macAddress: String,
        settings: BusinessSettings
    ): PrintResult {
        val builder = EscPosBuilder(settings.paperSize)
        builder.init()
            .alignCenter()
            .doubleSize(true)
            .textLine(settings.businessName)
            .doubleSize(false)
            .textLine("UJI CETAK PRINTER THERMAL")
            .textLine(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
            .divider('=')
            .alignLeft()
            .doubleRow("Koneksi:", "BERHASIL")
            .doubleRow("Ukuran Kertas:", settings.paperSize)
            .doubleRow("Karakter/Baris:", "${builder.maxColumns} char")
            .divider('-')
            .alignCenter()
            .textLine("Aplikasi Kasir Profesional")
            .bold(true)
            .textLine("KASIR DUNIA")
            .bold(false)
            .lineFeed(1)
            .cutPaper()

        return printBytes(macAddress, builder.build())
    }
}
