package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ProductTemplateDao
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProductTemplateRepository(private val productTemplateDao: ProductTemplateDao) {
    
    fun getAllTemplates(): Flow<List<ProductTemplateEntity>> = 
        productTemplateDao.getAllTemplates()
    
    fun getTemplateById(templateId: Long): Flow<ProductTemplateEntity?> = 
        productTemplateDao.getTemplateById(templateId)
    
    fun getTemplatesByCategory(categoryId: Long): Flow<List<ProductTemplateEntity>> = 
        productTemplateDao.getTemplatesByCategory(categoryId)
    
    suspend fun insertTemplate(template: ProductTemplateEntity): Long = 
        productTemplateDao.insertTemplate(template)
    
    suspend fun updateTemplate(template: ProductTemplateEntity) = 
        productTemplateDao.updateTemplate(template)
    
    suspend fun deleteTemplate(template: ProductTemplateEntity) = 
        productTemplateDao.deleteTemplate(template)
    
    // Initialize default templates for common products
    suspend fun initializeDefaultTemplates(categoryRepository: CategoryRepository) {
        val existing = productTemplateDao.getAllTemplates().first()
        if (existing.isNotEmpty()) return // Already initialized
        
        // Get category IDs
        val laptopCategory = categoryRepository.getCategoryByName("Laptop")
        val phoneCategory = categoryRepository.getCategoryByName("Telefon")
        val tabletCategory = categoryRepository.getCategoryByName("Tablet")
        val monitorCategory = categoryRepository.getCategoryByName("Monitor")
        val mouseCategory = categoryRepository.getCategoryByName("Mysz")
        val keyboardCategory = categoryRepository.getCategoryByName("Klawiatura")
        val dockingCategory = categoryRepository.getCategoryByName("Stacja dokująca")
        
        val templates = mutableListOf<ProductTemplateEntity>()
        
        // Laptop templates
        laptopCategory?.let { category ->
            templates.add(
                ProductTemplateEntity(
                    name = "Laptop Dell Latitude",
                    categoryId = category.id,
                    defaultManufacturer = "Dell",
                    defaultModel = "Latitude 5420",
                    defaultDescription = "Laptop biznesowy Dell Latitude"
                )
            )
            templates.add(
                ProductTemplateEntity(
                    name = "Laptop Lenovo ThinkPad",
                    categoryId = category.id,
                    defaultManufacturer = "Lenovo",
                    defaultModel = "ThinkPad T14",
                    defaultDescription = "Laptop biznesowy Lenovo ThinkPad"
                )
            )
        }
        
        // Phone templates
        phoneCategory?.let { category ->
            templates.add(
                ProductTemplateEntity(
                    name = "Skaner Zebra TC58",
                    categoryId = category.id,
                    defaultManufacturer = "Zebra",
                    defaultModel = "TC58",
                    defaultDescription = "Terminal mobilny z systemem Android"
                )
            )
        }
        
        // Monitor templates
        monitorCategory?.let { category ->
            templates.add(
                ProductTemplateEntity(
                    name = "Monitor Dell 24\"",
                    categoryId = category.id,
                    defaultManufacturer = "Dell",
                    defaultModel = "P2422H",
                    defaultDescription = "Monitor 24 cale Full HD"
                )
            )
        }
        
        // Keyboard template
        keyboardCategory?.let { category ->
            templates.add(
                ProductTemplateEntity(
                    name = "Klawiatura Logitech",
                    categoryId = category.id,
                    defaultManufacturer = "Logitech",
                    defaultModel = "K780",
                    defaultDescription = "Klawiatura bezprzewodowa"
                )
            )
        }
        
        // Mouse template
        mouseCategory?.let { category ->
            templates.add(
                ProductTemplateEntity(
                    name = "Mysz Logitech",
                    categoryId = category.id,
                    defaultManufacturer = "Logitech",
                    defaultModel = "MX Master 3",
                    defaultDescription = "Mysz bezprzewodowa"
                )
            )
        }
        
        // Insert all templates
        templates.forEach { template ->
            productTemplateDao.insertTemplate(template)
        }
    }
}
