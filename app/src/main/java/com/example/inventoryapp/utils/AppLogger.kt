package com.example.inventoryapp.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized logging utility for the Inventory App
 * Logs to both Logcat and file system at /Documents/inventory/logs/{date}.txt
 */
object AppLogger {
    private const val TAG = "InventoryApp"
    private const val LOG_DIR = "inventory/logs"
    
    // Coroutine scope for async file logging
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    enum class Level {
        DEBUG, INFO, WARNING, ERROR
    }
    
    /**
     * Get the logs directory in Documents
     */
    fun getLogsDirectory(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val logsDir = File(documentsDir, LOG_DIR)
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        return logsDir
    }
    
    /**
     * Get today's log file
     */
    private fun getLogFile(): File {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val logsDir = getLogsDirectory()
        return File(logsDir, "$today.txt")
    }
    
    /**
     * Log a message
     */
    fun log(level: Level, tag: String = TAG, message: String, throwable: Throwable? = null) {
        // Log to Logcat
        when (level) {
            Level.DEBUG -> Log.d(tag, message, throwable)
            Level.INFO -> Log.i(tag, message, throwable)
            Level.WARNING -> Log.w(tag, message, throwable)
            Level.ERROR -> Log.e(tag, message, throwable)
        }
        
        // Log to file asynchronously
        logScope.launch {
            try {
                val logFile = getLogFile()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logEntry = buildString {
                    append("[$timestamp] [${level.name}] [$tag] $message")
                    if (throwable != null) {
                        append("\n")
                        append(throwable.stackTraceToString())
                    }
                    append("\n")
                }
                
                FileWriter(logFile, true).use { writer ->
                    writer.append(logEntry)
                }
            } catch (e: Exception) {
                // If file logging fails, at least log to Logcat
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
    }
    
    /**
     * Convenience methods
     */
    fun d(tag: String = TAG, message: String) = log(Level.DEBUG, tag, message)
    fun i(tag: String = TAG, message: String) = log(Level.INFO, tag, message)
    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) =
        log(Level.WARNING, tag, message, throwable)
    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) =
        log(Level.ERROR, tag, message, throwable)
    
    /**
     * Log action events
     */
    fun logAction(action: String, details: String = "") {
        val message = if (details.isEmpty()) action else "$action: $details"
        log(Level.INFO, "Action", message)
    }
    
    /**
     * Log errors
     */
    fun logError(operation: String, error: Throwable) {
        log(Level.ERROR, "Error", "Operation '$operation' failed", error)
    }
    
    /**
     * Clean up old log files (keep last 30 days)
     */
    suspend fun cleanupOldLogs(daysToKeep: Int = 30) = withContext(Dispatchers.IO) {
        try {
            val logsDir = getLogsDirectory()
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            
            logsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    file.delete()
                    Log.d(TAG, "Deleted old log file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
}
