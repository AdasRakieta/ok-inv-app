package com.example.inventoryapp.utils

import com.example.inventoryapp.ui.tools.ExportData
import java.util.Locale

/**
 * Utility class for generating ZPL (Zebra Programming Language) commands
 * for printing database content on Zebra printers like ZQ310 Plus
 */
object ZPLPrinterHelper {
    
    /**
     * Generates ZPL commands from database export data
     * @param exportData The complete database export
     * @return ZPL command string ready to send to printer
     */
    fun generateDatabaseZPL(exportData: ExportData): String {
        val zpl = StringBuilder()
        
        // Start ZPL command
        zpl.append("^XA\n")
        
        // Header
        zpl.append("^FO50,50^A0N,40,40^FDINVENTORY DATABASE^FS\n")
        zpl.append("^FO50,100^A0N,30,30^FDProducts: ${exportData.products.size}  ")
        zpl.append("Packages: ${exportData.packages.size}  ")
        zpl.append("Templates: ${exportData.templates.size}^FS\n")
        zpl.append("^FO50,150^GB600,2,2^FS\n") // Horizontal line
        
        var yPos = 200
        
        // Products section
        if (exportData.products.isNotEmpty()) {
            zpl.append("^FO50,$yPos^A0N,30,30^FDPRODUCTS:^FS\n")
            yPos += 40
            
            exportData.products.take(10).forEachIndexed { index, product ->
                zpl.append("^FO50,$yPos^A0N,25,25^FD${index + 1}. ${product.name}^FS\n")
                yPos += 30
                zpl.append("^FO70,$yPos^A0N,20,20^FDSN: ${product.serialNumber}^FS\n")
                yPos += 30
                
                if (yPos > 1100) { // Page break for Zebra label
                    zpl.append("^XZ\n^XA\n")
                    yPos = 50
                }
            }
            
            if (exportData.products.size > 10) {
                zpl.append("^FO50,$yPos^A0N,20,20^FD... and ${exportData.products.size - 10} more^FS\n")
                yPos += 40
            }
        }
        
        // Packages section
        if (exportData.packages.isNotEmpty()) {
            yPos += 20
            zpl.append("^FO50,$yPos^GB600,2,2^FS\n") // Separator
            yPos += 20
            zpl.append("^FO50,$yPos^A0N,30,30^FDPACKAGES:^FS\n")
            yPos += 40
            
            exportData.packages.take(5).forEachIndexed { index, pkg ->
                zpl.append("^FO50,$yPos^A0N,25,25^FD${index + 1}. ${pkg.name}^FS\n")
                yPos += 30
                val s = pkg.status
                val lower = s.toLowerCase(Locale.getDefault())
                val readableStatus = if (lower.isNotEmpty()) lower.substring(0, 1).toUpperCase(Locale.getDefault()) + lower.substring(1) else lower
                zpl.append("^FO70,$yPos^A0N,20,20^FDStatus: $readableStatus^FS\n")
                yPos += 30
                
                if (yPos > 1100) {
                    zpl.append("^XZ\n^XA\n")
                    yPos = 50
                }
            }
            
            if (exportData.packages.size > 5) {
                zpl.append("^FO50,$yPos^A0N,20,20^FD... and ${exportData.packages.size - 5} more^FS\n")
                yPos += 40
            }
        }
        
        // Templates section
        if (exportData.templates.isNotEmpty()) {
            yPos += 20
            zpl.append("^FO50,$yPos^GB600,2,2^FS\n") // Separator
            yPos += 20
            zpl.append("^FO50,$yPos^A0N,30,30^FDTEMPLATES:^FS\n")
            yPos += 40
            
            exportData.templates.take(5).forEachIndexed { index, template ->
                zpl.append("^FO50,$yPos^A0N,25,25^FD${index + 1}. ${template.name}^FS\n")
                yPos += 35
            }
        }
        
        // End ZPL command
        zpl.append("^XZ\n")
        
        return zpl.toString()
    }
}
