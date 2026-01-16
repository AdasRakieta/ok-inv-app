package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageCode: String? = null, // Unique code from Google Sheets (Kod field) - used for deduplication
    val contractorId: Long? = null, // Optional contractor assignment
    val status: String = "PREPARATION", // Issued, Returned, Preparation, Ready, Warehouse
    val createdAt: Long = System.currentTimeMillis(),
    val shippedAt: Long? = null,
    val deliveredAt: Long? = null,
    val returnedAt: Long? = null, // Date when package was returned
    val archived: Boolean = false // Whether package is archived (only RETURNED packages can be archived)
)
