package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ScanHistoryDao
import com.example.inventoryapp.data.local.entities.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanHistoryDao: ScanHistoryDao) {
    
    fun getAllScans(): Flow<List<ScanHistoryEntity>> = scanHistoryDao.getAllScans()
    
    fun getScansByProduct(productId: Long): Flow<List<ScanHistoryEntity>> =
        scanHistoryDao.getScansByProduct(productId)
    
    suspend fun insertScan(scanHistory: ScanHistoryEntity): Long =
        scanHistoryDao.insertScan(scanHistory)
    
    suspend fun deleteScan(scanHistory: ScanHistoryEntity) =
        scanHistoryDao.deleteScan(scanHistory)
    
    suspend fun deleteScansOlderThan(timestamp: Long) =
        scanHistoryDao.deleteScansOlderThan(timestamp)
}
