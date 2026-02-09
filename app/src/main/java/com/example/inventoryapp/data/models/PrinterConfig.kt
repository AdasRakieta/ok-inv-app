package com.example.inventoryapp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration for printer connection and settings
 */
@Parcelize
data class PrinterConfig(
    val printerModel: PrinterModel = PrinterModel.BROTHER_PT_P950NW,
    val connectionMethod: ConnectionMethod = ConnectionMethod.WIFI,
    val ipAddress: String = "",
    val port: Int = 9100,
    val bluetoothAddress: String = "",
    val bluetoothName: String = "",
    val labelWidth: Int = 29,  // mm (tape width)
    val labelHeight: Int = 50,  // mm (label length - default: Large/Duża 5cm)
    val isConfigured: Boolean = false
) : Parcelable {

    /**
     * Available connection methods for printers
     */
    enum class ConnectionMethod {
        WIFI,
        BLUETOOTH,
        WIRELESS_DIRECT;

        fun getDisplayName(): String {
            return when (this) {
                WIFI -> "WiFi"
                BLUETOOTH -> "Bluetooth"
                WIRELESS_DIRECT -> "Wireless Direct"
            }
        }
    }

    /**
     * Validate WiFi configuration
     */
    fun isWifiConfigValid(): Boolean {
        return connectionMethod == ConnectionMethod.WIFI && 
               ipAddress.isNotBlank() && 
               isValidIpAddress(ipAddress) &&
               port in 1..65535
    }

    /**
     * Validate Bluetooth configuration
     */
    fun isBluetoothConfigValid(): Boolean {
        return connectionMethod == ConnectionMethod.BLUETOOTH && 
               bluetoothAddress.isNotBlank()
    }

    /**
     * Check if current configuration is valid
     */
    fun isValid(): Boolean {
        return when (connectionMethod) {
            ConnectionMethod.WIFI -> isWifiConfigValid()
            ConnectionMethod.BLUETOOTH -> isBluetoothConfigValid()
            ConnectionMethod.WIRELESS_DIRECT -> ipAddress.isNotBlank()
        }
    }

    companion object {
        /**
         * Validate IP address format
         */
        private fun isValidIpAddress(ip: String): Boolean {
            val ipPattern = Regex(
                "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
            )
            return ipPattern.matches(ip)
        }

        /**
         * Label sizes for Brother PT-P950NW
         * Format: (tape width, label length) in mm
         * - Small: 3cm length (NEW - compact)
         * - Medium: 4cm length (previous Small)
         * - Large: 5cm length (previous Medium) - default
         */
        val LABEL_SIZES = listOf(
            Pair(29, 30),   // Mała (3cm) - nowy kompaktowy
            Pair(29, 40),   // Średnia (4cm) - poprzednia mała
            Pair(29, 50)    // Duża (5cm) - poprzednia średnia, default
        )
        
        val LABEL_SIZE_NAMES = listOf(
            "Mała (3cm)",
            "Średnia (4cm)",
            "Duża (5cm)"
        )
    }
}
