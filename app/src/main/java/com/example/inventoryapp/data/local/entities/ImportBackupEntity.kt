package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store import backups for undo functionality
 * Stores snapshot of data before import operation
 */
@Entity(tableName = "import_backups")
data class ImportBackupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val backupTimestamp: Long = System.currentTimeMillis(),
    val backupJson: String, // JSON snapshot of all data before import
    val importDescription: String, // Description of what was imported
    val productsCount: Int = 0,
    val packagesCount: Int = 0,
    val templatesCount: Int = 0
)
