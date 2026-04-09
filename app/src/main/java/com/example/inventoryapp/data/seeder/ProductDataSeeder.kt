package com.example.inventoryapp.data.seeder

import android.util.Log
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import java.util.*

object ProductDataSeeder {
    
    suspend fun seedSampleProducts(app: InventoryApplication) {
        try {
            val productRepository = app.productRepository
            
            // Get categories (we know they exist from initializeDefaultCategories)
            val now = System.currentTimeMillis()
            
            // Use default category IDs (1=Elektronika, 2=Meble, 3=Narzędzia)
            val electronicsId = 1L
            val furnitureId = 2L
            val toolsId = 3L
        



            // Sample products
            val sampleProducts = listOf(
                ProductEntity(
                    name = "Laptop Dell Latitude 5420",
                    serialNumber = "DL5420-2024-001",
                    categoryId = electronicsId,
                    manufacturer = "Dell",
                    model = "Latitude 5420",
                    description = "Intel Core i5, 16GB RAM, 512GB SSD",
                    status = ProductStatus.IN_STOCK,
                    condition = "Nowy",
                    purchaseDate = now - (30L * 24 * 60 * 60 * 1000), // 30 days ago
                    purchasePrice = 3500.0,
                    warrantyExpiryDate = now + (365L * 2 * 24 * 60 * 60 * 1000), // 2 years
                    shelf = "A1",
                    bin = "01",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    name = "Monitor Samsung 27\"",
                    serialNumber = "SAM27-2024-045",
                    categoryId = electronicsId,
                    manufacturer = "Samsung",
                    model = "S27A600",
                    description = "27 cali, QHD, 75Hz",
                    status = ProductStatus.IN_STOCK,
                    condition = "Nowy",
                    purchaseDate = now - (15L * 24 * 60 * 60 * 1000),
                    purchasePrice = 890.0,
                    warrantyExpiryDate = now + (365L * 24 * 60 * 60 * 1000),
                    shelf = "A1",
                    bin = "02",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    name = "Klawiatura Logitech MX Keys",
                    serialNumber = "LGT-MXK-2024-123",
                    categoryId = electronicsId,
                    manufacturer = "Logitech",
                    model = "MX Keys",
                    description = "Klawiatura bezprzewodowa, podświetlana",
                    status = ProductStatus.ASSIGNED,
                    assignedToEmployeeId = 1L,
                    assignmentDate = now - (5L * 24 * 60 * 60 * 1000),
                    condition = "Używany",
                    purchaseDate = now - (90L * 24 * 60 * 60 * 1000),
                    purchasePrice = 450.0,
                    shelf = "A2",
                    bin = "05",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    name = "Biurko regulowane elektrycznie",
                    serialNumber = "DESK-ERG-2024-010",
                    categoryId = furnitureId,
                    manufacturer = "Ergon",
                    model = "ElectroDesk Pro",
                    description = "Biurko z regulacją wysokości 65-130cm",
                    status = ProductStatus.IN_STOCK,
                    condition = "Nowy",
                    purchaseDate = now - (60L * 24 * 60 * 60 * 1000),
                    purchasePrice = 1200.0,
                    warrantyExpiryDate = now + (365L * 3 * 24 * 60 * 60 * 1000),
                    shelf = "B1",
                    bin = "LARGE",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    name = "Fotel biurowy Herman Miller",
                    serialNumber = "HM-AERON-2024-005",
                    categoryId = furnitureId,
                    manufacturer = "Herman Miller",
                    model = "Aeron Remastered",
                    description = "Fotel ergonomiczny, rozmiar B",
                    status = ProductStatus.ASSIGNED,
                    assignedToEmployeeId = 1L,
                    assignmentDate = now - (10L * 24 * 60 * 60 * 1000),
                    condition = "Nowy",
                    purchaseDate = now - (45L * 24 * 60 * 60 * 1000),
                    purchasePrice = 4500.0,
                    warrantyExpiryDate = now + (365L * 12 * 24 * 60 * 60 * 1000), // 12 years
                    shelf = "B2",
                    bin = "LARGE",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    name = "Wiertarka Makita DHP484",
                    serialNumber = "MAK-DHP484-2024-078",
                    categoryId = toolsId,
                    manufacturer = "Makita",
                    model = "DHP484",
                    description = "Wiertarko-wkrętarka akumulatorowa 18V",
                    status = ProductStatus.IN_STOCK,
                    condition = "Używany",
                    purchaseDate = now - (180L * 24 * 60 * 60 * 1000),
                    purchasePrice = 350.0,
                    warrantyExpiryDate = now - (180L * 24 * 60 * 60 * 1000) + (365L * 24 * 60 * 60 * 1000),
                    shelf = "C1",
                    bin = "TOOL-01",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    name = "Zestaw kluczy nasadowych",
                    serialNumber = "TOOL-SET-2024-234",
                    categoryId = toolsId,
                    manufacturer = "Yato",
                    model = "YT-38841",
                    description = "Zestaw 94 elementów w walizce",
                    status = ProductStatus.IN_REPAIR,
                    condition = "Uszkodzony",
                    purchaseDate = now - (365L * 24 * 60 * 60 * 1000),
                    purchasePrice = 280.0,
                    shelf = "C2",
                    bin = "TOOL-05",
                    notes = "Brakuje klucza 17mm, w naprawie",
                    createdAt = now,
                    updatedAt = now
                ),
                ProductEntity(
                    name = "Drukarka HP LaserJet Pro",
                    serialNumber = "HP-LJ-PRO-2024-012",
                    categoryId = electronicsId,
                    manufacturer = "HP",
                    model = "LaserJet Pro M404dn",
                    description = "Drukarka laserowa mono, duplex",
                    status = ProductStatus.IN_STOCK,
                    condition = "Nowy",
                    purchaseDate = now - (20L * 24 * 60 * 60 * 1000),
                    purchasePrice = 1100.0,
                    warrantyExpiryDate = now + (365L * 24 * 60 * 60 * 1000),
                    shelf = "A3",
                    bin = "03",
                    createdAt = now,
                    updatedAt = now
                )
            )
            
            // Insert products
            sampleProducts.forEach { product ->
                try {
                    productRepository.insertProduct(product)
                } catch (_: Exception) {
                    // Ignore duplicates (if already exists)
                    Log.w("ProductSeeder", "Product ${product.serialNumber} might already exist")
                }
            }
            
            Log.d("ProductSeeder", "Successfully seeded ${sampleProducts.size} sample products")
            
        } catch (e: Exception) {
            Log.e("ProductSeeder", "Failed to seed products", e)
        }
    }
}
