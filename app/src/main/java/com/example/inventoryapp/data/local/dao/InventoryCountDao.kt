package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.InventoryCountItemEntity
import com.example.inventoryapp.data.local.entities.InventoryCountSessionEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for inventory count operations.
 * Handles sessions and scanned items.
 */
@Dao
interface InventoryCountDao {
        @Query("SELECT * FROM inventory_count_items WHERE sessionId = :sessionId")
        suspend fun getItemsForSession(sessionId: Long): List<InventoryCountItemEntity>
    
    // ===== SESSION OPERATIONS =====
    
    @Query("SELECT * FROM inventory_count_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<InventoryCountSessionEntity>>
    
    @Query("""
        SELECT inventory_count_sessions.*, COUNT(inventory_count_items.productId) as itemCount
        FROM inventory_count_sessions
        LEFT JOIN inventory_count_items ON inventory_count_sessions.id = inventory_count_items.sessionId
        GROUP BY inventory_count_sessions.id
        ORDER BY inventory_count_sessions.createdAt DESC
    """)
    fun getAllSessionsWithCount(): Flow<List<SessionWithCount>>
    
    @Query("SELECT * FROM inventory_count_sessions WHERE id = :sessionId")
    fun getSessionById(sessionId: Long): Flow<InventoryCountSessionEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InventoryCountSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: InventoryCountSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: InventoryCountSessionEntity)
    
    @Query("DELETE FROM inventory_count_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    @Query("UPDATE inventory_count_sessions SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :sessionId")
    suspend fun completeSession(sessionId: Long, completedAt: Long = System.currentTimeMillis())
    
    // ===== ITEM OPERATIONS =====
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryCountItemEntity): Long
    
    @Query("DELETE FROM inventory_count_items WHERE sessionId = :sessionId")
    suspend fun deleteAllItemsInSession(sessionId: Long)
    
    @Query("""
        SELECT MAX(sequenceNumber) 
        FROM inventory_count_items 
        WHERE sessionId = :sessionId
    """)
    suspend fun getMaxSequenceNumber(sessionId: Long): Int?
    
    // ===== PRODUCTS IN SESSION =====
    
    @Query("""
        SELECT products.* 
        FROM products
        INNER JOIN inventory_count_items ON products.id = inventory_count_items.productId
        WHERE inventory_count_items.sessionId = :sessionId
        ORDER BY inventory_count_items.sequenceNumber ASC
    """)
    fun getProductsInSession(sessionId: Long): Flow<List<ProductEntity>>
    
    @Query("""
        SELECT COUNT(*) 
        FROM inventory_count_items 
        WHERE sessionId = :sessionId
    """)
    fun getItemCountInSession(sessionId: Long): Flow<Int>
    
    // ===== STATISTICS =====
    
    /**
     * Get category statistics for a session.
     * Returns map of categoryId -> count of scanned products in that category.
     */
    @MapInfo(keyColumn = "categoryId", valueColumn = "count")
    @Query("""
        SELECT products.categoryId, COUNT(DISTINCT inventory_count_items.productId) as count
        FROM inventory_count_items
        INNER JOIN products ON inventory_count_items.productId = products.id
        WHERE inventory_count_items.sessionId = :sessionId
        GROUP BY products.categoryId
    """)
    fun getCategoryStatistics(sessionId: Long): Flow<Map<Long, Int>>
}

/**
 * Data class for session with item count.
 */
data class SessionWithCount(
    @Embedded val session: InventoryCountSessionEntity,
    val itemCount: Int
)
