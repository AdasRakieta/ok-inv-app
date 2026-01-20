package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ScanHistoryDao
import com.example.inventoryapp.data.local.entities.ScanHistoryEntity
import com.example.inventoryapp.data.local.entities.ScanType
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class ScanHistoryRepository(private val scanHistoryDao: ScanHistoryDao) {
    
    fun getRecentScans(): Flow<List<ScanHistoryEntity>> = 
        scanHistoryDao.getRecentScans()
    
    fun getScansByProduct(productId: Long): Flow<List<ScanHistoryEntity>> = 
        scanHistoryDao.getScansByProduct(productId)
    
    fun getScansByContext(context: String): Flow<List<ScanHistoryEntity>> = 
        scanHistoryDao.getScansByContext(context)
    
    suspend fun recordScan(
        scannedValue: String,
        scanType: ScanType = ScanType.BARCODE,
        context: String? = null,
        productId: Long? = null,
        employeeId: Long? = null,
        success: Boolean = true,
        errorMessage: String? = null
    ): Long {
        val scan = ScanHistoryEntity(
            scannedValue = scannedValue,
            scanType = scanType,
            context = context,
            productId = productId,
            employeeId = employeeId,
            success = success,
            errorMessage = errorMessage
        )
        return scanHistoryDao.insertScan(scan)
    }
    
    suspend fun cleanupOldScans(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())
        scanHistoryDao.deleteOldScans(cutoffTime)
    }
}
