package com.example.inventoryapp.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductTemplateRepository
import com.example.inventoryapp.data.repository.ImportBackupRepository
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.data.repository.UploadResult
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.data.local.entities.PackageProductCrossRef
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.BoxProductCrossRef
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.data.local.entities.DeviceMovementEntity
import com.example.inventoryapp.data.local.entities.ImportBackupEntity
import com.example.inventoryapp.data.local.entity.ImportPreview
import com.example.inventoryapp.data.local.entity.ImportPreviewFilter
import com.example.inventoryapp.utils.AppLogger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

import com.example.inventoryapp.data.local.entities.InventoryCountSessionEntity
import com.example.inventoryapp.data.local.entities.InventoryCountItemEntity

/**
 * Enhanced export data structure with relationships
 */
data class ExportData(
    val version: Int = 4, // Incremented version for full backup (added inventory count)
    val exportedAt: Long = System.currentTimeMillis(),
    val products: List<ProductEntity>,
    val packages: List<PackageEntity>,
    val templates: List<ProductTemplateEntity>,
    val boxes: List<BoxEntity>,
    val contractors: List<ContractorEntity>,
    val packageProductRelations: List<PackageProductCrossRef> = emptyList(), // Product-Package relations
    val boxProductRelations: List<BoxProductCrossRef> = emptyList(), // Product-Box relations
    val inventoryCountSessions: List<InventoryCountSessionEntity> = emptyList(),
    val inventoryCountItems: List<InventoryCountItemEntity> = emptyList()
    ,
    val deviceMovements: List<DeviceMovementEntity> = emptyList()
)

class ExportImportViewModel(
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository,
    private val templateRepository: ProductTemplateRepository,
    private val backupRepository: ImportBackupRepository,
    private val boxRepository: BoxRepository,
    private val contractorRepository: ContractorRepository,
    private val deviceMovementRepository: com.example.inventoryapp.data.repository.DeviceMovementRepository,
    private val inventoryCountRepository: com.example.inventoryapp.data.repository.InventoryCountRepository
) : ViewModel() {

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status
    
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val _hasRecentBackup = MutableStateFlow(false)
    val hasRecentBackup: StateFlow<Boolean> = _hasRecentBackup

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    init {
        checkForRecentBackup()
    }

    private fun checkForRecentBackup() {
        viewModelScope.launch {
            val backup = backupRepository.getLatestBackup()
            _hasRecentBackup.value = backup != null
        }
    }

    suspend fun exportToJson(outputFile: File): Boolean {
        return try {
            AppLogger.logAction("Export Started", "File: ${outputFile.absolutePath}")
            _status.value = "Exporting data..."
            
            val products = productRepository.getAllProducts().first()
            val packages = packageRepository.getAllPackages().first()
            val templates = templateRepository.getAllTemplates().first()
            val boxes = boxRepository.getAllBoxes().first()
            val contractors = contractorRepository.getAllContractors().first()

            // Collect all package-product relationships
            val packageProductRelations = mutableListOf<PackageProductCrossRef>()
            packages.forEach { pkg ->
                val productsInPackage = packageRepository.getProductsInPackage(pkg.id).first()
                productsInPackage.forEach { product ->
                    packageProductRelations.add(
                        PackageProductCrossRef(
                            packageId = pkg.id,
                            productId = product.id
                        )
                    )
                }
            }

            // Collect all box-product relationships
            val boxProductRelations = mutableListOf<BoxProductCrossRef>()
            boxes.forEach { box ->
                val productsInBox = boxRepository.getProductsInBox(box.id).first()
                productsInBox.forEach { product ->
                    boxProductRelations.add(
                        BoxProductCrossRef(
                            boxId = box.id,
                            productId = product.id
                        )
                    )
                }
            }

            // Device movements will be collected inline for the backup

            // Inventory Count Sessions & Items
            val inventoryCountSessions = inventoryCountRepository.getAllSessions().first()
            val inventoryCountItems = mutableListOf<InventoryCountItemEntity>()
            for (session in inventoryCountSessions) {
                val items = inventoryCountRepository.getItemsForSession(session.id)
                inventoryCountItems.addAll(items)
            }

            val exportData = ExportData(
                products = products,
                packages = packages,
                templates = templates,
                boxes = boxes,
                contractors = contractors,
                packageProductRelations = packageProductRelations,
                boxProductRelations = boxProductRelations,
                inventoryCountSessions = inventoryCountSessions,
                inventoryCountItems = inventoryCountItems,
                deviceMovements = deviceMovementRepository.getAllMovements().first()
            )

            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()

            OutputStreamWriter(FileOutputStream(outputFile), Charsets.UTF_8).use { writer ->
                gson.toJson(exportData, writer)
            }

            val message = "Export successful: ${products.size} products, ${packages.size} packages, ${boxes.size} boxes, ${contractors.size} contractors, ${templates.size} templates, ${packageProductRelations.size} pkg-relations, ${boxProductRelations.size} box-relations"
            _status.value = message
            AppLogger.logAction("Export Completed", message)
            true
        } catch (e: Exception) {
            val errorMsg = "Export failed: ${e.message}"
            _status.value = errorMsg
            AppLogger.logError("Export", e)
            false
        }
    }

    /**
     * Generate import preview showing what will be added/updated
     */
    suspend fun generateImportPreview(inputFile: File): ImportPreview? {
        return try {
            AppLogger.logAction("Generate Import Preview", "File: ${inputFile.absolutePath}")
            _status.value = "Analyzing import file..."
            
            val exportData = InputStreamReader(inputFile.inputStream(), Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, ExportData::class.java)
            }

            // Get existing data
            val existingProducts = productRepository.getAllProducts().first()
            val existingPackages = packageRepository.getAllPackages().first()
            val existingContractors = contractorRepository.getAllContractors().first()
            val existingBoxes = boxRepository.getAllBoxes().first()
            
            // Map existing items by their unique identifiers
            val existingProductSNs = existingProducts.map { it.serialNumber }.toSet()
            val existingPackageIds = existingPackages.map { it.id }.toSet()
            val existingContractorIds = existingContractors.map { it.id }.toSet()
            val existingBoxIds = existingBoxes.map { it.id }.toSet()
            
            // Categorize products: new vs update
            val newProducts = mutableListOf<ProductEntity>()
            val updateProducts = mutableListOf<ProductEntity>()
            
            exportData.products.forEach { product ->
                if (existingProductSNs.contains(product.serialNumber)) {
                    updateProducts.add(product)
                } else {
                    newProducts.add(product)
                }
            }
            
            // Categorize packages: new vs update
            val newPackages = mutableListOf<PackageEntity>()
            val updatePackages = mutableListOf<PackageEntity>()
            
            exportData.packages.forEach { pkg ->
                if (existingPackageIds.contains(pkg.id)) {
                    updatePackages.add(pkg)
                } else {
                    newPackages.add(pkg)
                }
            }
            
            // Categorize contractors: new vs update
            val newContractors = mutableListOf<ContractorEntity>()
            val updateContractors = mutableListOf<ContractorEntity>()
            
            exportData.contractors.forEach { contractor ->
                if (existingContractorIds.contains(contractor.id)) {
                    updateContractors.add(contractor)
                } else {
                    newContractors.add(contractor)
                }
            }
            
            // Categorize boxes: new vs update
            val newBoxes = mutableListOf<BoxEntity>()
            val updateBoxes = mutableListOf<BoxEntity>()
            
            exportData.boxes.forEach { box ->
                if (existingBoxIds.contains(box.id)) {
                    updateBoxes.add(box)
                } else {
                    newBoxes.add(box)
                }
            }
            
            // Templates are always new (they use auto-increment IDs)
            val newTemplates = exportData.templates
            
            val preview = ImportPreview(
                newProducts = newProducts,
                updateProducts = updateProducts,
                newPackages = newPackages,
                updatePackages = updatePackages,
                newTemplates = newTemplates,
                newContractors = newContractors,
                updateContractors = updateContractors,
                newBoxes = newBoxes,
                updateBoxes = updateBoxes
            )
            
            _status.value = "Preview ready: ${preview.totalNewItems} new, ${preview.totalUpdateItems} to update"
            AppLogger.logAction("Import Preview Generated", _status.value)
            
            preview
        } catch (e: Exception) {
            val errorMsg = "Failed to generate preview: ${e.message}"
            _status.value = errorMsg
            AppLogger.logError("Import Preview", e)
            null
        }
    }

    suspend fun importFromJson(inputFile: File): Boolean {
        return try {
            AppLogger.logAction("Import Started", "File: ${inputFile.absolutePath}")
            _status.value = "Creating backup..."
            
            // STEP 0: Create backup before import
            val backupCreated = createBackupBeforeImport("Import from ${inputFile.name}")
            if (!backupCreated) {
                _status.value = "Warning: Could not create backup, import aborted for safety"
                return false
            }
            
            _status.value = "Importing data..."
            
            val exportData = InputStreamReader(inputFile.inputStream(), Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, ExportData::class.java)
            }

            var importedProducts = 0
            var importedPackages = 0
            var importedTemplates = 0
            var importedBoxes = 0
            var importedContractors = 0
            var importedPackageRelations = 0
            var importedBoxRelations = 0
            var importedInventorySessions = 0
            var importedInventoryItems = 0
            val productIdMap = mutableMapOf<Long, Long>()
            // Step 8: Import Inventory Count Sessions (UPSERT by name)
            val sessionIdMap = mutableMapOf<Long, Long>()
            exportData.inventoryCountSessions.forEach { session ->
                try {
                    val oldId = session.id
                    val existingSessions = inventoryCountRepository.getAllSessions().first()
                    val existing = existingSessions.find { it.name.equals(session.name, ignoreCase = true) }
                    val newId = if (existing != null) {
                        inventoryCountRepository.updateSession(session.copy(id = existing.id))
                        existing.id
                    } else {
                        inventoryCountRepository.createSession(session.name, session.notes)
                    }
                    sessionIdMap[oldId] = newId
                    importedInventorySessions++
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped inventory session: ${session.name}", e)
                }
            }

            // Step 9: Import Inventory Count Items (map sessionId and productId)
            exportData.inventoryCountItems.forEach { item ->
                try {
                    val newSessionId = sessionIdMap[item.sessionId]
                    val newProductId = productIdMap[item.productId]
                    if (newSessionId != null && newProductId != null) {
                        val newItem = item.copy(id = 0, sessionId = newSessionId, productId = newProductId)
                        inventoryCountRepository.inventoryCountDao.insertItem(newItem)
                        importedInventoryItems++
                    }
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped inventory item: session ${item.sessionId} product ${item.productId}", e)
                }
            }

            // Step 1: Import contractors first (no dependencies) - UPSERT by name
            val contractorIdMap = mutableMapOf<Long, Long>()
            exportData.contractors.forEach { contractor ->
                try {
                    val oldId = contractor.id
                    // Check if contractor with same name exists
                    val existingContractors = contractorRepository.getAllContractors().first()
                    val existing = existingContractors.find { it.name.equals(contractor.name, ignoreCase = true) }
                    
                    val newId = if (existing != null) {
                        // Update existing contractor
                        contractorRepository.updateContractor(contractor.copy(id = existing.id))
                        existing.id
                    } else {
                        // Insert new contractor
                        contractorRepository.insertContractor(contractor.copy(id = 0))
                    }
                    contractorIdMap[oldId] = newId
                    importedContractors++
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped contractor: ${contractor.name}", e)
                }
            }

            // Step 2: Import templates (no dependencies) - UPSERT by name
            exportData.templates.forEach { template ->
                try {
                    // Check if template with same name exists
                    val existingTemplates = templateRepository.getAllTemplates().first()
                    val existing = existingTemplates.find { it.name.equals(template.name, ignoreCase = true) }
                    
                    if (existing != null) {
                        // Update existing template
                        templateRepository.updateTemplate(template.copy(id = existing.id))
                    } else {
                        // Insert new template
                        templateRepository.insertTemplate(template.copy(id = 0))
                    }
                    importedTemplates++
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped template: ${template.name}", e)
                }
            }

            // productIdMap already declared above
            // Step 3: Import products and track ID mapping (old ID -> new ID) - UPSERT by serialNumber
            exportData.products.forEach { product ->
                try {
                    val oldId = product.id
                    // Check if product with same serial number exists
                    val existingProducts = productRepository.getAllProducts().first()
                    val existing = existingProducts.find { 
                        it.serialNumber != null && 
                        it.serialNumber.equals(product.serialNumber, ignoreCase = true) 
                    }
                    
                    val newId = if (existing != null) {
                        // Update existing product
                        productRepository.updateProduct(product.copy(id = existing.id))
                        existing.id
                    } else {
                        // Insert new product
                        productRepository.insertProduct(product.copy(id = 0))
                    }
                    productIdMap[oldId] = newId
                    importedProducts++
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped product: ${product.name}", e)
                }
            }

            // Step 4: Import packages and track ID mapping (old ID -> new ID) - UPSERT by name
            val packageIdMap = mutableMapOf<Long, Long>()
            exportData.packages.forEach { pkg ->
                try {
                    val oldId = pkg.id
                    // Check if package with same name exists
                    val existingPackages = packageRepository.getAllPackages().first()
                    val existing = existingPackages.find { it.name.equals(pkg.name, ignoreCase = true) }
                    
                    // Remap contractor ID if exists
                    val newContractorId = pkg.contractorId?.let { contractorIdMap[it] }
                    
                    val newId = if (existing != null) {
                        // Update existing package
                        packageRepository.updatePackage(pkg.copy(id = existing.id, contractorId = newContractorId ?: existing.contractorId))
                        existing.id
                    } else {
                        // Insert new package
                        packageRepository.insertPackage(pkg.copy(id = 0, contractorId = newContractorId))
                    }
                    packageIdMap[oldId] = newId
                    importedPackages++
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped package: ${pkg.name}", e)
                }
            }

            // Step 5: Import boxes and track ID mapping - UPSERT by name
            val boxIdMap = mutableMapOf<Long, Long>()
            exportData.boxes.forEach { box ->
                try {
                    val oldId = box.id
                    // Check if box with same name exists
                    val existingBoxes = boxRepository.getAllBoxes().first()
                    val existing = existingBoxes.find { it.name.equals(box.name, ignoreCase = true) }
                    
                    val newId = if (existing != null) {
                        // Update existing box
                        boxRepository.updateBox(box.copy(id = existing.id))
                        existing.id
                    } else {
                        // Insert new box
                        boxRepository.insertBox(box.copy(id = 0))
                    }
                    boxIdMap[oldId] = newId
                    importedBoxes++
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped box: ${box.name}", e)
                }
            }

            // Step 6: Import package-product relationships using mapped IDs
            exportData.packageProductRelations.forEach { relation ->
                try {
                    val newPackageId = packageIdMap[relation.packageId]
                    val newProductId = productIdMap[relation.productId]
                    
                    if (newPackageId != null && newProductId != null) {
                        packageRepository.addProductToPackage(newPackageId, newProductId)
                        importedPackageRelations++
                    } else {
                        AppLogger.w("Import", "Skipped pkg relation - package ${relation.packageId} or product ${relation.productId} not found")
                    }
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped pkg relation: package ${relation.packageId} -> product ${relation.productId}", e)
                }
            }

            // Step 7: Import box-product relationships using mapped IDs
            exportData.boxProductRelations.forEach { relation ->
                try {
                    val newBoxId = boxIdMap[relation.boxId]
                    val newProductId = productIdMap[relation.productId]
                    
                    if (newBoxId != null && newProductId != null) {
                        boxRepository.addProductToBox(newBoxId, newProductId)
                        importedBoxRelations++
                    } else {
                        AppLogger.w("Import", "Skipped box relation - box ${relation.boxId} or product ${relation.productId} not found")
                    }
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped box relation: box ${relation.boxId} -> product ${relation.productId}", e)
                }
            }

            // Step 10: Import device movements (map old IDs to new IDs where possible)
            var importedMovements = 0
            exportData.deviceMovements.forEach { mov ->
                try {
                    val newProductId = productIdMap[mov.productId]
                    if (newProductId == null) {
                        AppLogger.w("Import", "Skipped movement - product ${mov.productId} not found in mapping")
                        return@forEach
                    }

                    val newFromId = when (mov.fromContainerType) {
                        "PACKAGE" -> mov.fromContainerId?.let { packageIdMap[it] }
                        "BOX" -> mov.fromContainerId?.let { boxIdMap[it] }
                        else -> mov.fromContainerId
                    }

                    val newToId = when (mov.toContainerType) {
                        "PACKAGE" -> mov.toContainerId?.let { packageIdMap[it] }
                        "BOX" -> mov.toContainerId?.let { boxIdMap[it] }
                        else -> mov.toContainerId
                    }

                    val newMovement = DeviceMovementEntity(
                        id = 0,
                        productId = newProductId,
                        action = mov.action,
                        fromContainerType = mov.fromContainerType,
                        fromContainerId = newFromId,
                        toContainerType = mov.toContainerType,
                        toContainerId = newToId,
                        timestamp = mov.timestamp,
                        packageStatus = mov.packageStatus,
                        note = mov.note
                    )

                    deviceMovementRepository.insertMovement(newMovement)
                    importedMovements++
                } catch (e: Exception) {
                    AppLogger.w("Import", "Skipped device movement import", e)
                }
            }

            val message = "Import successful: $importedProducts products, $importedPackages packages, $importedBoxes boxes, $importedContractors contractors, $importedTemplates templates, $importedPackageRelations pkg-relations, $importedBoxRelations box-relations"
            val messageFull = "Import successful: $importedProducts products, $importedPackages packages, $importedBoxes boxes, $importedContractors contractors, $importedTemplates templates, $importedPackageRelations pkg-relations, $importedBoxRelations box-relations, $importedInventorySessions inventory sessions, $importedInventoryItems inventory items, $importedMovements device movements"
            _status.value = messageFull
            AppLogger.logAction("Import Completed", messageFull)
            checkForRecentBackup() // Update backup status
            true
        } catch (e: Exception) {
            val errorMsg = "Import failed: ${e.message}"
            _status.value = errorMsg
            AppLogger.logError("Import", e)
            false
        }
    }

    /**
     * Create backup of current database state before import
     */
    private suspend fun createBackupBeforeImport(description: String): Boolean {
        return try {
            AppLogger.logAction("Backup", "Creating backup before import")
            
            // Collect current state
            val products = productRepository.getAllProducts().first()
            val packages = packageRepository.getAllPackages().first()
            val templates = templateRepository.getAllTemplates().first()
            val boxes = boxRepository.getAllBoxes().first()
            val contractors = contractorRepository.getAllContractors().first()
            
            val packageProductRelations = mutableListOf<PackageProductCrossRef>()
            packages.forEach { pkg ->
                val productsInPackage = packageRepository.getProductsInPackage(pkg.id).first()
                productsInPackage.forEach { product ->
                    packageProductRelations.add(
                        PackageProductCrossRef(
                            packageId = pkg.id,
                            productId = product.id
                        )
                    )
                }
            }
            
            val boxProductRelations = mutableListOf<BoxProductCrossRef>()
            boxes.forEach { box ->
                val productsInBox = boxRepository.getProductsInBox(box.id).first()
                productsInBox.forEach { product ->
                    boxProductRelations.add(
                        BoxProductCrossRef(
                            boxId = box.id,
                            productId = product.id
                        )
                    )
                }
            }

            val backupData = ExportData(
                products = products,
                packages = packages,
                templates = templates,
                boxes = boxes,
                contractors = contractors,
                packageProductRelations = packageProductRelations,
                boxProductRelations = boxProductRelations,
                deviceMovements = deviceMovementRepository.getAllMovements().first()
            )

            val backupJson = gson.toJson(backupData)
            
            val backup = ImportBackupEntity(
                backupJson = backupJson,
                importDescription = description,
                productsCount = products.size,
                packagesCount = packages.size,
                templatesCount = templates.size
            )
            
            backupRepository.insertBackup(backup)
            
            // Keep only last 5 backups to save space
            backupRepository.pruneOldBackups(5)
            
            AppLogger.logAction("Backup Created", "Products: ${products.size}, Packages: ${packages.size}, Templates: ${templates.size}")
            true
        } catch (e: Exception) {
            AppLogger.logError("Backup Creation Failed", e)
            false
        }
    }

    /**
     * Undo last import by restoring from backup
     */
    suspend fun undoLastImport(): Boolean {
        return try {
            AppLogger.logAction("Undo Import", "Starting...")
            _status.value = "Restoring from backup..."
            
            val backup = backupRepository.getLatestBackup()
            if (backup == null) {
                _status.value = "No backup found to restore"
                return false
            }
            
            val backupData = gson.fromJson(backup.backupJson, ExportData::class.java)
            
            // Clear current data
            _status.value = "Clearing current data..."
            
            // Delete all current entries
            productRepository.getAllProducts().first().forEach { product ->
                productRepository.deleteProduct(product)
            }
            packageRepository.getAllPackages().first().forEach { pkg ->
                packageRepository.deletePackage(pkg)
            }
            templateRepository.getAllTemplates().first().forEach { template ->
                templateRepository.deleteTemplate(template)
            }
            boxRepository.getAllBoxes().first().forEach { box ->
                boxRepository.deleteBox(box)
            }
            contractorRepository.getAllContractors().first().forEach { contractor ->
                contractorRepository.deleteContractor(contractor)
            }
            
            // Restore from backup
            _status.value = "Restoring data from backup..."
            
            var restoredProducts = 0
            var restoredPackages = 0
            var restoredTemplates = 0
            var restoredBoxes = 0
            var restoredContractors = 0
            var restoredPackageRelations = 0
            var restoredBoxRelations = 0
            
            // Restore contractors first (no dependencies)
            val contractorIdMap = mutableMapOf<Long, Long>()
            backupData.contractors.forEach { contractor ->
                try {
                    val oldId = contractor.id
                    val newId = contractorRepository.insertContractor(contractor.copy(id = 0))
                    contractorIdMap[oldId] = newId
                    restoredContractors++
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Failed to restore contractor: ${contractor.name}", e)
                }
            }
            
            // Restore templates
            backupData.templates.forEach { template ->
                try {
                    templateRepository.insertTemplate(template.copy(id = 0))
                    restoredTemplates++
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Failed to restore template: ${template.name}", e)
                }
            }
            
            // Restore products with ID mapping
            val productIdMap = mutableMapOf<Long, Long>()
            backupData.products.forEach { product ->
                try {
                    val oldId = product.id
                    val newId = productRepository.insertProduct(product.copy(id = 0))
                    productIdMap[oldId] = newId
                    restoredProducts++
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Failed to restore product: ${product.name}", e)
                }
            }
            
            // Restore packages with ID mapping and contractor mapping
            val packageIdMap = mutableMapOf<Long, Long>()
            backupData.packages.forEach { pkg ->
                try {
                    val oldId = pkg.id
                    val newContractorId = pkg.contractorId?.let { contractorIdMap[it] }
                    val newId = packageRepository.insertPackage(
                        pkg.copy(id = 0, contractorId = newContractorId)
                    )
                    packageIdMap[oldId] = newId
                    restoredPackages++
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Failed to restore package: ${pkg.name}", e)
                }
            }
            
            // Restore boxes with ID mapping
            val boxIdMap = mutableMapOf<Long, Long>()
            backupData.boxes.forEach { box ->
                try {
                    val oldId = box.id
                    val newId = boxRepository.insertBox(box.copy(id = 0))
                    boxIdMap[oldId] = newId
                    restoredBoxes++
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Failed to restore box: ${box.name}", e)
                }
            }
            
            // Restore package-product relations
            backupData.packageProductRelations.forEach { relation ->
                try {
                    val newPackageId = packageIdMap[relation.packageId]
                    val newProductId = productIdMap[relation.productId]
                    
                    if (newPackageId != null && newProductId != null) {
                        packageRepository.addProductToPackage(newPackageId, newProductId)
                        restoredPackageRelations++
                    }
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Failed to restore package relation", e)
                }
            }
            
            // Restore box-product relations
            backupData.boxProductRelations.forEach { relation ->
                try {
                    val newBoxId = boxIdMap[relation.boxId]
                    val newProductId = productIdMap[relation.productId]
                    
                    if (newBoxId != null && newProductId != null) {
                        boxRepository.addProductToBox(newBoxId, newProductId)
                        restoredBoxRelations++
                    }
                } catch (e: Exception) {
                    AppLogger.w("Restore", "Failed to restore box relation", e)
                }
            }
            
            // Delete the used backup
            backupRepository.deleteBackup(backup)
            
            val message = "Undo successful: Restored $restoredProducts products, $restoredPackages packages, $restoredBoxes boxes, $restoredContractors contractors, $restoredTemplates templates, $restoredPackageRelations pkg-relations, $restoredBoxRelations box-relations"
            _status.value = message
            AppLogger.logAction("Undo Completed", message)
            checkForRecentBackup() // Update backup status
            true
        } catch (e: Exception) {
            val errorMsg = "Undo failed: ${e.message}"
            _status.value = errorMsg
            AppLogger.logError("Undo", e)
            false
        }
    }

    fun clearStatus() {
        _status.value = ""
    }

    // ============================================================================
    // Google Sheets API Integration
    // ============================================================================
    
    /**
     * Sealed class representing Google Sheets sync state
     */
    sealed class GoogleSheetsSyncState {
        object Idle : GoogleSheetsSyncState()
        object Loading : GoogleSheetsSyncState()
        data class Success(val message: String) : GoogleSheetsSyncState()
        data class Error(val message: String) : GoogleSheetsSyncState()
    }
    
    private val _googleSheetsSyncState = MutableStateFlow<GoogleSheetsSyncState>(GoogleSheetsSyncState.Idle)
    val googleSheetsSyncState: StateFlow<GoogleSheetsSyncState> = _googleSheetsSyncState
    
    /**
     * Sync (download) data from Google Sheets API to local database
     * Fetches all sheets and merges products into Room database
     */
    fun syncFromGoogleSheets() {
        viewModelScope.launch {
            if (!com.example.inventoryapp.data.remote.GoogleSheetsApiService.ENABLED) {
                _status.value = "Google Sheets disabled"
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Error("Google Sheets disabled")
                return@launch
            }
            try {
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Loading
                _status.value = "Downloading from Google Sheets..."
                
                AppLogger.logAction("Google Sheets Sync", "Starting download")
                
                // Create repository instances
                val apiService = com.example.inventoryapp.data.remote.GoogleSheetsApiService()
                val googleSheetsRepo = com.example.inventoryapp.data.repository.GoogleSheetsRepository(
                    apiService,
                    productRepository,
                    packageRepository,
                    contractorRepository,
                    deviceMovementRepository
                )
                
                // Execute sync
                val (packagesProcessed, productsProcessed) = googleSheetsRepo.downloadAndSync()
                
                val message = "Sync complete: $packagesProcessed packages, $productsProcessed products processed"
                _status.value = message
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Success(message)
                
                AppLogger.logAction("Google Sheets Sync", message)
                
            } catch (e: Exception) {
                val errorMessage = "Sync failed: ${e.message}"
                _status.value = errorMessage
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Error(errorMessage)
                AppLogger.logError("Google Sheets Sync", e)
            }
        }
    }
    
    /**
     * Upload local changes to Google Sheets API
     * Uploads products that have been modified recently
     */
    fun uploadToGoogleSheets() {
        viewModelScope.launch {
            if (!com.example.inventoryapp.data.remote.GoogleSheetsApiService.ENABLED) {
                _status.value = "Google Sheets disabled"
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Error("Google Sheets disabled")
                return@launch
            }
            try {
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Loading
                _status.value = "Uploading to Google Sheets..."
                
                AppLogger.logAction("Google Sheets Upload", "Starting upload")
                
                // Create repository instances
                val apiService = com.example.inventoryapp.data.remote.GoogleSheetsApiService()
                val googleSheetsRepo = com.example.inventoryapp.data.repository.GoogleSheetsRepository(
                    apiService,
                    productRepository,
                    packageRepository,
                    contractorRepository,
                    deviceMovementRepository
                )
                
                // Execute upload (only recent changes from last 7 days)
                val result = googleSheetsRepo.uploadChanges(onlyRecent = true)
                
                val message = "${result.message}\nTotal: ${result.totalUploaded} products"
                _status.value = message
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Success(message)
                
                AppLogger.logAction("Google Sheets Upload", message)
                _toastMessage.value = result.message  // Show toast with result
                
            } catch (e: Exception) {
                val errorMessage = "✗ Upload failed: ${e.message}"
                _status.value = errorMessage
                _googleSheetsSyncState.value = GoogleSheetsSyncState.Error(errorMessage)
                _toastMessage.value = errorMessage
                AppLogger.logError("Google Sheets Upload", e)
            }
        }
    }
    
    fun clearToastMessage() {
        _toastMessage.value = null
    }
}
