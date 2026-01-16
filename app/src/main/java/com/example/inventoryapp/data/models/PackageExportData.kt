package com.example.inventoryapp.data.models

import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.local.entities.ProductEntity

/**
 * Data transfer object for package export via QR code.
 * Contains package info and all products in the package.
 */
data class PackageExportData(
    val packageInfo: PackageEntity,
    val products: List<ProductEntity>,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "" // Optional device identifier
)

/**
 * Result of package import operation
 */
data class PackageImportResult(
    val success: Boolean,
    val productsAdded: Int = 0,
    val productsUpdated: Int = 0, // Products with matching SNs that were overwritten
    val errors: List<String> = emptyList()
)
