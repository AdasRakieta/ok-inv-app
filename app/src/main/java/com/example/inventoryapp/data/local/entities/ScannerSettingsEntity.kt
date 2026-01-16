package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Scanner settings - stores scanner ID for this device
 */
@Entity(tableName = "scanner_settings")
data class ScannerSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Single row - always id=1
    val scannerId: String, // Scanner identifier (user-entered digits)
    val updatedAt: Long = System.currentTimeMillis()
)
