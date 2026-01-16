package com.example.inventoryapp.data.models

data class ScanResult(
    val code: String,
    val format: BarcodeFormat,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BarcodeFormat {
    QR_CODE,
    EAN_13,
    EAN_8,
    CODE_128,
    CODE_39,
    CODE_93,
    UPC_A,
    UPC_E,
    UNKNOWN
}
