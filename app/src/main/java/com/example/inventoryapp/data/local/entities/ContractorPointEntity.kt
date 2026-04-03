package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PointType {
    CP,
    CC,
    DC
}

@Entity(
    tableName = "contractor_points",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["pointType"]),
        Index(value = ["companyId"]),
        Index(value = ["name"])
    ]
)
data class ContractorPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val pointType: PointType,
    val companyId: Long,
    val address: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val contactPerson: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
