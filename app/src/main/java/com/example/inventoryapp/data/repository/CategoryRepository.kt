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
        // Parent categories
        val parents = listOf(
            CategoryEntity(name = "Urządzenia Biurowe", description = "Urządzenia używane w biurze", color = "#0F172A", icon = "🏢"),
            CategoryEntity(name = "Urządzenia firmowe", description = "Specjalistyczne urządzenia firmowe", color = "#111827", icon = "🏭")
        )

        val existing = categoryDao.getAllCategories().first()

        // Ensure parents exist and collect their IDs
        val parentIdMap = mutableMapOf<String, Long>()
        parents.forEach { parent ->
            val match = categoryDao.getCategoryByName(parent.name)
            if (match == null) {
                val id = categoryDao.insertCategory(parent)
                parentIdMap[parent.name] = id
            } else {
                // update metadata if needed
                categoryDao.updateCategory(match.copy(
                    description = parent.description,
                    color = parent.color,
                    icon = parent.icon
                ))
                parentIdMap[parent.name] = match.id
            }
        }

        // Child categories mapped to parents
        val children = listOf(
            // Urządzenia Biurowe
            CategoryEntity(name = "Laptop", description = "Laptopy", color = "#3B82F6", icon = "💻", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Monitor", description = "Monitory", color = "#F59E0B", icon = "🖥️", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Stacja dokująca", description = "Stacje dokujące do laptopów", color = "#0EA5E9", icon = "🔌", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Mysz", description = "Myszy komputerowe", color = "#6366F1", icon = "🖱️", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Klawiatura", description = "Klawiatury", color = "#EC4899", icon = "⌨️", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Zestaw mysz + klawiatura", description = "Zestawy mysz i klawiatura", color = "#7C3AED", icon = "🖱️+⌨️", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Tablet", description = "Tablety", color = "#8B5CF6", icon = "📱", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Telefon", description = "Telefony", color = "#10B981", icon = "📱", parentId = parentIdMap["Urządzenia Biurowe"]),

            // Urządzenia firmowe
            CategoryEntity(name = "Skaner", description = "Skanery ręczne", color = "#0EA5E9", icon = "🔍", parentId = parentIdMap["Urządzenia firmowe"]),
            CategoryEntity(name = "Stacja dokująca skanera", description = "Stacje dokujące do skanerów", color = "#06B6D4", icon = "🪫", parentId = parentIdMap["Urządzenia firmowe"]),
            CategoryEntity(name = "Drukarka mobilna", description = "Drukarki mobilne", color = "#EF4444", icon = "🖨️", parentId = parentIdMap["Urządzenia firmowe"]),
            CategoryEntity(name = "Stacja dokująca drukarki", description = "Stacje dokujące do drukarek", color = "#F97316", icon = "🔌", parentId = parentIdMap["Urządzenia firmowe"]),

            // Misc
            CategoryEntity(name = "Narzędzia", description = "Narzędzia i akcesoria", color = "#A16207", icon = "🔧", parentId = parentIdMap["Urządzenia Biurowe"]),
            CategoryEntity(name = "Other", description = "Pozostałe", color = "#6B7280", icon = "📦", parentId = null)
        )

        val desiredNames = (parents.map { it.name } + children.map { it.name }).toSet()

        // Delete categories that are not desired anymore (product FK is set-null on delete)
        existing.filter { it.name !in desiredNames }.forEach { obsolete ->
            categoryDao.deleteCategoryById(obsolete.id)
        }

        // Insert or update children
        children.forEach { child ->
            val match = categoryDao.getCategoryByName(child.name)
            if (match == null) {
                // Insert new child category
                categoryDao.insertCategory(child)
            } else {
                // Update existing category to ensure icon, description, color and parent are current
                categoryDao.updateCategory(match.copy(
                    description = child.description,
                    color = child.color,
                    icon = child.icon,
                    parentId = child.parentId
                ))
            }
        }
    }
}
