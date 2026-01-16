package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.EquipmentDao
import com.example.inventoryapp.data.local.entities.EquipmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EquipmentRepository(private val equipmentDao: EquipmentDao) {
    
    suspend fun getAllEquipment(): List<EquipmentEntity> = withContext(Dispatchers.IO) {
        equipmentDao.getAll()
    }
    
    suspend fun getEquipmentById(id: Long): EquipmentEntity? = withContext(Dispatchers.IO) {
        equipmentDao.getById(id)
    }
    
    suspend fun getEquipmentBySerialNumber(serial: String): EquipmentEntity? = withContext(Dispatchers.IO) {
        equipmentDao.getBySerialNumber(serial)
    }
    
    suspend fun insertEquipment(equipment: EquipmentEntity): Long = withContext(Dispatchers.IO) {
        equipmentDao.insert(equipment)
    }
    
    suspend fun updateEquipment(equipment: EquipmentEntity) = withContext(Dispatchers.IO) {
        equipmentDao.update(equipment.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteEquipment(equipment: EquipmentEntity) = withContext(Dispatchers.IO) {
        equipmentDao.delete(equipment)
    }
    
    /**
     * Update equipment status (AVAILABLE, ASSIGNED, REPAIR, RETIRED)
     */
    suspend fun updateEquipmentStatus(equipmentId: Long, status: String) = withContext(Dispatchers.IO) {
        val equipment = equipmentDao.getById(equipmentId)
        equipment?.let {
            equipmentDao.update(it.copy(status = status, updatedAt = System.currentTimeMillis()))
        }
    }
}
