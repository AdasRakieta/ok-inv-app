package com.example.inventoryapp.data.repository

/**
 * Result of upload operation to Google Sheets
 */
data class UploadResult(
    val totalUploaded: Int,
    val insertCount: Int,
    val updateCount: Int,
    val errorCount: Int,
    val message: String
)
