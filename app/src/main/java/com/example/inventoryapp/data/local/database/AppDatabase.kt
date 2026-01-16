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
        // Internal equipment domain
        EmployeeEntity::class,
        EquipmentEntity::class,
        EquipmentAssignmentEntity::class
    ],
    version = 25,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Equipment domain DAOs
    abstract fun employeeDao(): EmployeeDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun equipmentAssignmentDao(): EquipmentAssignmentDao

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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory_database"
                )
                    .addMigrations(MIGRATION_24_25)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
