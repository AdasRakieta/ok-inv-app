package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val description: String? = null,
    val color: String? = null, // Hex color for UI
    val icon: String? = null, // Icon name or emoji
    
    val createdAt: Long = System.currentTimeMillis()
)
