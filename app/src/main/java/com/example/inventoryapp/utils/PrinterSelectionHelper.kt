package com.example.inventoryapp.utils

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.PrinterEntity
import com.example.inventoryapp.data.repository.PrinterRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * Helper class for printer selection dialogs
 */
object PrinterSelectionHelper {
    
    /**
     * Show a printer selection dialog
     * @param fragment The fragment showing the dialog
     * @param onPrinterSelected Callback when printer is selected
     */
    fun showPrinterSelectionDialog(
        fragment: Fragment,
        onPrinterSelected: (PrinterEntity) -> Unit
    ) {
        val context = fragment.requireContext()
        val database = AppDatabase.getDatabase(context)
        
        fragment.lifecycleScope.launch {
            try {
                val printerList = withContext(Dispatchers.IO) {
                    // Get all printers - use first() to get single emission
                    database.printerDao().getAllPrinters().first()
                }
                
                if (printerList.isEmpty()) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("No Printers")
                        .setMessage("Please configure a printer in Settings first")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    showDialog(context, printerList, onPrinterSelected)
                }
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Error")
                    .setMessage("Failed to load printers: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    /**
     * Get the default printer or show selection dialog
     * @param fragment The fragment
     * @param onPrinterSelected Callback when printer is selected
     */
    fun getDefaultOrSelectPrinter(
        fragment: Fragment,
        onPrinterSelected: (PrinterEntity) -> Unit
    ) {
        val database = AppDatabase.getDatabase(fragment.requireContext())
        val printerRepository = PrinterRepository(database.printerDao())
        
        fragment.lifecycleScope.launch {
            try {
                val defaultPrinter = withContext(Dispatchers.IO) {
                    printerRepository.getDefaultPrinterSync()
                }
                
                if (defaultPrinter != null) {
                    onPrinterSelected(defaultPrinter)
                } else {
                    // No default printer, show selection dialog
                    showPrinterSelectionDialog(fragment, onPrinterSelected)
                }
            } catch (e: Exception) {
                MaterialAlertDialogBuilder(fragment.requireContext())
                    .setTitle("Error")
                    .setMessage("Failed to load printer: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun showDialog(
        context: Context,
        printers: List<PrinterEntity>,
        onPrinterSelected: (PrinterEntity) -> Unit
    ) {
        val printerNames = printers.map { printer ->
            if (printer.isDefault) {
                "${printer.name} (Default)"
            } else {
                printer.name
            }
        }.toTypedArray()
        
        MaterialAlertDialogBuilder(context)
            .setTitle("Select Printer")
            .setItems(printerNames) { _, which ->
                onPrinterSelected(printers[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
