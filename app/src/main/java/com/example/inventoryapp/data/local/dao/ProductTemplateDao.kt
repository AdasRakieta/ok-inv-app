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
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ProductTemplateEntity): Long
    
    @Update
    suspend fun updateTemplate(template: ProductTemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: ProductTemplateEntity)
}
