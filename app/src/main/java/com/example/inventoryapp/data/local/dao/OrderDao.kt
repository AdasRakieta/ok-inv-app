package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.OrderEntity
import com.example.inventoryapp.data.local.entities.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderById(orderId: Long): Flow<OrderEntity?>
    
    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber LIMIT 1")
    suspend fun getOrderByNumber(orderNumber: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE status = :status ORDER BY orderDate DESC")
    fun getOrdersByStatus(status: OrderStatus): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE supplierId = :supplierId ORDER BY orderDate DESC")
    fun getOrdersBySupplier(supplierId: Long): Flow<List<OrderEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long
    
    @Update
    suspend fun updateOrder(order: OrderEntity)
    
    @Delete
    suspend fun deleteOrder(order: OrderEntity)
}
