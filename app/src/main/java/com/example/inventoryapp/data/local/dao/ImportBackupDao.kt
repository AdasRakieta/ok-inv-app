package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.ImportBackupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportBackupDao {
    
    @Query("SELECT * FROM import_backups ORDER BY backupTimestamp DESC")
    fun getAllBackups(): Flow<List<ImportBackupEntity>>
    
    @Query("SELECT * FROM import_backups ORDER BY backupTimestamp DESC LIMIT 1")
    suspend fun getLatestBackup(): ImportBackupEntity?
    
    @Query("SELECT * FROM import_backups WHERE id = :backupId")
    suspend fun getBackupById(backupId: Long): ImportBackupEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: ImportBackupEntity): Long
    
    @Delete
    suspend fun deleteBackup(backup: ImportBackupEntity)
    
    @Query("DELETE FROM import_backups WHERE id = :backupId")
    suspend fun deleteBackupById(backupId: Long)
    
    @Query("DELETE FROM import_backups")
    suspend fun deleteAllBackups()
    
    @Query("SELECT COUNT(*) FROM import_backups")
    suspend fun getBackupCount(): Int
    
    /**
     * Keep only the most recent N backups, delete older ones
     */
    @Query("""
        DELETE FROM import_backups 
        WHERE id NOT IN (
            SELECT id FROM import_backups 
            ORDER BY backupTimestamp DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun pruneOldBackups(keepCount: Int = 5)
}
