package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.ScannerSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannerSettingsDao {
    
    @Query("SELECT * FROM scanner_settings WHERE id = 1 LIMIT 1")
    fun getScannerSettings(): Flow<ScannerSettingsEntity?>
    
    @Query("SELECT scannerId FROM scanner_settings WHERE id = 1 LIMIT 1")
    suspend fun getScannerId(): String?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScannerSettings(settings: ScannerSettingsEntity)
    
    @Query("UPDATE scanner_settings SET scannerId = :scannerId, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateScannerId(scannerId: String, updatedAt: Long = System.currentTimeMillis())
}
