package com.example.inventoryapp.data.models

/**
 * Supported printer models with specific connection requirements
 */
enum class PrinterModel(
    val displayName: String,
    val manufacturer: String,
    val connectionType: ConnectionType,
    val requiresSecureConnection: Boolean,
    val supportsBLE: Boolean
) {
    ZQ310_PLUS(
        displayName = "ZQ310 Plus",
        manufacturer = "Zebra",
        connectionType = ConnectionType.SPP,
        requiresSecureConnection = false,
        supportsBLE = false
    ),
    ZD421(
        displayName = "ZD421",
        manufacturer = "Zebra",
        connectionType = ConnectionType.SPP_OR_BLE,
        requiresSecureConnection = true,
        supportsBLE = true
    ),
    ZD621(
        displayName = "ZD621",
        manufacturer = "Zebra",
        connectionType = ConnectionType.SPP_OR_BLE,
        requiresSecureConnection = true,
        supportsBLE = true
    ),
    GENERIC_ESC_POS(
        displayName = "Generic ESC/POS",
        manufacturer = "Generic",
        connectionType = ConnectionType.SPP,
        requiresSecureConnection = false,
        supportsBLE = false
    ),
    OTHER_ZEBRA(
        displayName = "Other Zebra",
        manufacturer = "Zebra",
        connectionType = ConnectionType.SPP,
        requiresSecureConnection = false,
        supportsBLE = false
    );

    enum class ConnectionType {
        SPP,           // Serial Port Profile (classic Bluetooth)
        BLE,           // Bluetooth Low Energy
        SPP_OR_BLE     // Supports both
    }

    companion object {
        /**
         * Get printer model by name (case-insensitive)
         */
        fun fromString(name: String?): PrinterModel {
            return values().find { 
                it.name.equals(name, ignoreCase = true) 
            } ?: GENERIC_ESC_POS
        }

        /**
         * Get all Zebra printer models
         */
        fun getZebraModels(): List<PrinterModel> {
            return values().filter { it.manufacturer == "Zebra" }
        }

        /**
         * Get display names for UI dropdown
         */
        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}
