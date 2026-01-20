package com.example.inventoryapp.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Represents a single row from Google Sheets API response
 * Maps actual Polish field names from the API
 * 
 * Package Sync Mapping:
 * - Kod → Package name (products grouped by this field)
 * - Serial Number Field → Product serial number (unique product identifier)
 *   - "Skanery" for scanner sheets
 *   - "Drukarki" for printer sheet
 *   - "Stacje do drukarek" for printer docking station sheet
 *   - "Stacje dokujące" for scanner docking station sheet
 * - Status → Package status (mapped to PackageEntity status)
 * - Nazwa + sheet name → Package description
 * 
 * v1.24.17: Added alternate field names for serial numbers to support all 5 sheet types
 */
data class GoogleSheetItem(
    @SerializedName("Urządzenie") val urzadzenie: String? = null,   // Device type (e.g., "Skaner TC58E")
    @SerializedName(value = "Skanery", alternate = ["Drukarki", "Stacje do drukarek", "Stacje dokujące"])
    val serialNumber: String,                                        // Product serial number (multi-sheet compatible)
    @SerializedName("Status") val status: String? = null,           // Package status (e.g., "Wydano", "Magazyn")
    @SerializedName("Kod") val kod: String? = null,                 // PACKAGE NAME (group identifier)
    @SerializedName("Nazwa") val nazwa: String? = null,             // Location/store name for description
    @SerializedName("Data wydania") val dataWydania: String? = null,// Issue date
    @SerializedName("Data zwrotu") val dataZwrotu: String? = null,  // Return date
    @SerializedName("Firma") val firma: String? = null,             // Company name
    @SerializedName("Komentarz") val komentarz: String? = null,     // Comments
    @SerializedName("ID") val deviceId: String? = null,             // Fixed device ID (column J)
    @SerializedName("Config") val configValue: Int? = null          // Config value (0-10 range)
)

/**
 * API response wrapper from Google Apps Script
 */
data class ApiResponse(
    val status: String? = null,    // "SUKCES" or "BLAD" (Apps Script GET)
    val success: Boolean? = null,  // true/false (Apps Script POST)
    val message: String? = null    // Success/error message
)

/**
 * Request body for POST operations (update/insert)
 * Matches Google Apps Script expected structure
 */
data class ApiRequest(
    val akcja: String,                    // "update" or "insert"
    val arkusz: String,                   // Sheet name (Skanery, Drukarki, etc.)
    val serialNumber: String? = null,     // For UPDATE: serial number to search for
    val kod: String? = null,              // For UPDATE: optional package code
    val nazwa: String? = null,            // For UPDATE: optional package name
    val status: String? = null,           // For UPDATE: new status
    val miejsce: String? = null,          // For UPDATE: new location
    val dane: InsertData? = null          // For INSERT: data object
)

/**
 * Data object for INSERT operations
 * Contains all fields to be inserted as new row
 */
data class InsertData(
    val serialNumber: String,
    val Urzadzenie: String? = null,
    val Kod: String? = null,
    val Nazwa: String? = null,
    val Status: String? = null,
    val Firma: String? = null,
    val Komentarz: String? = null,
    val Miejsce: String? = null,
    val Kategoria: String? = null,
    val Data: String? = null,
    val Model: String? = null,
    @SerializedName("Data wydania") val dataWydania: String? = null,
    @SerializedName("Data zwrotu") val dataZwrotu: String? = null,
    @SerializedName("ID") val deviceId: String? = null,
    @SerializedName("Config") val configValue: Int? = null
)

/**
 * Bulk upload request - sends multiple operations at once
 */
data class BulkApiRequest(
    val akcja: String = "bulk",
    val operacje: List<BulkOperation>
)

/**
 * Single operation in bulk request
 */
data class BulkOperation(
    val typ: String,                      // "update" or "insert"
    val arkusz: String,                   // Sheet name
    val serialNumber: String,             // Serial number (for both update and insert)
    val kod: String? = null,
    val nazwa: String? = null,
    val status: String? = null,
    val miejsce: String? = null,
    @SerializedName("Data wydania") val dataWydania: String? = null,
    val dane: InsertData? = null
) 