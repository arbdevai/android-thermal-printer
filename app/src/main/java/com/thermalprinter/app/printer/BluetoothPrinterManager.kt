package com.thermalprinter.app.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

sealed class PrinterConnectionStatus {
    data object Disconnected : PrinterConnectionStatus()
    data class Connecting(val deviceName: String) : PrinterConnectionStatus()
    data class Connected(val deviceName: String, val address: String) : PrinterConnectionStatus()
    data class Error(val message: String) : PrinterConnectionStatus()
}

class BluetoothPrinterManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val _connectionStatus = MutableStateFlow<PrinterConnectionStatus>(PrinterConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<PrinterConnectionStatus> = _connectionStatus.asStateFlow()

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        disconnect()
        val adapter = bluetoothAdapter ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia di perangkat ini"))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth belum aktif. Silakan aktifkan Bluetooth."))
        }

        try {
            val device = adapter.getRemoteDevice(address)
            val devName = device.name ?: address
            _connectionStatus.value = PrinterConnectionStatus.Connecting(devName)

            // Cancel discovery to improve connection stability and speed
            try {
                adapter.cancelDiscovery()
            } catch (_: Exception) {}

            val sock = device.createRfcommSocketToServiceRecord(sppUuid)
            sock.connect()
            socket = sock
            outputStream = sock.outputStream
            _connectionStatus.value = PrinterConnectionStatus.Connected(devName, address)
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            _connectionStatus.value = PrinterConnectionStatus.Error(e.message ?: "Gagal terhubung ke printer")
            Result.failure(e)
        }
    }

    suspend fun printBytes(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val out = outputStream
        if (out == null || socket?.isConnected != true) {
            return@withContext Result.failure(Exception("Printer belum terhubung via Bluetooth"))
        }
        try {
            out.write(data)
            out.flush()
            Result.success(Unit)
        } catch (e: IOException) {
            disconnect()
            _connectionStatus.value = PrinterConnectionStatus.Error("Koneksi terputus saat mencetak: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun printTestReceipt(storeName: String): Result<Unit> {
        val driver = EscPosDriver(lineChars = 32).init()
        driver.alignCenter()
        driver.bold(true).doubleHeight(true).line(storeName.ifBlank { "THERMAL PRINTER" })
        driver.bold(false).doubleHeight(false)
        driver.line("Uji Coba Printer Bluetooth 58mm")
        driver.doubleDivider()
        driver.alignLeft()
        driver.twoColumn("Status", "BERHASIL KONEK")
        driver.twoColumn("Tipe", "ESC/POS 58mm")
        driver.twoColumn("Karakter/Baris", "32 Karakter")
        driver.divider('-')
        driver.alignCenter()
        driver.line("Printer siap digunakan untuk mencetak bukti transaksi!")
        driver.feed(3)
        driver.cut()
        return printBytes(driver.build())
    }

    suspend fun feedPaper(lines: Int = 3): Result<Unit> {
        val driver = EscPosDriver().feed(lines)
        return printBytes(driver.build())
    }

    suspend fun cutPaper(): Result<Unit> {
        val driver = EscPosDriver().cut()
        return printBytes(driver.build())
    }

    fun disconnect() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        outputStream = null
        socket = null
        _connectionStatus.value = PrinterConnectionStatus.Disconnected
    }
}
