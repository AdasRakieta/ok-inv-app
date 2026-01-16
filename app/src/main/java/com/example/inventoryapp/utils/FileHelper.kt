package com.example.inventoryapp.utils

import android.os.Environment
import java.io.File

/**
 * Helper for managing file system paths for the Inventory App
 */
object FileHelper {
    private const val APP_DIR = "inventory"
    private const val EXPORTS_DIR = "exports"
    private const val LOGS_DIR = "logs"
    
    /**
     * Get the base Documents/inventory directory
     */
    fun getAppDirectory(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(documentsDir, APP_DIR)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }
    
    /**
     * Get the Documents/inventory/exports directory
     */
    fun getExportsDirectory(): File {
        val appDir = getAppDirectory()
        val exportsDir = File(appDir, EXPORTS_DIR)
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }
        return exportsDir
    }
    
    /**
     * Get the Documents/inventory/logs directory
     */
    fun getLogsDirectory(): File {
        val appDir = getAppDirectory()
        val logsDir = File(appDir, LOGS_DIR)
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        return logsDir
    }
    
    /**
     * Check if external storage is writable
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Get available space in bytes
     */
    fun getAvailableSpace(): Long {
        return try {
            val exportsDir = getExportsDirectory()
            exportsDir.usableSpace
        } catch (e: Exception) {
            0L
        }
    }
}
