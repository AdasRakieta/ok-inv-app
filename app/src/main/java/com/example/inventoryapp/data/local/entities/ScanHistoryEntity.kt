package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "scan_history",
    indices = [
        Index(value = ["scannedValue"]),
        Index(value = ["timestamp"])
    ]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val scannedValue: String,
    val scanType: ScanType = ScanType.BARCODE,
    val context: String? = null, // e.g., "bulk_add", "product_details", "inventory_count"
    
    val productId: Long? = null,
    val employeeId: Long? = null,
    
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val errorMessage: String? = null
)

enum class ScanType {
    BARCODE,
    QR_CODE,
    SERIAL_NUMBER
}
