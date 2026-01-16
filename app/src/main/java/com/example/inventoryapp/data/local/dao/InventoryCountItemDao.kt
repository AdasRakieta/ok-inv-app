package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.InventoryCountItemEntity
import com.example.inventoryapp.data.local.entities.ItemCountStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryCountItemDao {
    @Query("SELECT * FROM inventory_count_items WHERE countId = :countId")
    fun getCountItems(countId: Long): Flow<List<InventoryCountItemEntity>>
    
    @Query("SELECT * FROM inventory_count_items WHERE countId = :countId AND status = :status")
    fun getCountItemsByStatus(countId: Long, status: ItemCountStatus): Flow<List<InventoryCountItemEntity>>
    
    @Query("SELECT * FROM inventory_count_items WHERE id = :itemId")
    fun getCountItemById(itemId: Long): Flow<InventoryCountItemEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountItem(item: InventoryCountItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountItems(items: List<InventoryCountItemEntity>): List<Long>
    
    @Update
    suspend fun updateCountItem(item: InventoryCountItemEntity)
    
    @Delete
    suspend fun deleteCountItem(item: InventoryCountItemEntity)
}
