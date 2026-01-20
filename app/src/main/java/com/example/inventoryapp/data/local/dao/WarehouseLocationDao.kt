package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.WarehouseLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseLocationDao {
    @Query("SELECT * FROM warehouse_locations ORDER BY code ASC")
    fun getAllLocations(): Flow<List<WarehouseLocationEntity>>
    
    @Query("SELECT * FROM warehouse_locations WHERE id = :locationId")
    fun getLocationById(locationId: Long): Flow<WarehouseLocationEntity?>
    
    @Query("SELECT * FROM warehouse_locations WHERE code = :code LIMIT 1")
    suspend fun getLocationByCode(code: String): WarehouseLocationEntity?
    
    @Query("SELECT * FROM warehouse_locations WHERE zone = :zone ORDER BY code ASC")
    fun getLocationsByZone(zone: String): Flow<List<WarehouseLocationEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: WarehouseLocationEntity): Long
    
    @Update
    suspend fun updateLocation(location: WarehouseLocationEntity)
    
    @Delete
    suspend fun deleteLocation(location: WarehouseLocationEntity)
}
