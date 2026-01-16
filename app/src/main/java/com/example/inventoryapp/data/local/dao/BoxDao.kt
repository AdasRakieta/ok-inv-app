package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.BoxProductCrossRef
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface BoxDao {
    
    @Query("SELECT * FROM boxes ORDER BY createdAt DESC")
    fun getAllBoxes(): Flow<List<BoxEntity>>
    
    @Query("SELECT * FROM boxes WHERE id = :boxId")
    fun getBoxById(boxId: Long): Flow<BoxEntity?>
    
    @Query("SELECT * FROM boxes WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getBoxByName(name: String): BoxEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: BoxEntity): Long
    
    @Update
    suspend fun updateBox(box: BoxEntity)
    
    @Delete
    suspend fun deleteBox(box: BoxEntity)
    
    @Query("DELETE FROM boxes WHERE id = :boxId")
    suspend fun deleteBoxById(boxId: Long)
    
    // Box-Product relations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProductToBox(crossRef: BoxProductCrossRef)
    
    @Delete
    suspend fun removeProductFromBox(crossRef: BoxProductCrossRef)
    
    @Query("DELETE FROM box_product_cross_ref WHERE boxId = :boxId AND productId = :productId")
    suspend fun removeProductFromBoxById(boxId: Long, productId: Long)
    
    @Query("""
        SELECT products.* FROM products
        INNER JOIN box_product_cross_ref ON products.id = box_product_cross_ref.productId
        WHERE box_product_cross_ref.boxId = :boxId
        ORDER BY products.createdAt DESC
    """)
    fun getProductsInBox(boxId: Long): Flow<List<ProductEntity>>
    
    @Transaction
    @Query("""
        SELECT products.* FROM products
        INNER JOIN box_product_cross_ref ON products.id = box_product_cross_ref.productId
        WHERE box_product_cross_ref.boxId = :boxId
        ORDER BY products.createdAt DESC
    """)
    fun getProductsWithCategoriesInBox(boxId: Long): Flow<List<ProductWithCategory>>
    
    @Query("""
        SELECT COUNT(*) FROM box_product_cross_ref
        WHERE boxId = :boxId
    """)
    fun getProductCountInBox(boxId: Long): Flow<Int>
    
    @Query("""
        SELECT boxes.*, COUNT(box_product_cross_ref.productId) as productCount
        FROM boxes
        LEFT JOIN box_product_cross_ref ON boxes.id = box_product_cross_ref.boxId
        GROUP BY boxes.id
        ORDER BY boxes.createdAt DESC
    """)
    fun getAllBoxesWithCount(): Flow<List<BoxWithCount>>
    
    @Query("SELECT COUNT(*) FROM boxes")
    fun getBoxCount(): Flow<Int>
    
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM box_product_cross_ref 
            WHERE boxId = :boxId AND productId = :productId
        )
    """)
    suspend fun isProductInBox(boxId: Long, productId: Long): Boolean
    
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM boxes INNER JOIN box_product_cross_ref ON boxes.id = box_product_cross_ref.boxId WHERE box_product_cross_ref.productId = :productId LIMIT 1")
    fun getBoxForProduct(productId: Long): Flow<BoxEntity?>

    @Query("SELECT * FROM box_product_cross_ref WHERE productId = :productId")
    suspend fun getBoxCrossRefsForProduct(productId: Long): List<BoxProductCrossRef>

}

data class BoxWithCount(
    @Embedded val box: BoxEntity,
    val productCount: Int
)
