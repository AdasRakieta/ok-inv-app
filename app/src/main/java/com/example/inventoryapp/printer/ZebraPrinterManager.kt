package com.example.inventoryapp.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.zebra.sdk.comm.BluetoothConnectionInsecure
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLinkOs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset

class ZebraPrinterManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    /**
     * Check if Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) { // Android 12 (API 31)
            ActivityCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_SCAN") == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Get paired Bluetooth devices
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Print ZPL content to Zebra printer via Bluetooth
     * @param macAddress MAC address of the printer
     * @param zplContent ZPL formatted content to print
     * @param labelLength Length of the label in dots (optional, defaults to 0 for auto)
     * @return null if successful, error message if failed
     */
    suspend fun printDocument(
        macAddress: String,
        zplContent: String,
        labelLength: Int = 0
    ): String? = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            // Create Bluetooth connection
            connection = BluetoothConnectionInsecure(macAddress)

            // Open connection
            connection.open()

            // Get LinkOs printer instance (used by Flutter plugin)
            val linkOsPrinter: ZebraPrinterLinkOs = ZebraPrinterFactory.getLinkOsPrinter(connection)

            // Set label length if specified
            if (labelLength > 0) {
                linkOsPrinter.setSetting("zpl.label_length", labelLength.toString())
            }

            // Send ZPL content with windows-1250 encoding (for Polish characters)
            val charset = Charset.forName("windows-1250")
            connection.write(zplContent.toByteArray(charset))

            // Wait for data to be sent to printer (as in Flutter plugin)
            Thread.sleep(1000)

            // Success
            null

        } catch (e: Exception) {
            when (e) {
                is IOException -> "Connection error: ${e.message}"
                is IllegalArgumentException -> "Invalid printer address or ZPL content: ${e.message}"
                else -> "Print error: ${e.message}"
            }
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    /**
     * Test connection to printer
     * @param macAddress MAC address of the printer
     * @return null if successful, error message if failed
     */
    suspend fun testConnection(macAddress: String): String? = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            connection = BluetoothConnectionInsecure(macAddress)
            connection.open()

            // Try to get LinkOs printer and check status
            val linkOsPrinter: ZebraPrinterLinkOs = ZebraPrinterFactory.getLinkOsPrinter(connection)
            val status = linkOsPrinter.currentStatus

            if (status.isReadyToPrint) {
                null // Success
            } else {
                "Printer not ready: ${status}"
            }

        } catch (e: Exception) {
            "Connection test failed: ${e.message}"
        } finally {
            try {
                connection?.close()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}