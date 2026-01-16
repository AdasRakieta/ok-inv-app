package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.InventoryCountEntity
import com.example.inventoryapp.data.local.entities.CountStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryCountDao {
    @Query("SELECT * FROM inventory_counts ORDER BY startDate DESC")
    fun getAllCounts(): Flow<List<InventoryCountEntity>>
    
    @Query("SELECT * FROM inventory_counts WHERE id = :countId")
    fun getCountById(countId: Long): Flow<InventoryCountEntity?>
    
    @Query("SELECT * FROM inventory_counts WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getCountBySessionId(sessionId: String): InventoryCountEntity?
    
    @Query("SELECT * FROM inventory_counts WHERE status = :status ORDER BY startDate DESC")
    fun getCountsByStatus(status: CountStatus): Flow<List<InventoryCountEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCount(count: InventoryCountEntity): Long
    
    @Update
    suspend fun updateCount(count: InventoryCountEntity)
    
    @Delete
    suspend fun deleteCount(count: InventoryCountEntity)
}
