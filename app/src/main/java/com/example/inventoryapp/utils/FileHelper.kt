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
    private const val PUBLIC_APP_DIR = "ok_inv_app"
    
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
     * Public Documents/ok_inv_app directory (visible in device file explorer)
     */
    fun getPublicAppDirectory(): File {
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(publicDocs, PUBLIC_APP_DIR)
        if (!appDir.exists()) {
            try {
                appDir.mkdirs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return appDir
    }

    /**
     * Get or create a public export subdirectory under Documents/ok_inv_app
     */
    fun getPublicExportSubdir(subdirName: String): File? {
        return try {
            val appDir = getPublicAppDirectory()
            val sub = File(appDir, subdirName)
            if (!sub.exists()) sub.mkdirs()
            sub
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Ensure public export folders exist. Safe to call on every start.
     */
    fun ensurePublicExportFoldersCreated() {
        if (!isExternalStorageWritable()) return
        try {
            getPublicExportSubdir("Eksport Ean Produkty")
            getPublicExportSubdir("Eksport QR Lokalizacja")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sanitize a string to be safe for filenames.
     */
    fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_')
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
