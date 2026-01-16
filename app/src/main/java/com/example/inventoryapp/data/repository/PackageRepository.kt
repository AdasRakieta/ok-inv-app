package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.PackageDao
import com.example.inventoryapp.data.local.dao.BoxDao
import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.dao.PackageWithCount
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.repository.DeviceMovementRepository
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.PackageProductCrossRef
import com.example.inventoryapp.data.local.entities.BoxProductCrossRef
import com.example.inventoryapp.data.models.AddProductResult
import com.example.inventoryapp.data.models.PackageExportData
import com.example.inventoryapp.data.models.PackageImportResult
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PackageRepository(
    private val packageDao: PackageDao,
    private val productDao: ProductDao,
    private val boxDao: BoxDao,
    private val deviceMovementRepository: DeviceMovementRepository? = null
) {
    
    fun getAllPackages(): Flow<List<PackageEntity>> = packageDao.getAllPackages()
    
    fun getAllPackagesWithCount(): Flow<List<PackageWithCount>> = packageDao.getAllPackagesWithCount()
    
    fun getArchivedPackages(): Flow<List<PackageEntity>> = packageDao.getArchivedPackages()
    
    fun getArchivedPackagesWithCount(): Flow<List<PackageWithCount>> = packageDao.getArchivedPackagesWithCount()
    
    fun getPackageById(packageId: Long): Flow<PackageEntity?> = 
        packageDao.getPackageById(packageId)
    
    suspend fun getPackageByName(name: String): PackageEntity? =
        packageDao.getPackageByName(name)
    
    suspend fun getPackageByCode(packageCode: String): PackageEntity? =
        packageDao.getPackageByCode(packageCode)
    
    suspend fun insertPackage(packageEntity: PackageEntity): Long {
        // Check if package with same name already exists
        val existing = packageDao.getPackageByName(packageEntity.name)
        if (existing != null) {
            throw IllegalArgumentException("Package with name '${packageEntity.name}' already exists")
        }
        return packageDao.insertPackage(packageEntity)
    }
    
    /**
     * Update package. If the package is currently archived, disallow edits (no-op)
     * unless caller is unarchiving (setting archived=false).
     */
    suspend fun updatePackage(packageEntity: PackageEntity) {
        val current = getPackageById(packageEntity.id).first() ?: return
        // If currently archived and still archived in the update, disallow edits
        if (current.archived && packageEntity.archived) {
            // No-op: archived packages are read-only. Caller should unarchive first.
            return
        }
        // Otherwise allow update (including unarchive)
        packageDao.updatePackage(packageEntity)
    }
    
    suspend fun deletePackage(packageEntity: PackageEntity) =
        packageDao.deletePackage(packageEntity)
    
    suspend fun deletePackageById(packageId: Long) =
        packageDao.deletePackageById(packageId)
    
    suspend fun addProductToPackage(packageId: Long, productId: Long): AddProductResult {
        return try {
            // If product is already assigned to a package, enforce rules
            val existingPackage = packageDao.getPackageForProduct(productId).first()
            if (existingPackage != null && existingPackage.id != packageId) {
                if (existingPackage.archived) {
                    // Existing package is archived — treat it as historical. Keep the archived relation intact
                    // and allow assigning the product to the new package (no unassign from archived package).
                } else {
                    // Existing package is active: do not allow reassignment — user must archive the previous package first
                    return AddProductResult.AlreadyInActivePackage(existingPackage.name, existingPackage.status)
                }
            }

            // Check if product is already in a box, remove it if so
            val existingBox = boxDao.getBoxForProduct(productId).first()
            if (existingBox != null) {
                // remove from box and record movement UNASSIGN -> ASSIGN
                boxDao.removeProductFromBoxById(existingBox.id, productId)
                // record unassign from box
                deviceMovementRepository?.recordUnassignFromBox(productId, existingBox.id)
                packageDao.addProductToPackage(PackageProductCrossRef(packageId, productId))
                // record assign to package
                deviceMovementRepository?.recordAssignToPackage(productId, packageId)
                return AddProductResult.TransferredFromBox(existingBox.name)
            }

            // Product not in any box or package (or previous was archived), just add it
            packageDao.addProductToPackage(PackageProductCrossRef(packageId, productId))
            deviceMovementRepository?.recordAssignToPackage(productId, packageId)
            AddProductResult.Success
        } catch (e: Exception) {
            AddProductResult.Error("Failed to add product to package: ${e.message}")
        }
    }
    
    suspend fun removeProductFromPackage(packageId: Long, productId: Long) {
        // Prevent removing products from an archived package — archived packages are read-only
        val pkg = getPackageById(packageId).first() ?: return
        if (pkg.archived) {
            // Don't remove relations from archived packages; keep historical membership
            return
        }
        packageDao.removeProductFromPackage(PackageProductCrossRef(packageId, productId))
        deviceMovementRepository?.recordUnassignFromPackage(productId, packageId)
    }
    
    fun getProductsInPackage(packageId: Long): Flow<List<ProductEntity>> =
        packageDao.getProductsInPackage(packageId)
    
    fun getPackageForProduct(productId: Long): Flow<PackageEntity?> =
        packageDao.getPackageForProduct(productId)
    
    fun getPackageByProductId(productId: Long): Flow<PackageEntity?> =
        getPackageForProduct(productId)
    
    suspend fun removeAllProductsFromPackage(packageId: Long) =
        packageDao.removeAllProductsFromPackage(packageId)
    
    /**
     * Archive a package (only RETURNED packages can be archived)
     */
    suspend fun archivePackage(packageId: Long) {
        val pkg = getPackageById(packageId).first() ?: return
        if (pkg.status == "RETURNED") {
            val archivedPackage = pkg.copy(archived = true)
            updatePackage(archivedPackage)
        }
    }
    
    /**
     * Unarchive a package (restore to active packages list)
     */
    suspend fun unarchivePackage(packageId: Long) {
        val pkg = getPackageById(packageId).first() ?: return
        val unarchivedPackage = pkg.copy(archived = false)
        updatePackage(unarchivedPackage)
    }
    
    /**
     * Archive multiple packages at once
     */
    suspend fun archivePackages(packageIds: List<Long>) {
        packageIds.forEach { packageId ->
            archivePackage(packageId)
        }
    }
    
    /**
     * Export package data for QR code sharing
     */
    suspend fun exportPackage(packageId: Long): PackageExportData? {
        val pkg = getPackageById(packageId).first() ?: return null
        val products = getProductsInPackage(packageId).first()
        return PackageExportData(
            packageInfo = pkg,
            products = products
        )
    }
    
    /**
     * Import package data from QR scan with merge logic:
     * - Keep existing products in database
     * - Add new products from import
     * - If serial numbers match, overwrite with imported data (update)
     */
    suspend fun importPackage(exportData: PackageExportData): PackageImportResult {
        val errors = mutableListOf<String>()
        var productsAdded = 0
        var productsUpdated = 0
        
        try {
            // Insert or update package
            val existingPkg = getPackageById(exportData.packageInfo.id).first()
            val packageId = if (existingPkg == null) {
                insertPackage(exportData.packageInfo)
            } else {
                updatePackage(exportData.packageInfo)
                exportData.packageInfo.id
            }
            
            // Process each product
            for (importedProduct in exportData.products) {
                try {
                    // Check if product with this serial number already exists
                    val existingProduct = if (importedProduct.serialNumber != null) {
                        productDao.getProductBySerialNumber(importedProduct.serialNumber)
                    } else {
                        null
                    }
                    
                    val productId = existingProduct?.let { product ->
                        // Product with matching SN exists - update it
                        val updatedProduct = importedProduct.copy(
                            id = product.id,
                            updatedAt = System.currentTimeMillis()
                        )
                        productDao.updateProduct(updatedProduct)
                        productsUpdated++
                        product.id
                    } ?: run {
                        // New product - insert it
                        val newProduct = importedProduct.copy(
                            id = 0, // Let database assign new ID
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        val newId = productDao.insertProduct(newProduct)
                        productsAdded++
                        newId
                    }
                    
                    // Add product to package (if not already there)
                    try {
                        addProductToPackage(packageId, productId)
                    } catch (e: Exception) {
                        // Ignore if already in package
                    }
                    
                } catch (e: Exception) {
                    errors.add("Error importing product ${importedProduct.name}: ${e.message}")
                }
            }
            
            return PackageImportResult(
                success = errors.isEmpty(),
                productsAdded = productsAdded,
                productsUpdated = productsUpdated,
                errors = errors
            )
            
        } catch (e: Exception) {
            return PackageImportResult(
                success = false,
                errors = listOf("Package import failed: ${e.message}")
            )
        }
    }
    
    suspend fun isProductInPackage(packageId: Long, productId: Long): Boolean {
        return packageDao.isProductInPackage(packageId, productId)
    }
    
    /**
     * Update package status
     */
    suspend fun updatePackageStatus(packageId: Long, newStatus: String) {
        val packageEntity = getPackageById(packageId).first() ?: return
        
        val updatedPackage = when (newStatus) {
            CategoryHelper.PackageStatus.ISSUED -> packageEntity.copy(
                status = newStatus,
                shippedAt = System.currentTimeMillis()
            )
            CategoryHelper.PackageStatus.READY -> packageEntity.copy(
                status = newStatus
            )
            CategoryHelper.PackageStatus.RETURNED -> packageEntity.copy(
                status = newStatus,
                returnedAt = System.currentTimeMillis()
            )
            CategoryHelper.PackageStatus.WAREHOUSE -> packageEntity.copy(
                status = newStatus
            )
            else -> packageEntity.copy(status = newStatus)
        }
        updatePackage(updatedPackage)
        // Record package status change per-product
        val products = getProductsInPackage(packageId).first()
        products.forEach { product ->
            deviceMovementRepository?.recordPackageStatusChange(product.id, packageId, newStatus)
        }
    }
    
    /**
     * Update package contractor
     */
    suspend fun updatePackageContractor(packageId: Long, contractorId: Long?) {
        val packageEntity = getPackageById(packageId).first() ?: return
        val updatedPackage = packageEntity.copy(contractorId = contractorId)
        updatePackage(updatedPackage)
    }
}
