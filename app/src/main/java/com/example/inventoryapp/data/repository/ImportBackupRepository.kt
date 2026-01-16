package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ImportBackupDao
import com.example.inventoryapp.data.local.entities.ImportBackupEntity
import kotlinx.coroutines.flow.Flow

class ImportBackupRepository(private val importBackupDao: ImportBackupDao) {
    
    fun getAllBackups(): Flow<List<ImportBackupEntity>> = importBackupDao.getAllBackups()
    
    suspend fun getLatestBackup(): ImportBackupEntity? = importBackupDao.getLatestBackup()
    
    suspend fun getBackupById(backupId: Long): ImportBackupEntity? = importBackupDao.getBackupById(backupId)
    
    suspend fun insertBackup(backup: ImportBackupEntity): Long = importBackupDao.insertBackup(backup)
    
    suspend fun deleteBackup(backup: ImportBackupEntity) = importBackupDao.deleteBackup(backup)
    
    suspend fun deleteBackupById(backupId: Long) = importBackupDao.deleteBackupById(backupId)
    
    suspend fun deleteAllBackups() = importBackupDao.deleteAllBackups()
    
    suspend fun getBackupCount(): Int = importBackupDao.getBackupCount()
    
    suspend fun pruneOldBackups(keepCount: Int = 5) = importBackupDao.pruneOldBackups(keepCount)
}
