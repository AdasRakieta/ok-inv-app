package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "box_product_cross_ref",
    primaryKeys = ["boxId", "productId"],
    foreignKeys = [
        ForeignKey(
            entity = BoxEntity::class,
            parentColumns = ["id"],
            childColumns = ["boxId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("boxId"), Index("productId")]
)
data class BoxProductCrossRef(
    val boxId: Long,
    val productId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
