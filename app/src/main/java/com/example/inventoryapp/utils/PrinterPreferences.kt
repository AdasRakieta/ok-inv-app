package com.example.inventoryapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.inventoryapp.data.models.PrinterConfig
import com.example.inventoryapp.data.models.PrinterModel

/**
 * Helper class for saving and loading printer configuration
 */
class PrinterPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Save printer configuration
     */
    fun savePrinterConfig(config: PrinterConfig) {
        prefs.edit().apply {
            putString(KEY_PRINTER_MODEL, config.printerModel.name)
            putString(KEY_CONNECTION_METHOD, config.connectionMethod.name)
            putString(KEY_IP_ADDRESS, config.ipAddress)
            putInt(KEY_PORT, config.port)
            putString(KEY_BLUETOOTH_ADDRESS, config.bluetoothAddress)
            putString(KEY_BLUETOOTH_NAME, config.bluetoothName)
            putInt(KEY_LABEL_WIDTH, config.labelWidth)
            putInt(KEY_LABEL_HEIGHT, config.labelHeight)
            putBoolean(KEY_IS_CONFIGURED, config.isConfigured)
            apply()
        }
    }

    /**
     * Load printer configuration
     */
    fun loadPrinterConfig(): PrinterConfig {
        val printerModelName = prefs.getString(KEY_PRINTER_MODEL, null)
        val printerModel = if (printerModelName != null) {
            PrinterModel.fromString(printerModelName)
        } else {
            PrinterModel.BROTHER_PT_P950NW
        }

        val connectionMethodName = prefs.getString(
            KEY_CONNECTION_METHOD,
            PrinterConfig.ConnectionMethod.WIFI.name
        )
        val connectionMethod = try {
            PrinterConfig.ConnectionMethod.valueOf(connectionMethodName ?: "WIFI")
        } catch (e: IllegalArgumentException) {
            PrinterConfig.ConnectionMethod.WIFI
        }

        return PrinterConfig(
            printerModel = printerModel,
            connectionMethod = connectionMethod,
            ipAddress = prefs.getString(KEY_IP_ADDRESS, "") ?: "",
            port = prefs.getInt(KEY_PORT, 9100),
            bluetoothAddress = prefs.getString(KEY_BLUETOOTH_ADDRESS, "") ?: "",
            bluetoothName = prefs.getString(KEY_BLUETOOTH_NAME, "") ?: "",
            labelWidth = prefs.getInt(KEY_LABEL_WIDTH, 29),
            labelHeight = prefs.getInt(KEY_LABEL_HEIGHT, 90),
            isConfigured = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        )
    }

    /**
     * Clear printer configuration
     */
    fun clearPrinterConfig() {
        prefs.edit().clear().apply()
    }

    /**
     * Check if printer is configured
     */
    fun isPrinterConfigured(): Boolean {
        return prefs.getBoolean(KEY_IS_CONFIGURED, false)
    }

    /**
     * Save last connection status
     */
    fun saveLastConnectionStatus(success: Boolean, message: String = "") {
        prefs.edit().apply {
            putBoolean(KEY_LAST_CONNECTION_SUCCESS, success)
            putString(KEY_LAST_CONNECTION_MESSAGE, message)
            putLong(KEY_LAST_CONNECTION_TIME, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Get last connection status
     */
    fun getLastConnectionStatus(): Triple<Boolean, String, Long> {
        val success = prefs.getBoolean(KEY_LAST_CONNECTION_SUCCESS, false)
        val message = prefs.getString(KEY_LAST_CONNECTION_MESSAGE, "") ?: ""
        val time = prefs.getLong(KEY_LAST_CONNECTION_TIME, 0L)
        return Triple(success, message, time)
    }

    companion object {
        private const val PREF_NAME = "printer_preferences"
        
        private const val KEY_PRINTER_MODEL = "printer_model"
        private const val KEY_CONNECTION_METHOD = "connection_method"
        private const val KEY_IP_ADDRESS = "ip_address"
        private const val KEY_PORT = "port"
        private const val KEY_BLUETOOTH_ADDRESS = "bluetooth_address"
        private const val KEY_BLUETOOTH_NAME = "bluetooth_name"
        private const val KEY_LABEL_WIDTH = "label_width"
        private const val KEY_LABEL_HEIGHT = "label_height"
        private const val KEY_IS_CONFIGURED = "is_configured"
        
        private const val KEY_LAST_CONNECTION_SUCCESS = "last_connection_success"
        private const val KEY_LAST_CONNECTION_MESSAGE = "last_connection_message"
        private const val KEY_LAST_CONNECTION_TIME = "last_connection_time"
    }
}
