package com.example.inventoryapp

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.dao.DeviceMovementDao
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.DeviceMovementRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.ImportBackupRepository
import com.example.inventoryapp.data.repository.BackfillRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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

    val deviceMovementRepository by lazy { DeviceMovementRepository(database.deviceMovementDao()) }
    val boxRepository by lazy { BoxRepository(database.boxDao(), database.productDao(), database.packageDao(), deviceMovementRepository) }
    val packageRepository by lazy { PackageRepository(database.packageDao(), database.productDao(), database.boxDao(), deviceMovementRepository) }
    val productRepository by lazy { ProductRepository(database.productDao()) }
    val importBackupRepository by lazy { ImportBackupRepository(database.importBackupDao()) }

    override fun onCreate() {
        super.onCreate()
        // Application initialization
        // Debug: run backfill if a flag file is present in filesDir named "run_backfill_now"
        try {
            if (BuildConfig.DEBUG) {
                val flag = File(filesDir, "run_backfill_now")
                if (flag.exists()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val runner = BackfillRunner(database.productDao(), database.packageDao(), database.boxDao(), deviceMovementRepository)
                            runner.runFullBackfill { msg -> android.util.Log.i("BackfillRunner", msg) }
                        } catch (e: Exception) {
                            android.util.Log.e("InventoryApp", "Backfill failed", e)
                        }
                        flag.delete()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("InventoryApp", "Backfill trigger check failed", e)
        }
    }
}
