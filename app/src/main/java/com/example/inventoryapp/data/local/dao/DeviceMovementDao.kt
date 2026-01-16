package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.DeviceMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceMovementDao {
    @Query("SELECT * FROM device_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsByProduct(productId: Long): Flow<List<DeviceMovementEntity>>

    @Query("SELECT * FROM device_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<DeviceMovementEntity>>

    @Query("SELECT * FROM device_movements WHERE productId = :productId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMovement(productId: Long): DeviceMovementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: DeviceMovementEntity): Long

    @Delete
    suspend fun deleteMovement(movement: DeviceMovementEntity)
}
