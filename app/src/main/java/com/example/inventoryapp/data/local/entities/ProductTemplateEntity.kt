package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "product_templates",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class ProductTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val categoryId: Long,
    
    // Template defaults
    val defaultManufacturer: String? = null,
    val defaultModel: String? = null,
    val defaultDescription: String? = null,
    
    // Serial number pattern (e.g., "SN-{YYYY}-{####}")
    val serialNumberPattern: String? = null,
    val serialNumberPrefix: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
