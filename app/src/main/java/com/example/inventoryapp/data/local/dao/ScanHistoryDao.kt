package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE productId = :productId ORDER BY timestamp DESC")
    fun getScansByProduct(productId: Long): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scanHistory: ScanHistoryEntity): Long

    @Delete
    suspend fun deleteScan(scanHistory: ScanHistoryEntity)

    @Query("DELETE FROM scan_history WHERE timestamp < :timestamp")
    suspend fun deleteScansOlderThan(timestamp: Long)
}
