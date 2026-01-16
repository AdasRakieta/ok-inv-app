package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "inventory_count_items",
    foreignKeys = [
        ForeignKey(
            entity = InventoryCountEntity::class,
            parentColumns = ["id"],
            childColumns = ["countId"],
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
        Index(value = ["countId"]),
        Index(value = ["productId"]),
        Index(value = ["countId", "productId"], unique = true)
    ]
)
data class InventoryCountItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val countId: Long,
    val productId: Long,
    
    val expectedQuantity: Int = 1,
    val actualQuantity: Int = 0,
    val variance: Int = 0,
    
    val scannedAt: Long? = null,
    val notes: String? = null,
    
    val status: ItemCountStatus = ItemCountStatus.NOT_COUNTED
)

enum class ItemCountStatus {
    NOT_COUNTED,
    COUNTED,
    MISSING,
    EXTRA
}
