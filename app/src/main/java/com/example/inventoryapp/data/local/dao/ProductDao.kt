package com.example.inventoryapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.inventoryapp.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    fun getProductById(productId: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE serialNumber = :serialNumber")
    suspend fun getProductBySerialNumber(serialNumber: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE serialNumber = :serialNumber")
    suspend fun deleteProductBySerialNumber(serialNumber: String)
    
    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    @Query("UPDATE products SET serialNumber = :serialNumber, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateSerialNumber(productId: Long, serialNumber: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM products WHERE serialNumber = :serialNumber")
    suspend fun isSerialNumberExists(serialNumber: String): Int

    @Query("SELECT * FROM products WHERE name = :name AND categoryId = :categoryId AND serialNumber IS NULL LIMIT 1")
    suspend fun findProductByNameAndCategory(name: String, categoryId: Long?): ProductEntity?

    @Query("UPDATE products SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateQuantity(productId: Long, quantity: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        SELECT categoryId, SUM(quantity) as totalQuantity
        FROM products
        GROUP BY categoryId
    """)
    suspend fun getCategoryStatistics(): List<CategoryCount>
}

data class CategoryCount(
    val categoryId: Long?,
    val totalQuantity: Int
)
