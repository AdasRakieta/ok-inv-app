package com.example.inventoryapp

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.inventoryapp.data.repository.EquipmentDataSeeder
import com.example.inventoryapp.data.seeder.ProductDataSeeder

/**
 * Demo variant Application — automatically seeds sample data on startup.
 * This class is used only in the `demo` product flavor (src/demo).
 */
class DemoInventoryApplication : InventoryApplication() {
    override fun onCreate() {
        super.onCreate()
        Log.i("DemoInventoryApp", "Demo flavor detected — seeding sample data")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                EquipmentDataSeeder.seedSampleData(this@DemoInventoryApplication)
            } catch (e: Exception) {
                Log.e("DemoInventoryApp", "Equipment seed failed", e)
            }

            try {
                ProductDataSeeder.seedSampleProducts(this@DemoInventoryApplication)
            } catch (e: Exception) {
                Log.e("DemoInventoryApp", "Product seed failed", e)
            }
        }
    }
}
