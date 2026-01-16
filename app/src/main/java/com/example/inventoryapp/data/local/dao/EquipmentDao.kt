package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.EquipmentEntity

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment ORDER BY updatedAt DESC")
    suspend fun getAll(): List<EquipmentEntity>

    @Query("SELECT * FROM equipment WHERE id = :id")
    suspend fun getById(id: Long): EquipmentEntity?

    @Query("SELECT * FROM equipment WHERE serialNumber = :serial LIMIT 1")
    suspend fun getBySerialNumber(serial: String): EquipmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(equipment: EquipmentEntity): Long

    @Update
    suspend fun update(equipment: EquipmentEntity)

    @Delete
    suspend fun delete(equipment: EquipmentEntity)
}
