package com.example.inventoryapp.data.models

data class Package(
    val id: Long = 0,
    val name: String,
    val status: PackageStatus = PackageStatus.PREPARATION,
    val createdAt: Long = System.currentTimeMillis(),
    val shippedAt: Long? = null,
    val deliveredAt: Long? = null
)

enum class PackageStatus {
    PREPARATION,
    READY,
    SHIPPED,
    DELIVERED
}
