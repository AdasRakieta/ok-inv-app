package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconResId: Int = 0,
    val requiresSerialNumber: Boolean = true, // false for "Other" category
    val createdAt: Long = System.currentTimeMillis()
)
