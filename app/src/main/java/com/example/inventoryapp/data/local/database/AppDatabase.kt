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
        ProductEntity::class,
        CategoryEntity::class,
        PackageEntity::class,
        PackageProductCrossRef::class,
        ScanHistoryEntity::class,
        ProductTemplateEntity::class,
        ScannerSettingsEntity::class,
        ContractorEntity::class,
        BoxEntity::class,
        BoxProductCrossRef::class,
        ImportBackupEntity::class,
        PrinterEntity::class,
        InventoryCountSessionEntity::class,
        InventoryCountItemEntity::class
        ,
        DeviceMovementEntity::class
    ],
    version = 23,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun packageDao(): PackageDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun productTemplateDao(): ProductTemplateDao
    abstract fun scannerSettingsDao(): ScannerSettingsDao
    abstract fun contractorDao(): ContractorDao
    abstract fun boxDao(): BoxDao
    abstract fun importBackupDao(): ImportBackupDao
    abstract fun printerDao(): PrinterDao
    abstract fun inventoryCountDao(): InventoryCountDao
    abstract fun deviceMovementDao(): DeviceMovementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration 1 -> 2: Add ProductTemplate table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `product_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        // Migration 2 -> 3: Add ScannerSettings table and make serialNumber required in products
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create scanner_settings table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `scanner_settings` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `scannerId` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // For products: serialNumber is now required (non-null)
                // Note: Existing products with null SNs will be deleted in destructive migration
                // In production, you'd migrate data carefully
            }
        }

        // Migration 3 -> 4: Ensure unique index on products.serialNumber exists
        // Also remove duplicate non-null serial numbers to satisfy UNIQUE constraint
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove duplicate rows keeping the lowest id for each non-null serialNumber
                database.execSQL(
                    """
                    DELETE FROM products
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM products
                        WHERE serialNumber IS NOT NULL
                        GROUP BY serialNumber
                    )
                    AND serialNumber IS NOT NULL
                    """.trimIndent()
                )

                // Create the unique index if it doesn't exist
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_products_serialNumber
                    ON products(serialNumber)
                    """.trimIndent()
                )
            }
        }

        // Migration 4 -> 5: Add contractors table and contractorId column to packages
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create contractors table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contractors` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `contactPerson` TEXT,
                        `email` TEXT,
                        `phone` TEXT,
                        `address` TEXT,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Add contractorId column to packages table
                database.execSQL("ALTER TABLE packages ADD COLUMN contractorId INTEGER")
            }
        }

        // Migration 5 -> 6: Add description column to products table
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add description column to products table
                database.execSQL("ALTER TABLE products ADD COLUMN description TEXT")
            }
        }

        // Migration 6 -> 7: Add boxes table and box_product_cross_ref table
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create boxes table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `boxes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `warehouseLocation` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create box_product_cross_ref table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `box_product_cross_ref` (
                        `boxId` INTEGER NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`boxId`, `productId`),
                        FOREIGN KEY(`boxId`) REFERENCES `boxes`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indices
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_box_product_cross_ref_boxId` ON `box_product_cross_ref` (`boxId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_box_product_cross_ref_productId` ON `box_product_cross_ref` (`productId`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create import_backups table for undo functionality
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `import_backups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `backupTimestamp` INTEGER NOT NULL,
                        `backupJson` TEXT NOT NULL,
                        `importDescription` TEXT NOT NULL,
                        `productsCount` INTEGER NOT NULL,
                        `packagesCount` INTEGER NOT NULL,
                        `templatesCount` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add requiresSerialNumber column to categories table
                database.execSQL("ALTER TABLE `categories` ADD COLUMN `requiresSerialNumber` INTEGER NOT NULL DEFAULT 1")
                
                // Create "Other" category that doesn't require serial numbers
                database.execSQL("""
                    INSERT INTO `categories` (`name`, `iconResId`, `requiresSerialNumber`, `createdAt`)
                    VALUES ('Other', 0, 0, ${System.currentTimeMillis()})
                """.trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add quantity column to products table for aggregated products (especially "Other" category)
                database.execSQL("ALTER TABLE `products` ADD COLUMN `quantity` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create printers table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `printers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `macAddress` TEXT NOT NULL,
                        `labelWidthMm` INTEGER NOT NULL DEFAULT 50,
                        `labelHeightMm` INTEGER NOT NULL DEFAULT 30,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove label size columns from printers table - let printer use default settings
                // SQLite doesn't support DROP COLUMN, so we need to recreate the table
                
                // Create new printers table without size columns
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `printers_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `macAddress` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Copy data from old table (without size columns)
                database.execSQL("""
                    INSERT INTO `printers_new` (id, name, macAddress, isDefault, createdAt)
                    SELECT id, name, macAddress, isDefault, createdAt FROM `printers`
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE `printers`")
                
                // Rename new table to printers
                database.execSQL("ALTER TABLE `printers_new` RENAME TO `printers`")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Simplify contractors table - remove contactPerson and address, rename notes to description
                
                // Create new contractors table with simplified schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `contractors_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT,
                        `email` TEXT,
                        `description` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Copy data from old table (map notes to description, drop contactPerson and address)
                database.execSQL("""
                    INSERT INTO `contractors_new` (id, name, phone, email, description, createdAt, updatedAt)
                    SELECT id, name, phone, email, notes, createdAt, updatedAt FROM `contractors`
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE `contractors`")
                
                // Rename new table to contractors
                database.execSQL("ALTER TABLE `contractors_new` RENAME TO `contractors`")
            }
        }

        // Migration 13 -> 14: Add label dimension fields to printers table
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns for label dimensions and DPI
                // Default values: 50mm width, null height (continuous roll), 203 DPI
                database.execSQL("""
                    ALTER TABLE `printers` ADD COLUMN `labelWidthMm` INTEGER NOT NULL DEFAULT 50
                """.trimIndent())
                
                database.execSQL("""
                    ALTER TABLE `printers` ADD COLUMN `labelHeightMm` INTEGER DEFAULT NULL
                """.trimIndent())
                
                database.execSQL("""
                    ALTER TABLE `printers` ADD COLUMN `dpi` INTEGER NOT NULL DEFAULT 203
                """.trimIndent())
            }
        }

        // Migration 14 -> 15: Add font size field to printers table
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add font size column with default "small"
                database.execSQL("""
                    ALTER TABLE `printers` ADD COLUMN `fontSize` TEXT NOT NULL DEFAULT 'small'
                """.trimIndent())
            }
        }

        // Migration 15 -> 16: Add printer model field for connection strategy
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add model column with default "GENERIC_ESC_POS"
                database.execSQL("""
                    ALTER TABLE `printers` ADD COLUMN `model` TEXT NOT NULL DEFAULT 'GENERIC_ESC_POS'
                """.trimIndent())
            }
        }

        // Migration 16 -> 17: Add inventory count tables
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create inventory_count_sessions table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `inventory_count_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `status` TEXT NOT NULL,
                        `notes` TEXT
                    )
                """.trimIndent())
                
                // Create inventory_count_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `inventory_count_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `scannedAt` INTEGER NOT NULL,
                        `sequenceNumber` INTEGER NOT NULL,
                        FOREIGN KEY(`sessionId`) REFERENCES `inventory_count_sessions`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for performance
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_inventory_count_items_sessionId` 
                    ON `inventory_count_items` (`sessionId`)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_inventory_count_items_productId` 
                    ON `inventory_count_items` (`productId`)
                """.trimIndent())
            }
        }

        // Migration 17 -> 18: Add returnedAt field to packages table
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add returnedAt column to packages table
                database.execSQL("ALTER TABLE packages ADD COLUMN returnedAt INTEGER")
            }
        }

        // Migration 18 -> 19: Add archived field to packages table
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add archived column to packages table (default false)
                database.execSQL("ALTER TABLE packages ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration 19 -> 20: Add device_movements table and backfill from existing cross-ref tables
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `device_movements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `action` TEXT NOT NULL,
                        `fromContainerType` TEXT,
                        `fromContainerId` INTEGER,
                        `toContainerType` TEXT,
                        `toContainerId` INTEGER,
                        `timestamp` INTEGER NOT NULL,
                        `packageStatus` TEXT,
                        `note` TEXT
                    )
                """.trimIndent())

                database.execSQL("CREATE INDEX IF NOT EXISTS `index_device_movements_product_timestamp` ON device_movements(productId, timestamp DESC)")

                // Backfill: box assignments (use addedAt)
                database.execSQL("""
                    INSERT INTO device_movements (productId, action, toContainerType, toContainerId, timestamp, note)
                    SELECT productId, 'ASSIGN', 'BOX', boxId, addedAt, 'backfill' FROM box_product_cross_ref
                """.trimIndent())

                // Backfill: package assignments (use packages.shippedAt or packages.createdAt)
                database.execSQL("""
                    INSERT INTO device_movements (productId, action, toContainerType, toContainerId, timestamp, note)
                    SELECT ppc.productId, 'ASSIGN', 'PACKAGE', ppc.packageId, COALESCE(pkg.shippedAt, pkg.createdAt), 'backfill'
                    FROM package_product_cross_ref ppc
                    LEFT JOIN packages pkg ON ppc.packageId = pkg.id
                """.trimIndent())
            }
        }

        // Migration 20 -> 21: Add packageCode field to packages table for Google Sheets deduplication
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE packages ADD COLUMN packageCode TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_package_code` ON packages(packageCode)")
            }
        }

        // Migration 21 -> 22: Add deviceId field to products table for fixed device IDs from Google Sheets
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN deviceId TEXT")
            }
        }

        // Migration 22 -> 23: Add configValue field to products table for scanner config values (0-10)
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN configValue INTEGER")
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
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, 
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, 
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, 
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                        MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                        MIGRATION_21_22, MIGRATION_22_23
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
