package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "warehouse_locations",
    indices = [Index(value = ["code"], unique = true)]
)
data class WarehouseLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val code: String, // e.g., "A-01", "B-12"
    // qrUid: stable identifier stored in DB and encoded into QR codes/deep links
    // Backfill migration will populate this for existing rows
    val qrUid: String? = null,
    val name: String,
    val zone: String? = null, // e.g., "Zone A", "Receiving"
    val type: LocationType = LocationType.SHELF,
    
    val description: String? = null,
    val capacity: Int? = null,
    val currentOccupancy: Int = 0,
    
    val createdAt: Long = System.currentTimeMillis()
)

enum class LocationType {
    SHELF,
    BIN,
    PALLET,
    ROOM,
    ZONE
}
