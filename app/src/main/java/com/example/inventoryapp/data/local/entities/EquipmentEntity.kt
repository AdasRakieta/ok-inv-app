package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "equipment",
    indices = [Index(value = ["serialNumber"], unique = true)]
)
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val serialNumber: String?,
    val category: String?,
    val status: String = "AVAILABLE", // AVAILABLE, ASSIGNED, REPAIR, RETIRED
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
