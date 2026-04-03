package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "companies",
    indices = [
        Index(value = ["taxId"], unique = true),
        Index(value = ["name"])
    ]
)
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val taxId: String,
    val address: String?,
    val city: String?,
    val postalCode: String?,
    val country: String? = null,
    val contactPerson: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
