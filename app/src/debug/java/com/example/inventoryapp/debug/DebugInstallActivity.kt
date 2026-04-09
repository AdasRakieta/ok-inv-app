package com.example.inventoryapp.debug

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.repository.EquipmentDataSeeder
import com.example.inventoryapp.data.seeder.ProductDataSeeder
import com.example.inventoryapp.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug install activity — appears only in debug builds (src/debug).
 * Presents option to populate app with sample/test data for faster testing.
 */
class DebugInstallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = applicationContext as InventoryApplication

        AlertDialog.Builder(this)
            .setTitle("Instalacja (debug)")
            .setMessage("Czy chcesz zainstalować aplikację z przykładowymi danymi testowymi?\n\n(tylko w buildzie debug)")
            .setPositiveButton("Tak — z danymi") { _, _ ->
                // Seed data on background thread
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Seed equipment/employees
                        EquipmentDataSeeder.seedSampleData(this@DebugInstallActivity)

                        // Seed products (uses InventoryApplication repositories)
                        ProductDataSeeder.seedSampleProducts(app)

                        runOnUiThread {
                            Toast.makeText(this@DebugInstallActivity, "Dane testowe zainstalowane", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@DebugInstallActivity, MainActivity::class.java))
                            finish()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@DebugInstallActivity, "Błąd podczas seedowania danych: ${e.message}", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@DebugInstallActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }
            .setNegativeButton("Nie — bez danych") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
