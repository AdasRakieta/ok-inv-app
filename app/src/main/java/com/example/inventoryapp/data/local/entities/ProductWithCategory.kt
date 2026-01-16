package com.example.inventoryapp.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Composite data class that holds a Product and its Category
 * Used for printing labels where we need category information
 */
data class ProductWithCategory(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)
