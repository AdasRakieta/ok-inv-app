package com.example.inventoryapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.inventoryapp.data.local.dao.*
import com.example.inventoryapp.data.local.entities.*

@Database(
    entities = [
        // Employees
        EmployeeEntity::class,
        // Departments
        com.example.inventoryapp.data.local.entities.DepartmentEntity::class,
        
        // Products
        ProductEntity::class,
        CategoryEntity::class,
        ProductTemplateEntity::class,
        
        // Equipment (legacy - keeping for compatibility)
        EquipmentEntity::class,
        EquipmentAssignmentEntity::class,
        
        // Warehouse
        WarehouseLocationEntity::class,
        
        // Orders
        OrderEntity::class,
        OrderItemEntity::class,
        SupplierEntity::class,
        
        // Inventory Management
        InventoryCountEntity::class,
        InventoryCountItemEntity::class,
        
        // Tracking
        ScanHistoryEntity::class
    ],
    version = 32,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Employee DAO
    abstract fun employeeDao(): EmployeeDao
    
    // Department DAO
    abstract fun departmentDao(): com.example.inventoryapp.data.local.dao.DepartmentDao
    
    // Product DAOs
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productTemplateDao(): ProductTemplateDao
    
    // Equipment DAOs (legacy)
    abstract fun equipmentDao(): EquipmentDao
    abstract fun equipmentAssignmentDao(): EquipmentAssignmentDao
    
    // Warehouse DAOs
    abstract fun warehouseLocationDao(): WarehouseLocationDao
    
    // Order DAOs
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun supplierDao(): SupplierDao
    
    // Inventory Count DAOs
    abstract fun inventoryCountDao(): InventoryCountDao
    abstract fun inventoryCountItemDao(): InventoryCountItemDao
    
    // Tracking DAOs
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration 24 -> 25: Remove warehouse inventory domain (destructive migration)
        // This migration drops all warehouse tables and keeps only equipment domain
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop all warehouse inventory tables
                database.execSQL("DROP TABLE IF EXISTS products")
                database.execSQL("DROP TABLE IF EXISTS categories")
                database.execSQL("DROP TABLE IF EXISTS packages")
                database.execSQL("DROP TABLE IF EXISTS package_product_cross_ref")
                database.execSQL("DROP TABLE IF EXISTS scan_history")
                database.execSQL("DROP TABLE IF EXISTS product_templates")
                database.execSQL("DROP TABLE IF EXISTS scanner_settings")
                database.execSQL("DROP TABLE IF EXISTS contractors")
                database.execSQL("DROP TABLE IF EXISTS boxes")
                database.execSQL("DROP TABLE IF EXISTS box_product_cross_ref")
                database.execSQL("DROP TABLE IF EXISTS import_backups")
                database.execSQL("DROP TABLE IF EXISTS printers")
                database.execSQL("DROP TABLE IF EXISTS inventory_count_sessions")
                database.execSQL("DROP TABLE IF EXISTS inventory_count_items")
                database.execSQL("DROP TABLE IF EXISTS device_movements")
                
                // Equipment tables already exist from Migration 23->24, no need to recreate
            }
        }
        
        // Migration 25 -> 26: Add new complete inventory management system
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        color TEXT,
                        icon TEXT,
                        createdAt INTEGER NOT NULL,
                        UNIQUE(name)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
                
                // Create products table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        serialNumber TEXT NOT NULL,
                        categoryId INTEGER,
                        warehouseLocationId INTEGER,
                        shelf TEXT,
                        bin TEXT,
                        description TEXT,
                        manufacturer TEXT,
                        model TEXT,
                        status TEXT NOT NULL,
                        condition TEXT,
                        purchaseDate INTEGER,
                        purchasePrice REAL,
                        warrantyExpiryDate INTEGER,
                        assignedToEmployeeId INTEGER,
                        assignmentDate INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        notes TEXT,
                        movementHistory TEXT,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                    )
                """)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_serialNumber ON products(serialNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_products_categoryId ON products(categoryId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_products_warehouseLocationId ON products(warehouseLocationId)")
                
                // Create warehouse_locations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS warehouse_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        zone TEXT,
                        type TEXT NOT NULL,
                        description TEXT,
                        capacity INTEGER,
                        currentOccupancy INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        UNIQUE(code)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_warehouse_locations_code ON warehouse_locations(code)")
                
                // Create product_templates table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS product_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        defaultManufacturer TEXT,
                        defaultModel TEXT,
                        defaultDescription TEXT,
                        serialNumberPattern TEXT,
                        serialNumberPrefix TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE,
                        UNIQUE(name)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_product_templates_name ON product_templates(name)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_product_templates_categoryId ON product_templates(categoryId)")
                
                // Create suppliers table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS suppliers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        contactPerson TEXT,
                        email TEXT,
                        phone TEXT,
                        address TEXT,
                        website TEXT,
                        taxId TEXT,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        UNIQUE(name)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_suppliers_name ON suppliers(name)")
                
                // Create orders table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderNumber TEXT NOT NULL,
                        supplierId INTEGER,
                        status TEXT NOT NULL,
                        orderDate INTEGER NOT NULL,
                        expectedDeliveryDate INTEGER,
                        actualDeliveryDate INTEGER,
                        totalAmount REAL,
                        currency TEXT NOT NULL,
                        notes TEXT,
                        trackingNumber TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        UNIQUE(orderNumber)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_orderNumber ON orders(orderNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_supplierId ON orders(supplierId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_orders_status ON orders(status)")
                
                // Create order_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        orderId INTEGER NOT NULL,
                        productId INTEGER,
                        productName TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice REAL,
                        totalPrice REAL,
                        receivedQuantity INTEGER NOT NULL,
                        notes TEXT,
                        FOREIGN KEY(orderId) REFERENCES orders(id) ON DELETE CASCADE,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE SET NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_orderId ON order_items(orderId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_order_items_productId ON order_items(productId)")
                
                // Create scan_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS scan_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scannedValue TEXT NOT NULL,
                        scanType TEXT NOT NULL,
                        context TEXT,
                        productId INTEGER,
                        employeeId INTEGER,
                        timestamp INTEGER NOT NULL,
                        success INTEGER NOT NULL,
                        errorMessage TEXT
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_scan_history_scannedValue ON scan_history(scannedValue)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_scan_history_timestamp ON scan_history(timestamp)")
                
                // Create inventory_counts table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_counts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        status TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        countedById INTEGER,
                        totalExpectedItems INTEGER NOT NULL,
                        totalCountedItems INTEGER NOT NULL,
                        totalMissingItems INTEGER NOT NULL,
                        totalExtraItems INTEGER NOT NULL,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        UNIQUE(sessionId)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_counts_sessionId ON inventory_counts(sessionId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_counts_status ON inventory_counts(status)")
                
                // Create inventory_count_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_count_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        countId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        expectedQuantity INTEGER NOT NULL,
                        actualQuantity INTEGER NOT NULL,
                        variance INTEGER NOT NULL,
                        scannedAt INTEGER,
                        notes TEXT,
                        status TEXT NOT NULL,
                        FOREIGN KEY(countId) REFERENCES inventory_counts(id) ON DELETE CASCADE,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE,
                        UNIQUE(countId, productId)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_count_items_countId ON inventory_count_items(countId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_count_items_productId ON inventory_count_items(productId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_count_items_countId_productId ON inventory_count_items(countId, productId)")
            }
        }
        
        // Migration 26 -> 27: Fix index_products_serialNumber to be UNIQUE
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop existing non-unique index
                database.execSQL("DROP INDEX IF EXISTS index_products_serialNumber")
                // Create unique index
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_serialNumber ON products(serialNumber)")
            }
        }
        
        // Migration 27 -> 28: Add customId field to products table
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add customId column (nullable, unique)
                database.execSQL("ALTER TABLE products ADD COLUMN customId TEXT")
                // Create unique index for customId (excluding NULL values)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_customId ON products(customId) WHERE customId IS NOT NULL")
            }
        }
        
        // Migration 28 -> 29: Update employees table structure
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new employees table with updated structure
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS employees_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firstName TEXT NOT NULL,
                        lastName TEXT NOT NULL,
                        email TEXT,
                        phone TEXT,
                        department TEXT,
                        position TEXT,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // Migrate data from old table (split name into firstName and lastName)
                database.execSQL("""
                    INSERT INTO employees_new (id, firstName, lastName, email, phone, department, position, notes, createdAt, updatedAt)
                    SELECT 
                        id,
                        CASE 
                            WHEN instr(name, ' ') > 0 THEN substr(name, 1, instr(name, ' ') - 1)
                            ELSE name
                        END as firstName,
                        CASE 
                            WHEN instr(name, ' ') > 0 THEN substr(name, instr(name, ' ') + 1)
                            ELSE ''
                        END as lastName,
                        email,
                        NULL as phone,
                        department,
                        position,
                        NULL as notes,
                        createdAt,
                        updatedAt
                    FROM employees
                """)
                
                // Drop old table and rename new one
                database.execSQL("DROP TABLE employees")
                database.execSQL("ALTER TABLE employees_new RENAME TO employees")
                
                // Create unique index for email
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_employees_email ON employees(email) WHERE email IS NOT NULL")
            }
        }

        // Migration 29 -> 30: Add movementHistory to products
        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN movementHistory TEXT")
            }
        }

        // Migration 30 -> 31: Create departments table
        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS departments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        UNIQUE(name)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_departments_name ON departments(name)")
            }
        }

        // Migration 31 -> 32: Add parentId to categories for hierarchical categories
        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add parentId column to categories (nullable)
                database.execSQL("ALTER TABLE categories ADD COLUMN parentId INTEGER")
                // Create index to speed up parent lookups
                database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_parentId ON categories(parentId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory_database"
                )
                    .addMigrations(
                            MIGRATION_24_25,
                            MIGRATION_25_26,
                            MIGRATION_26_27,
                            MIGRATION_27_28,
                            MIGRATION_28_29,
                            MIGRATION_29_30,
                            MIGRATION_30_31,
                            MIGRATION_31_32
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
