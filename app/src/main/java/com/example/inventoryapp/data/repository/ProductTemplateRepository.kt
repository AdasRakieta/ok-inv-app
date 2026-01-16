package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ProductTemplateDao
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import kotlinx.coroutines.flow.Flow

class ProductTemplateRepository(private val productTemplateDao: ProductTemplateDao) {
    
    fun getAllTemplates(): Flow<List<ProductTemplateEntity>> = 
        productTemplateDao.getAllTemplates()
    
    fun getTemplateById(templateId: Long): Flow<ProductTemplateEntity?> = 
        productTemplateDao.getTemplateById(templateId)
    
    fun getTemplatesByCategory(categoryId: Long): Flow<List<ProductTemplateEntity>> =
        productTemplateDao.getTemplatesByCategory(categoryId)
    
    suspend fun insertTemplate(template: ProductTemplateEntity): Long {
        // Check if template with same name already exists
        val existing = productTemplateDao.getTemplateByName(template.name)
        if (existing != null) {
            throw IllegalArgumentException("Template with name '${template.name}' already exists")
        }
        return productTemplateDao.insertTemplate(template)
    }
    
    suspend fun updateTemplate(template: ProductTemplateEntity) =
        productTemplateDao.updateTemplate(template)
    
    suspend fun deleteTemplate(template: ProductTemplateEntity) =
        productTemplateDao.deleteTemplate(template)
    
    suspend fun isTemplateNameExists(name: String): Boolean =
        productTemplateDao.isTemplateNameExists(name) > 0
}
