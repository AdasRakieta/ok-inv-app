package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.WarehouseLocationDao
import com.example.inventoryapp.data.local.entities.WarehouseLocationEntity
import kotlinx.coroutines.flow.Flow

class WarehouseLocationRepository(private val warehouseLocationDao: WarehouseLocationDao) {
    
    fun getAllLocations(): Flow<List<WarehouseLocationEntity>> = 
        warehouseLocationDao.getAllLocations()
    
    fun getLocationById(locationId: Long): Flow<WarehouseLocationEntity?> = 
        warehouseLocationDao.getLocationById(locationId)
    
    suspend fun getLocationByCode(code: String): WarehouseLocationEntity? = 
        warehouseLocationDao.getLocationByCode(code)
    
    fun getLocationsByZone(zone: String): Flow<List<WarehouseLocationEntity>> = 
        warehouseLocationDao.getLocationsByZone(zone)
    
    suspend fun insertLocation(location: WarehouseLocationEntity): Long = 
        warehouseLocationDao.insertLocation(location)
    
    suspend fun updateLocation(location: WarehouseLocationEntity) = 
        warehouseLocationDao.updateLocation(location)
    
    suspend fun deleteLocation(location: WarehouseLocationEntity) = 
        warehouseLocationDao.deleteLocation(location)
}
