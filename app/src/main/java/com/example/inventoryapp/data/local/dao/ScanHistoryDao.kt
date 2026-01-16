package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentScans(): Flow<List<ScanHistoryEntity>>
    
    @Query("SELECT * FROM scan_history WHERE productId = :productId ORDER BY timestamp DESC")
    fun getScansByProduct(productId: Long): Flow<List<ScanHistoryEntity>>
    
    @Query("SELECT * FROM scan_history WHERE context = :context ORDER BY timestamp DESC LIMIT 50")
    fun getScansByContext(context: String): Flow<List<ScanHistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity): Long
    
    @Query("DELETE FROM scan_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldScans(cutoffTime: Long)
}
