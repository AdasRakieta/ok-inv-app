package com.example.inventoryapp

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InventoryApplication : Application() {

    private val database by lazy {
        try {
            AppDatabase.getDatabase(this)
        } catch (e: Exception) {
            // Migration or DB open failed — fallback to destructive database to avoid crash
            Log.e("InventoryApp", "Database open failed, falling back to destructive migration", e)
            Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "inventory_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    // Employee repository
    val employeeRepository by lazy { EmployeeRepository(database.employeeDao()) }
    
    // Product repositories
    val productRepository by lazy { ProductRepository(database.productDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val productTemplateRepository by lazy { ProductTemplateRepository(database.productTemplateDao()) }
    
    // Equipment repositories (legacy - keeping for compatibility)
    val equipmentRepository by lazy { EquipmentRepository(database.equipmentDao()) }
    val assignmentRepository by lazy { AssignmentRepository(database.equipmentAssignmentDao(), database.equipmentDao()) }
    
    // Warehouse repository
    val warehouseLocationRepository by lazy { WarehouseLocationRepository(database.warehouseLocationDao()) }
    
    // Tracking repository
    val scanHistoryRepository by lazy { ScanHistoryRepository(database.scanHistoryDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize data on first launch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize default categories
                categoryRepository.initializeDefaultCategories()
                
                // Initialize default product templates
                productTemplateRepository.initializeDefaultTemplates(categoryRepository)
                
                // Seed sample products
                com.example.inventoryapp.data.seeder.ProductDataSeeder.seedSampleProducts(this@InventoryApplication)
                
                // Seed sample equipment data (legacy)
                EquipmentDataSeeder.seedSampleData(this@InventoryApplication)
            } catch (e: Exception) {
                Log.e("InventoryApp", "Initialization failed", e)
            }
        }
    }
}
