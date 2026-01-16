package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductTemplateDao {
    @Query("SELECT * FROM product_templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<ProductTemplateEntity>>

    @Query("SELECT * FROM product_templates WHERE id = :templateId")
    fun getTemplateById(templateId: Long): Flow<ProductTemplateEntity?>

    @Query("SELECT * FROM product_templates WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getTemplatesByCategory(categoryId: Long): Flow<List<ProductTemplateEntity>>

    @Query("SELECT * FROM product_templates WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTemplateByName(name: String): ProductTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTemplate(template: ProductTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: ProductTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: ProductTemplateEntity)

    @Query("SELECT COUNT(*) FROM product_templates WHERE name = :name")
    suspend fun isTemplateNameExists(name: String): Int
}
