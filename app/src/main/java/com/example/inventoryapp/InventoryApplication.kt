package com.example.inventoryapp

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.Room
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.*

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
    // Department repository
    val departmentRepository by lazy { com.example.inventoryapp.data.repository.DepartmentRepository(database.departmentDao()) }
    // Company repositories
    val companyRepository by lazy { CompanyRepository(database.companyDao()) }
    val contractorPointRepository by lazy { ContractorPointRepository(database.contractorPointDao()) }
    
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
        
        CoroutineScope(Dispatchers.IO).launch {
            categoryRepository.initializeDefaultCategories()
            // Initialize default departments if none exist
            departmentRepository.initializeDefaultDepartments(listOf("IT / Helpdesk", "Marketing", "Sprzedaż", "HR", "Zarząd"))
        }
    }
}
