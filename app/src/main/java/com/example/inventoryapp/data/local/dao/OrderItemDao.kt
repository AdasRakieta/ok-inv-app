package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Long): Flow<List<OrderItemEntity>>
    
    @Query("SELECT * FROM order_items WHERE id = :itemId")
    fun getOrderItemById(itemId: Long): Flow<OrderItemEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>): List<Long>
    
    @Update
    suspend fun updateOrderItem(item: OrderItemEntity)
    
    @Delete
    suspend fun deleteOrderItem(item: OrderItemEntity)
}
