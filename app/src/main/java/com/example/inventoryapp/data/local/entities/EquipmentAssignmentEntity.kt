package com.example.inventoryapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "equipment_assignments",
    indices = [Index(value = ["employeeId", "equipmentId", "assignedAt"], unique = true)]
)
data class EquipmentAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val equipmentId: Long,
    val assignedAt: Long = System.currentTimeMillis(),
    val returnedAt: Long? = null,
    val note: String? = null
)
