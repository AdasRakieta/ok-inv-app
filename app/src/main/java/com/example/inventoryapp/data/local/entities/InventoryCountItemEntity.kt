package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a scanned product in an inventory count session.
 * Each scan is recorded as a separate item (allows multiple scans of same product).
 */
@Entity(
    tableName = "inventory_count_items",
    foreignKeys = [
        ForeignKey(
            entity = InventoryCountSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("productId")
    ]
)
data class InventoryCountItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** ID of the inventory count session */
    val sessionId: Long,
    
    /** ID of the scanned product */
    val productId: Long,
    
    /** Timestamp when product was scanned */
    val scannedAt: Long = System.currentTimeMillis(),
    
    /** Sequence number of this scan in the session (1, 2, 3, ...) */
    val sequenceNumber: Int
)
