package com.example.inventoryapp

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.EmployeeRepository
import com.example.inventoryapp.data.repository.EquipmentRepository
import com.example.inventoryapp.data.repository.AssignmentRepository
import com.example.inventoryapp.data.repository.EquipmentDataSeeder
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

    // Equipment domain repositories
    val employeeRepository by lazy { EmployeeRepository(database.employeeDao()) }
    val equipmentRepository by lazy { EquipmentRepository(database.equipmentDao()) }
    val assignmentRepository by lazy { AssignmentRepository(database.equipmentAssignmentDao(), database.equipmentDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // Seed sample data for internal equipment on first launch
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EquipmentDataSeeder.seedSampleData(this@InventoryApplication)
            } catch (e: Exception) {
                android.util.Log.e("InventoryApp", "Seeding failed", e)
            }
        }
    }
}
