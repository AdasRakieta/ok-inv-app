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
    val labelWidth: Int = 29,  // mm
    val labelHeight: Int = 90,  // mm
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
         * Default label sizes for Brother PT-P950NW (width x height in mm)
         */
        val LABEL_SIZES = listOf(
            Pair(29, 90),   // Standard
            Pair(25, 50),   // Small
            Pair(36, 110),  // Large
            Pair(24, 24)    // Square
        )
    }
}
