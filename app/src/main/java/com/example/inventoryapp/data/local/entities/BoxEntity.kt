package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "boxes",
    indices = [Index(value = ["qrUid"], unique = true), Index(value = ["warehouseLocationId"])]
)
data class BoxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val code: String? = null,
    val qrUid: String? = null,
    val warehouseLocationId: Long? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
