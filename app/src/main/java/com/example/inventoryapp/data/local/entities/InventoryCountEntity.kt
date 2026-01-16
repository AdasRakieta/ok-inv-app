package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "inventory_counts",
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index(value = ["status"])
    ]
)
data class InventoryCountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sessionId: String, // Unique session identifier
    val name: String,
    val description: String? = null,
    
    val status: CountStatus = CountStatus.IN_PROGRESS,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    
    val countedById: Long? = null, // Employee who performed count
    
    val totalExpectedItems: Int = 0,
    val totalCountedItems: Int = 0,
    val totalMissingItems: Int = 0,
    val totalExtraItems: Int = 0,
    
    val notes: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class CountStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
