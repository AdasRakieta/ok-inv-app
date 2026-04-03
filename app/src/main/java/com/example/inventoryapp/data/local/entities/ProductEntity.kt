package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["serialNumber"], unique = true),
        Index(value = ["customId"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["warehouseLocationId"]),
        Index(value = ["assignedToContractorPointId"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val customId: String? = null,  // Custom ID like "LapOK10"
    val serialNumber: String,
    val categoryId: Long? = null,
    
    // Warehouse location
    val warehouseLocationId: Long? = null,
    val shelf: String? = null,
    val bin: String? = null,
    
    // Product details
    val description: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    
    // Status
    val status: ProductStatus = ProductStatus.IN_STOCK,
    val condition: String? = null,
    
    // Tracking
    val purchaseDate: Long? = null,
    val purchasePrice: Double? = null,
    val warrantyExpiryDate: Long? = null,
    
    // Assignment (if assigned to employee)
    val assignedToEmployeeId: Long? = null,
    val assignedToContractorPointId: Long? = null,
    val assignmentDate: Long? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Notes
    val notes: String? = null
    ,
    val movementHistory: String? = null
)

enum class ProductStatus {
    IN_STOCK,
    ASSIGNED,
    UNASSIGNED,
    IN_REPAIR,
    RETIRED,
    LOST
}
