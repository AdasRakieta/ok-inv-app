package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.CategoryDao
import com.example.inventoryapp.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    
    fun getCategoryById(categoryId: Long): Flow<CategoryEntity?> = 
        categoryDao.getCategoryById(categoryId)
    
    suspend fun getCategoryByName(name: String): CategoryEntity? = 
        categoryDao.getCategoryByName(name)
    
    suspend fun insertCategory(category: CategoryEntity): Long = 
        categoryDao.insertCategory(category)
    
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long> = 
        categoryDao.insertCategories(categories)
    
    suspend fun updateCategory(category: CategoryEntity) = 
        categoryDao.updateCategory(category)
    
    suspend fun deleteCategory(category: CategoryEntity) = 
        categoryDao.deleteCategory(category)
    
    suspend fun deleteCategoryById(categoryId: Long) = 
        categoryDao.deleteCategoryById(categoryId)
    
    // Initialize default categories if needed
    suspend fun initializeDefaultCategories() {
        val existingCategories = categoryDao.getCategoryByName("Electronics")
        if (existingCategories == null) {
            val defaults = listOf(
                CategoryEntity(name = "Electronics", description = "Electronic devices", color = "#3B82F6", icon = "💻"),
                CategoryEntity(name = "Furniture", description = "Office furniture", color = "#8B5CF6", icon = "🪑"),
                CategoryEntity(name = "Tools", description = "Tools and equipment", color = "#F59E0B", icon = "🔧"),
                CategoryEntity(name = "Supplies", description = "Office supplies", color = "#10B981", icon = "📦"),
                CategoryEntity(name = "Other", description = "Miscellaneous items", color = "#6B7280", icon = "📋")
            )
            insertCategories(defaults)
        }
    }
}
