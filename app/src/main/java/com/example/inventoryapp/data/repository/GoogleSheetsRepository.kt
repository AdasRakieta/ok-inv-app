package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.data.remote.GoogleSheetsApiService
import com.example.inventoryapp.data.remote.GoogleSheetItem
import com.example.inventoryapp.data.remote.BulkOperation
import com.example.inventoryapp.data.remote.InsertData
import com.example.inventoryapp.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * Repository for Google Sheets API synchronization
 * Handles bidirectional sync between local Room database and Google Sheets
 */
class GoogleSheetsRepository(
    private val apiService: GoogleSheetsApiService,
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository,
    private val contractorRepository: ContractorRepository,
    private val deviceMovementRepository: DeviceMovementRepository
) {
    
    companion object {
        /**
         * Sheet configuration based on KONFIGURACJA from Google Apps Script
         * Maps sheet names to their column structure and category
         */
        data class SheetConfig(
            val nazwa: String,       // Sheet name
            val colKod: Int,         // Column index for Kod (package name)
            val colStatus: Int,      // Column index for Status
            val category: String     // Category for products in this sheet
        )
        
        private val KONFIGURACJA = listOf(
            SheetConfig("Skanery", 1, 2, "Scanner"),
            SheetConfig("Skanery tc27", 1, 2, "Scanner"),  // TC27 scanners - same category as regular scanners
            SheetConfig("Drukarki", 1, 2, "Printer"),
            SheetConfig("Stacje do drukarek", 1, 2, "Printer Docking Station"),
            SheetConfig("Stacje Dokujące", 1, 2, "Scanner Docking Station")
        )
        
        /**
         * Maps Google Sheets sheet names to CategoryHelper category names
         */
        private val SHEET_TO_CATEGORY_MAP = KONFIGURACJA.associate { it.nazwa to it.category }
        
        /**
         * Reverse mapping: category name → sheet name for uploads
         */
        private val CATEGORY_TO_SHEET_MAP = KONFIGURACJA.associate { it.category to it.nazwa }
    }
    
    /**
     * Download data from all Google Sheets and sync to local database
     * Strategy: Group products by Kod (package name), create/update packages
     * @return Pair<packages added/updated, products added/updated>
     */
    suspend fun downloadAndSync(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var packagesProcessed = 0
        var productsProcessed = 0
        
        try {
            AppLogger.logAction("Google Sheets Sync", "Starting download from API")
            println("[SYNC] Starting Google Sheets sync - checking ${GoogleSheetsApiService.SHEET_NAMES.size} sheets")
            
            for (sheetName in GoogleSheetsApiService.SHEET_NAMES) {
                try {
                    println("[SYNC] Fetching sheet: $sheetName")
                    val items = apiService.fetchSheet(sheetName)
                    println("[SYNC] Received ${items.size} items from $sheetName")
                    AppLogger.d("Sync", "Fetched ${items.size} items from $sheetName")
                    
                    val categoryName = SHEET_TO_CATEGORY_MAP[sheetName] ?: "Other"
                    val categoryId = getCategoryIdByName(categoryName)
                    println("[SYNC] Category for $sheetName: $categoryName (ID: $categoryId)")
                    
                    // Filter out invalid serial numbers (like "Total 8", summaries, etc.)
                    val validItems = items.filter { isValidSerialNumber(it.serialNumber) }
                    val invalidItems = items.filter { !isValidSerialNumber(it.serialNumber) }
                    
                    if (invalidItems.isNotEmpty()) {
                        println("[SYNC] Skipped ${invalidItems.size} invalid items (summaries/totals): ${invalidItems.map { it.serialNumber }.take(5)}")
                    }
                    
                    // Split items into two groups:
                    // 1. Items WITH Kod → group into packages
                    // 2. Items WITHOUT Kod → add as standalone products
                    val itemsWithCode = validItems.filter { it.kod?.isNotBlank() == true }
                    val itemsWithoutCode = validItems.filter { it.kod == null || it.kod.isBlank() }
                    
                    println("[SYNC] Found ${itemsWithCode.size} items with package code, ${itemsWithoutCode.size} standalone items")
                    
                    // Group items by Kod (unique package identifier)
                    // Use Nazwa (location name) as the display name for the package
                    val packageGroups = itemsWithCode.groupBy { it.kod!! }
                    
                    println("[SYNC] Grouped into ${packageGroups.size} packages in $sheetName")
                    AppLogger.d("Sync", "Found ${packageGroups.size} packages in $sheetName")
                    
                    for ((packageCode, packageItems) in packageGroups) {
                        // Use Nazwa (location name) as the package display name
                        val packageName = packageItems.firstOrNull()?.nazwa?.takeIf { it.isNotBlank() } 
                            ?: packageCode  // Fallback to code if nazwa is empty
                        
                        println("[SYNC] Processing package: $packageName (code: $packageCode) with ${packageItems.size} products")
                        
                        // Get status from first item (all items with same Kod should have same status)
                        val statusFromSheet = packageItems.firstOrNull()?.status
                        val packageStatus = mapStatusToPackageStatus(statusFromSheet)
                        println("[SYNC] Package $packageName status: $statusFromSheet -> $packageStatus")
                        
                        // Build package description from Nazwa and sheet name
                        val locationName = packageItems.firstOrNull()?.nazwa ?: ""
                        val description = if (locationName.isNotBlank()) {
                            "$sheetName - $locationName"
                        } else {
                            sheetName
                        }
                        
                        // Create/get contractor from Firma field
                        val firmaName = packageItems.firstOrNull()?.firma?.takeIf { it.isNotBlank() }
                        var contractorId: Long? = null
                        if (!firmaName.isNullOrBlank()) {
                            try {
                                val existingContractor = contractorRepository.getContractorByName(firmaName)
                                contractorId = if (existingContractor != null) {
                                    println("[SYNC] Contractor exists: $firmaName (ID: ${existingContractor.id})")
                                    existingContractor.id
                                } else {
                                    val newContractor = ContractorEntity(name = firmaName)
                                    val newId = contractorRepository.insertContractor(newContractor)
                                    println("[SYNC] Created new contractor: $firmaName (ID: $newId)")
                                    newId
                                }
                            } catch (e: Exception) {
                                println("[SYNC] ERROR creating contractor $firmaName: ${e.message}")
                                AppLogger.logError("Contractor creation", e)
                            }
                        }
                        
                        // Parse dates from first item (all items with same Kod should have same dates)
                        val firstItem = packageItems.firstOrNull()
                        val shippedAt = parseIsoDate(firstItem?.dataWydania)
                        val returnedAt = parseIsoDate(firstItem?.dataZwrotu)
                        
                        if (shippedAt != null) {
                            println("[SYNC] Package $packageName shipped at: ${firstItem?.dataWydania}")
                        }
                        if (returnedAt != null) {
                            println("[SYNC] Package $packageName returned at: ${firstItem?.dataZwrotu}")
                        }
                        
                        // Check if package exists by packageCode (unique identifier for deduplication)
                        val existingPackage = packageRepository.getPackageByCode(packageCode)
                        
                        val packageId = if (existingPackage != null) {
                            // Update existing package
                            val updatedPackage = existingPackage.copy(
                                name = packageName,  // Update name from Nazwa field
                                status = packageStatus,
                                contractorId = contractorId ?: existingPackage.contractorId,
                                shippedAt = shippedAt ?: existingPackage.shippedAt,
                                returnedAt = returnedAt ?: existingPackage.returnedAt
                                // Keep createdAt unchanged
                            )
                            packageRepository.updatePackage(updatedPackage)
                            println("[SYNC] Updated package: $packageName (code: $packageCode, ID: ${existingPackage.id})")
                            packagesProcessed++
                            existingPackage.id
                        } else {
                            // Create new package
                            val newPackage = PackageEntity(
                                name = packageName,
                                packageCode = packageCode,  // Store Kod field for deduplication
                                status = packageStatus,
                                contractorId = contractorId,
                                createdAt = System.currentTimeMillis(),
                                shippedAt = shippedAt,
                                returnedAt = returnedAt
                            )
                            val id = packageRepository.insertPackage(newPackage)
                            println("[SYNC] Created new package: $packageName (code: $packageCode, ID: $id)")
                            packagesProcessed++
                            id
                        }
                        
                        // Now add/update products and assign to package
                        for (item in packageItems) {
                            val serialNumber = item.serialNumber
                            val productName = item.urzadzenie?.takeIf { it.isNotBlank() } ?: categoryName
                            
                            // Build product description
                            val productDesc = buildString {
                                item.firma?.takeIf { it.isNotBlank() }?.let { append("Firma: $it | ") }
                                item.komentarz?.takeIf { it.isNotBlank() }?.let { append("Komentarz: $it") }
                            }
                            
                            // Check if product exists
                            val existingProduct = productRepository.getProductBySerialNumber(serialNumber)
                            
                            val productId = if (existingProduct != null) {
                                // Update existing product
                                val updatedProduct = existingProduct.copy(
                                    name = productName,
                                    categoryId = categoryId,
                                    description = productDesc,
                                    deviceId = item.deviceId?.takeIf { it.isNotBlank() },
                                    configValue = item.configValue?.takeIf { it in 0..10 },
                                    updatedAt = System.currentTimeMillis()
                                )
                                productRepository.updateProduct(updatedProduct)
                                existingProduct.id
                            } else {
                                // Add new product
                                val newProduct = ProductEntity(
                                    name = productName,
                                    categoryId = categoryId,
                                    serialNumber = serialNumber,
                                    description = productDesc,
                                    deviceId = item.deviceId?.takeIf { it.isNotBlank() },
                                    configValue = item.configValue?.takeIf { it in 0..10 },
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                productRepository.insertProduct(newProduct)
                            }
                            
                            // Add product to package
                            try {
                                packageRepository.addProductToPackage(packageId, productId)
                                productsProcessed++
                                println("[SYNC] Added/updated product $serialNumber to package $packageName")
                            } catch (e: Exception) {
                                println("[SYNC] ERROR adding product $serialNumber to package: ${e.message}")
                                AppLogger.logError("Add product to package", e)
                            }
                        }
                    }
                    
                    // Process standalone products (without package code)
                    println("[SYNC] Processing ${itemsWithoutCode.size} standalone products in $sheetName")
                    for (item in itemsWithoutCode) {
                        val serialNumber = item.serialNumber
                        val productName = item.urzadzenie?.takeIf { it.isNotBlank() } ?: categoryName
                        
                        // Build product description
                        val productDesc = buildString {
                            item.nazwa?.takeIf { it.isNotBlank() }?.let { append("Lokalizacja: $it | ") }
                            item.firma?.takeIf { it.isNotBlank() }?.let { append("Firma: $it | ") }
                            item.status?.takeIf { it.isNotBlank() }?.let { append("Status: $it | ") }
                            item.komentarz?.takeIf { it.isNotBlank() }?.let { append("Komentarz: $it") }
                        }.trimEnd('|', ' ')
                        
                        // Check if product exists
                        val existingProduct = productRepository.getProductBySerialNumber(serialNumber)
                        
                        if (existingProduct != null) {
                            // Update existing standalone product
                            val updatedProduct = existingProduct.copy(
                                name = productName,
                                categoryId = categoryId,
                                description = productDesc,
                                deviceId = item.deviceId?.takeIf { it.isNotBlank() },
                                configValue = item.configValue?.takeIf { it in 0..10 },
                                updatedAt = System.currentTimeMillis()
                            )
                            productRepository.updateProduct(updatedProduct)
                            println("[SYNC] Updated standalone product: $serialNumber")
                        } else {
                            // Add new standalone product
                            val newProduct = ProductEntity(
                                name = productName,
                                categoryId = categoryId,
                                serialNumber = serialNumber,
                                description = productDesc,
                                deviceId = item.deviceId?.takeIf { it.isNotBlank() },
                                configValue = item.configValue?.takeIf { it in 0..10 },
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            productRepository.insertProduct(newProduct)
                            println("[SYNC] Created standalone product: $serialNumber")
                        }
                        productsProcessed++
                    }
                } catch (e: Exception) {
                    println("[SYNC] ERROR syncing sheet $sheetName: ${e.message}")
                    AppLogger.logError("Sync from $sheetName", e)
                    // Continue with other sheets even if one fails
                }
            }
            
            val message = "Download complete: $packagesProcessed packages, $productsProcessed products processed"
            println("[SYNC] $message")
            AppLogger.logAction("Google Sheets Sync", message)
            
        } catch (e: Exception) {
            println("[SYNC] CRITICAL ERROR: ${e.message}")
            AppLogger.logError("Google Sheets downloadAndSync", e)
            throw e
        }
        
        Pair(packagesProcessed, productsProcessed)
    }
    
    /**
     * Upload local products to Google Sheets
     * Only uploads products that have been modified (updatedAt > threshold)
     * @param onlyRecent If true, only upload products modified in last 7 days
     * @return UploadResult with details about upload operation
     */
    suspend fun uploadChanges(onlyRecent: Boolean = true): UploadResult = withContext(Dispatchers.IO) {
        var uploadedCount = 0
        var insertCount = 0
        var updateCount = 0
        var errorCount = 0
        var resultMessage = ""
        
        try {
            AppLogger.logAction("Google Sheets Upload", "Starting upload to API")
            println("[UPLOAD] Starting Google Sheets upload")
            
            val allProducts = productRepository.getAllProducts().first()
            val threshold = if (onlyRecent) {
                System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
            } else {
                0L
            }
            
            // Filter products that were recently updated (if onlyRecent is true)
            val productsToUpload = allProducts.filter { product ->
                product.serialNumber?.isNotBlank() == true && // Must have serial number
                (!onlyRecent || product.updatedAt >= threshold) // Within time threshold if filtering
            }
            
            println("[UPLOAD] Found ${productsToUpload.size} products to upload")
            
            // Build list of bulk operations
            val operations = mutableListOf<BulkOperation>()
            
            for (product in productsToUpload) {
                try {
                    val sheetName = determineTargetSheet(product)
                    val packageInfo = packageRepository.getPackageForProduct(product.id).first()
                    val contractorName = packageInfo?.contractorId?.let { contractorRepository.getContractorById(it).first()?.name }
                    val isFromGoogleSheets = wasDownloadedFromSheets(product)
                    
                    if (isFromGoogleSheets) {
                        // UPDATE operation
                        println("[UPLOAD] Preparing UPDATE: ${product.serialNumber}")
                        updateCount++
                        operations.add(BulkOperation(
                            typ = "update",
                            arkusz = sheetName,
                            serialNumber = product.serialNumber!!,
                            kod = packageInfo?.packageCode,
                            nazwa = packageInfo?.name,
                            status = mapPackageStatusToSheetStatus(packageInfo?.status),
                            miejsce = null,
                            dataWydania = if (packageInfo?.status == "ISSUED") {
                                (packageInfo.shippedAt ?: System.currentTimeMillis()).toString()
                            } else null
                        ))
                    } else {
                        // INSERT operation
                        println("[UPLOAD] Preparing INSERT: ${product.serialNumber}")
                        insertCount++
                        val insertData = InsertData(
                            serialNumber = product.serialNumber!!,
                            Urzadzenie = product.name,
                            Kod = packageInfo?.packageCode,
                            Nazwa = packageInfo?.name,
                            Status = mapPackageStatusToSheetStatus(packageInfo?.status),
                            Firma = contractorName,
                            Komentarz = cleanComment(product.description),
                            dataWydania = if (packageInfo?.status == "ISSUED") {
                                (packageInfo.shippedAt ?: System.currentTimeMillis()).toString()
                            } else packageInfo?.shippedAt?.toString(),
                            dataZwrotu = packageInfo?.returnedAt?.toString(),
                            deviceId = product.deviceId,
                            configValue = product.configValue
                        )
                        operations.add(BulkOperation(
                            typ = "insert",
                            arkusz = sheetName,
                            serialNumber = product.serialNumber!!,
                            dane = insertData
                        ))
                    }
                } catch (e: Exception) {
                    println("[UPLOAD] ERROR preparing ${product.serialNumber}: ${e.message}")
                    AppLogger.logError("Prepare upload ${product.serialNumber}", e)
                    errorCount++
                }
            }
            
            // Send bulk request
            if (operations.isNotEmpty()) {
                println("[UPLOAD] Sending bulk request with ${operations.size} operations")
                val response = apiService.bulkUpload(operations)
                
                val ok = (response.success == true) || (response.status == "SUKCES")
                if (ok) {
                    uploadedCount = operations.size
                    resultMessage = "✓ Upload SUCCESS: $insertCount new, $updateCount updated"
                    println("[UPLOAD] BULK SUCCESS: ${response.message}")
                } else {
                    resultMessage = "✗ Upload FAILED: ${response.message}"
                    println("[UPLOAD] BULK FAILED: ${response.message}")
                    errorCount = operations.size
                }
            } else {
                resultMessage = "No products to upload"
                println("[UPLOAD] No operations to upload")
            }
            
            // Upload device movements (history steps)
            try {
                val allMovements = deviceMovementRepository.getAllMovements().first()
                val movementsToUpload = allMovements.filter { movement ->
                    (!onlyRecent || movement.timestamp >= threshold)
                }
                
                println("[UPLOAD] Found ${movementsToUpload.size} device movements to upload")
                
                var movementsUploaded = 0
                var movementsError = 0
                
                for (movement in movementsToUpload) {
                    try {
                        val product = productRepository.getProductById(movement.productId).first()
                        val serialNumber = product?.serialNumber
                        
                        if (serialNumber.isNullOrBlank()) {
                            println("[UPLOAD] Skipping movement for product ${movement.productId} - no SN")
                            continue
                        }
                        
                        // Send add_step request
                        val stepData = mapOf(
                            "action" to "add_step",
                            "sn" to serialNumber,
                            "krok" to movement.action,
                            "data" to movement.timestamp.toString()
                        )
                        
                        println("[UPLOAD] Uploading movement: ${serialNumber} - ${movement.action}")
                        val response = apiService.addStep(stepData)
                        
                        if (response.success == true || response.status == "SUKCES") {
                            movementsUploaded++
                            println("[UPLOAD] Movement uploaded: ${serialNumber}")
                        } else {
                            movementsError++
                            println("[UPLOAD] Movement failed: ${serialNumber} - ${response.message}")
                        }
                        
                    } catch (e: Exception) {
                        println("[UPLOAD] ERROR uploading movement ${movement.id}: ${e.message}")
                        AppLogger.logError("Upload movement ${movement.id}", e)
                        movementsError++
                    }
                }
                
                if (movementsUploaded > 0 || movementsError > 0) {
                    resultMessage += "\nHistory: $movementsUploaded uploaded, $movementsError errors"
                    println("[UPLOAD] History complete: $movementsUploaded uploaded, $movementsError errors")
                }
                
            } catch (e: Exception) {
                resultMessage += "\nHistory upload error: ${e.message}"
                println("[UPLOAD] History upload error: ${e.message}")
                AppLogger.logError("Upload device movements", e)
            }
            
            AppLogger.logAction("Google Sheets Upload", "Complete: $insertCount inserts, $updateCount updates")
            println("[UPLOAD] Complete: $insertCount inserts, $updateCount updates, $uploadedCount total")
            
        } catch (e: Exception) {
            resultMessage = "✗ Upload ERROR: ${e.message}"
            AppLogger.logError("Google Sheets uploadChanges", e)
            errorCount++
        }
        
        UploadResult(
            totalUploaded = uploadedCount,
            insertCount = insertCount,
            updateCount = updateCount,
            errorCount = errorCount,
            message = resultMessage
        )
    }

    // Usuwa z opisu wstępne etykiety typu "Firma: ... | Status: ..." aby Komentarz był czysty
    private fun cleanComment(description: String?): String? {
        if (description.isNullOrBlank()) return null
        val cleaned = description
            .replace(Regex("(?i)firma:\\s*[^|]*\\|?\\s*"), "")
            .replace(Regex("(?i)status:\\s*[^|]*\\|?\\s*"), "")
            .replace(Regex("(?i)lokalizacja:\\s*[^|]*\\|?\\s*"), "")
            .trim().trim('|', ' ')
        return cleaned.ifBlank { null }
    }
    
    /**
     * Map Android package status to Google Sheets format
     * Android: ISSUED, RETURNED, PREPARATION, READY, WAREHOUSE
     * Google Sheets: Wydano, Zwrócono, Przygotowanie, Do wysyłki, Magazyn
     */
    private fun mapPackageStatusToSheetStatus(androidStatus: String?): String {
        // Domyślnie (brak paczki / null / puste) traktuj jako Magazyn
        val normalized = androidStatus?.uppercase()?.trim()
        return when (normalized) {
            null, "", "WAREHOUSE" -> "Magazyn"
            "ISSUED" -> "Wydano"
            "RETURNED" -> "Zwrócono"
            "PREPARATION" -> "Przygotowanie"
            "READY" -> "Do wysyłki"
            else -> "Magazyn" // fallback na Magazyn dla nieznanych
        }
    }
    
    /**
     * Determine target Google Sheet based on product name and category
     * - TC58E scanners → "Skanery"
     * - TC27 scanners → "Skanery tc27"
     * - Printers → "Drukarki"
     * - Printer docking stations → "Stacje do drukarek"
     * - Scanner docking stations → "Stacje Dokujące"
     */
    private fun determineTargetSheet(product: ProductEntity): String {
        val productName = product.name.lowercase()
        val categoryId = product.categoryId
        
        return when (categoryId) {
            1L -> { // Scanner
                if (productName.contains("tc27", ignoreCase = true)) {
                    "Skanery tc27"
                } else {
                    "Skanery" // TC58E and other scanners
                }
            }
            2L -> "Drukarki" // Printer
            3L -> "Stacje Dokujące" // Scanner Docking Station
            4L -> "Stacje do drukarek" // Printer Docking Station
            else -> "Skanery" // Default fallback
        }
    }
    
    /**
     * Check if product was downloaded from Google Sheets (vs created locally)
     * Heuristic: If product was created more than 1 hour before last update,
     * it's likely from sync. If created == updated (within 1 min), it's new.
     */
    private fun wasDownloadedFromSheets(product: ProductEntity): Boolean {
        val timeDiff = product.updatedAt - product.createdAt
        // If created and updated are very close (< 1 minute), it's newly created
        if (timeDiff < 60_000L) {
            return false // Newly created in app
        }
        // If there's significant time difference, likely from sync
        return timeDiff > 3600_000L // More than 1 hour difference
    }
    
    /**
     * Map Google Sheets status to PackageEntity status
     * Google Sheets: "Wydano", "Magazyn", "Zwrócono", etc.
     * PackageEntity: "Issued", "Returned", "Preparation", "Ready", "Warehouse"
     */
    private fun mapStatusToPackageStatus(sheetStatus: String?): String {
        return when (sheetStatus?.trim()?.lowercase()) {
            "wydano" -> "ISSUED"          // Issued
            "zwrócono" -> "RETURNED"      // Returned
            "przygotowanie" -> "PREPARATION"  // Preparation
            "do wysyłki" -> "READY"       // Ready to ship
            "magazyn" -> "WAREHOUSE"      // Warehouse
            else -> "PREPARATION"         // Default
        }
    }
    
    /**
     * Validate serial number - must look like a real SN, not summary data
     * Valid: S25013524202057, T58E-12345, etc.
     * Invalid: "Total 8", "Suma", numbers only, too short
     */
    private fun isValidSerialNumber(sn: String): Boolean {
        val trimmed = sn.trim()
        
        // Reject common summary patterns
        if (trimmed.matches(Regex("(?i)^(total|suma|razem|podsumowanie).*"))) {
            return false
        }
        
        // Reject pure numbers that look like counts (1-3 digits)
        if (trimmed.matches(Regex("^\\d{1,3}$"))) {
            return false
        }
        
        // Must be at least 5 characters for a valid SN
        if (trimmed.length < 5) {
            return false
        }
        
        // Must contain at least one letter or be longer than 8 chars (typical SN format)
        if (!trimmed.any { it.isLetter() } && trimmed.length < 8) {
            return false
        }
        
        return true
    }
    
    /**
     * Parse ISO 8601 date string to Unix timestamp (milliseconds)
     * Format: "2025-09-01T07:00:00.000Z"
     * Returns null if parsing fails or date is empty
     */
    private fun parseIsoDate(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null
        
        return try {
            // Simple parsing for ISO 8601 format YYYY-MM-DDTHH:mm:ss.sssZ
            val cleanDate = dateString.replace("Z", "").replace("T", " ").substring(0, 19)
            val parts = cleanDate.split(" ")
            val dateParts = parts[0].split("-")
            val timeParts = parts.getOrNull(1)?.split(":") ?: listOf("0", "0", "0")
            
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt()
            val day = dateParts[2].toInt()
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val second = timeParts[2].toInt()
            
            // Simple conversion (not timezone-aware, treats as UTC)
            java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(year, month - 1, day, hour, minute, second)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (e: Exception) {
            println("[SYNC] ERROR parsing date '$dateString': ${e.message}")
            null
        }
    }
    
    /**
     * Helper to get categoryId from category name
     * Uses reflection to access CategoryHelper or hardcoded mapping
     */
    private fun getCategoryIdByName(categoryName: String): Long? {
        return when (categoryName) {
            "Scanner" -> 1L
            "Printer" -> 2L
            "Scanner Docking Station" -> 3L
            "Printer Docking Station" -> 4L
            "Other" -> 5L
            else -> null
        }
    }
    
    /**
     * Get sheet name for a given category ID
     */
    private fun getSheetNameForCategory(categoryId: Long?): String? {
        val categoryName = when (categoryId) {
            1L -> "Scanner"
            2L -> "Printer"
            3L -> "Scanner Docking Station"
            4L -> "Printer Docking Station"
            5L -> "Other"
            else -> return null
        }
        return CATEGORY_TO_SHEET_MAP[categoryName]
    }
}
