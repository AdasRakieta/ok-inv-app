package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employees",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["companyId"])
    ]
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val companyId: Long? = null,
    val email: String?,
    val phone: String?,
    val department: String?,
    val position: String?,
    val notes: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = "$firstName $lastName"
}
