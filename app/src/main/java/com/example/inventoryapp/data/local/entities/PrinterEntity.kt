package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a Bluetooth printer configuration
 * Label dimensions are stored in millimeters
 */
@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val macAddress: String,
    
    // Printer model for connection strategy
    val model: String = "GENERIC_ESC_POS", // PrinterModel enum name
    
    // Label dimensions in millimeters
    val labelWidthMm: Int = 50, // Default 50mm width
    val labelHeightMm: Int? = null, // null = continuous roll (auto-height)
    val dpi: Int = 203, // Printer DPI (203 or 300)
    val fontSize: String = "small", // Font size: "small", "medium", "large"
    
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
