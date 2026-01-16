package com.example.inventoryapp.data.models

data class Product(
    val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val serialNumber: String? = null,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
