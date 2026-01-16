package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE id = :productId")
    fun getProductById(productId: Long): Flow<ProductEntity?>
    
    @Query("SELECT * FROM products WHERE serialNumber = :serialNumber LIMIT 1")
    suspend fun getProductBySerialNumber(serialNumber: String): ProductEntity?
    
    @Query("SELECT * FROM products WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE warehouseLocationId = :locationId")
    fun getProductsByLocation(locationId: Long): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE assignedToEmployeeId = :employeeId")
    fun getProductsAssignedToEmployee(employeeId: Long): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE status = :status ORDER BY createdAt DESC")
    fun getProductsByStatus(status: ProductStatus): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR serialNumber LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>
    
    @Query("SELECT COUNT(*) FROM products")
    fun getProductCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM products WHERE status = :status")
    fun getProductCountByStatus(status: ProductStatus): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>): List<Long>
    
    @Update
    suspend fun updateProduct(product: ProductEntity)
    
    @Delete
    suspend fun deleteProduct(product: ProductEntity)
    
    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)
    
    @Query("UPDATE products SET assignedToEmployeeId = :employeeId, assignmentDate = :assignmentDate, status = :status, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun assignToEmployee(productId: Long, employeeId: Long, assignmentDate: Long, status: ProductStatus, updatedAt: Long)
    
    @Query("UPDATE products SET assignedToEmployeeId = NULL, assignmentDate = NULL, status = :status, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun unassignFromEmployee(productId: Long, status: ProductStatus, updatedAt: Long)
}
