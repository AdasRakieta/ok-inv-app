package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an inventory count session.
 * Used for periodic stock-taking and verification of products in the database.
 */
@Entity(tableName = "inventory_count_sessions")
data class InventoryCountSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Name of the inventory count session (e.g., "Q1 2024 Inventory") */
    val name: String,
    
    /** Timestamp when session was created */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Timestamp when session was completed (null if still in progress) */
    val completedAt: Long? = null,
    
    /** Session status: "IN_PROGRESS" or "COMPLETED" */
    val status: String = "IN_PROGRESS",
    
    /** Optional notes about the inventory count */
    val notes: String? = null
)
