package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_movements")
data class DeviceMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val action: String,
    val fromContainerType: String? = null,
    val fromContainerId: Long? = null,
    val toContainerType: String? = null,
    val toContainerId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val packageStatus: String? = null,
    val note: String? = null
)
