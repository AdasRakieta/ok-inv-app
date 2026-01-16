package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.EquipmentAssignmentEntity

@Dao
interface EquipmentAssignmentDao {
    @Query("SELECT * FROM equipment_assignments WHERE returnedAt IS NULL ORDER BY assignedAt DESC")
    suspend fun getActiveAssignments(): List<EquipmentAssignmentEntity>

    @Query("SELECT * FROM equipment_assignments WHERE employeeId = :employeeId AND returnedAt IS NULL")
    suspend fun getActiveAssignmentsForEmployee(employeeId: Long): List<EquipmentAssignmentEntity>

    @Query("SELECT * FROM equipment_assignments WHERE equipmentId = :equipmentId AND returnedAt IS NULL LIMIT 1")
    suspend fun getActiveAssignmentForEquipment(equipmentId: Long): EquipmentAssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: EquipmentAssignmentEntity): Long

    @Update
    suspend fun update(assignment: EquipmentAssignmentEntity)
}
