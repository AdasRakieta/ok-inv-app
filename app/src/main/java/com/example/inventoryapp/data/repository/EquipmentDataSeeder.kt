package com.example.inventoryapp.data.repository

import android.content.Context
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.EquipmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds initial sample data for internal equipment inventory
 * Run once on first app launch or after database reset
 */
object EquipmentDataSeeder {
    
    suspend fun seedSampleData(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val employeeDao = db.employeeDao()
        val equipmentDao = db.equipmentDao()
        
        // Check if already seeded
        val existingEmployees = employeeDao.getAll()
        if (existingEmployees.isNotEmpty()) {
            println("[Seeder] Data already seeded, skipping")
            return@withContext
        }
        
        println("[Seeder] Seeding sample employees and equipment...")
        
        // Seed employees
        val employees = listOf(
            EmployeeEntity(
                name = "Jan Kowalski",
                email = "jan.kowalski@firma.pl",
                department = "IT",
                position = "Developer"
            ),
            EmployeeEntity(
                name = "Anna Nowak",
                email = "anna.nowak@firma.pl",
                department = "Magazyn",
                position = "Magazynier"
            ),
            EmployeeEntity(
                name = "Piotr Wiśniewski",
                email = "piotr.wisniewski@firma.pl",
                department = "Logistyka",
                position = "Kierownik"
            ),
            EmployeeEntity(
                name = "Maria Wójcik",
                email = "maria.wojcik@firma.pl",
                department = "IT",
                position = "Administrator"
            )
        )
        
        employees.forEach { employee ->
            employeeDao.insert(employee)
        }
        
        // Seed equipment
        val equipment = listOf(
            EquipmentEntity(
                name = "Laptop Dell Latitude",
                serialNumber = "DL12345678",
                category = "Laptop",
                status = "AVAILABLE",
                description = "Dell Latitude 5420, i5, 16GB RAM"
            ),
            EquipmentEntity(
                name = "Skaner Zebra TC21",
                serialNumber = "ZB87654321",
                category = "Scanner",
                status = "AVAILABLE",
                description = "Zebra TC21 Android barcode scanner"
            ),
            EquipmentEntity(
                name = "Laptop HP ProBook",
                serialNumber = "HP98765432",
                category = "Laptop",
                status = "AVAILABLE",
                description = "HP ProBook 450 G8, i7, 32GB RAM"
            ),
            EquipmentEntity(
                name = "Drukarka Zebra ZD420",
                serialNumber = "ZD42012345",
                category = "Printer",
                status = "AVAILABLE",
                description = "Zebra ZD420 label printer"
            ),
            EquipmentEntity(
                name = "Skaner Honeywell CT40",
                serialNumber = "HW55512345",
                category = "Scanner",
                status = "AVAILABLE",
                description = "Honeywell CT40 mobile computer"
            ),
            EquipmentEntity(
                name = "Monitor Dell 24\"",
                serialNumber = "DM24567890",
                category = "Monitor",
                status = "AVAILABLE",
                description = "Dell P2422H 24-inch monitor"
            )
        )
        
        equipment.forEach { item ->
            equipmentDao.insert(item)
        }
        
        println("[Seeder] Seeded ${employees.size} employees and ${equipment.size} equipment items")
    }
}
