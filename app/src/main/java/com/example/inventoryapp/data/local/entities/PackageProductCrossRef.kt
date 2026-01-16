package com.example.inventoryapp.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "package_product_cross_ref",
    primaryKeys = ["packageId", "productId"]
)
data class PackageProductCrossRef(
    val packageId: Long,
    val productId: Long
)
