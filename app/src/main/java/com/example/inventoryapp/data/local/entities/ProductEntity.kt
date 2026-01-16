package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["serialNumber"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val serialNumber: String?, // Nullable in DB, but required in UI validation
    val description: String? = null,
    val imageUri: String? = null,
    val quantity: Int = 1, // Quantity for aggregated products (especially "Other" category)
    val deviceId: String? = null, // Fixed device ID from Google Sheets column J
    val configValue: Int? = null, // Config value from Google Sheets (0-10 range)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Data class that holds a Product and its Package information
 * Used for displaying missing products with package assignment status
 */
data class ProductWithPackageInfo(
    val product: ProductEntity,
    val packageInfo: PackageEntity? = null
)
