package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.CategoryDao
import com.example.inventoryapp.data.local.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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
        val desired = listOf(
            CategoryEntity(name = "Laptop", description = "Laptopy", color = "#3B82F6", icon = "💻"),
            CategoryEntity(name = "Telefon", description = "Telefony", color = "#10B981", icon = "📱"),
            CategoryEntity(name = "Tablet", description = "Tablety", color = "#8B5CF6", icon = "📱"),
            CategoryEntity(name = "Monitor", description = "Monitory", color = "#F59E0B", icon = "🖥️"),
            CategoryEntity(name = "Mysz", description = "Myszy komputerowe", color = "#6366F1", icon = "🖱️"),
            CategoryEntity(name = "Klawiatura", description = "Klawiatury", color = "#EC4899", icon = "⌨️"),
            CategoryEntity(name = "Stacja dokująca", description = "Stacje dokujące", color = "#0EA5E9", icon = "🔌"),
            CategoryEntity(name = "Narzędzia", description = "Narzędzia i akcesoria", color = "#A16207", icon = "🔧"),
            CategoryEntity(name = "Other", description = "Pozostałe", color = "#6B7280", icon = "📦")
        )

        val existing = categoryDao.getAllCategories().first()
        val desiredNames = desired.map { it.name }.toSet()

        // Delete categories that are not desired anymore (product FK is set-null on delete)
        existing.filter { it.name !in desiredNames }.forEach { obsolete ->
            categoryDao.deleteCategoryById(obsolete.id)
        }

        // Insert missing or update existing desired categories
        desired.forEach { category ->
            val match = categoryDao.getCategoryByName(category.name)
            if (match == null) {
                // Insert new category
                categoryDao.insertCategory(category)
            } else {
                // Update existing category to ensure icon, description and color are current
                categoryDao.updateCategory(match.copy(
                    description = category.description,
                    color = category.color,
                    icon = category.icon
                ))
            }
        }
    }
}
