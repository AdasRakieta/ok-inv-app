package com.example.inventoryapp.utils

import android.content.Context
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
    fun getAppDirectory(context: Context): File {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val appDir = File(documentsDir, APP_DIR)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }
    
    /**
     * Get the Documents/inventory/exports directory
     */
    fun getExportsDirectory(context: Context): File {
        val appDir = getAppDirectory(context)
        val exportsDir = File(appDir, EXPORTS_DIR)
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }
        return exportsDir
    }
    
    /**
     * Get the Documents/inventory/logs directory
     */
    fun getLogsDirectory(context: Context): File {
        val appDir = getAppDirectory(context)
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
    fun getAvailableSpace(context: Context): Long {
        return try {
            val exportsDir = getExportsDirectory(context)
            exportsDir.usableSpace
        } catch (e: Exception) {
            0L
        }
    }
}
