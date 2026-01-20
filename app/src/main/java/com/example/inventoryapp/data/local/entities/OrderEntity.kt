package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["supplierId"]),
        Index(value = ["status"])
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val orderNumber: String,
    val supplierId: Long? = null,
    
    val status: OrderStatus = OrderStatus.PENDING,
    val orderDate: Long = System.currentTimeMillis(),
    val expectedDeliveryDate: Long? = null,
    val actualDeliveryDate: Long? = null,
    
    val totalAmount: Double? = null,
    val currency: String = "PLN",
    
    val notes: String? = null,
    val trackingNumber: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
