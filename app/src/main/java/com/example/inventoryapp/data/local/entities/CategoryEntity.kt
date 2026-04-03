package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DestinationType {
    OFFICE,
    CONTRACTOR
}

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true), Index(value = ["parentId"]) ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val description: String? = null,
    val color: String? = null, // Hex color for UI
    val icon: String? = null, // Icon name or emoji
    val parentId: Long? = null,

    val createdAt: Long = System.currentTimeMillis()
) {
    val destinationType: DestinationType
        get() = when (parentId) {
            OFFICE_CATEGORY_PARENT_ID -> DestinationType.OFFICE
            CONTRACTOR_CATEGORY_PARENT_ID -> DestinationType.CONTRACTOR
            else -> DestinationType.OFFICE
        }

    companion object {
        const val OFFICE_CATEGORY_PARENT_ID = 1L
        const val CONTRACTOR_CATEGORY_PARENT_ID = 2L
    }
}
