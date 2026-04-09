package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.utils.MovementHistoryUtils
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    
    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()
    
    fun getProductById(productId: Long): Flow<ProductEntity?> = productDao.getProductById(productId)
    
    suspend fun getProductBySerialNumber(serialNumber: String): ProductEntity? = 
        productDao.getProductBySerialNumber(serialNumber)
    
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> = 
        productDao.getProductsByCategory(categoryId)
    
    fun getProductsByLocation(locationId: Long): Flow<List<ProductEntity>> = 
        productDao.getProductsByLocation(locationId)

    fun getProductsByLocationIncludingBoxes(locationId: Long): Flow<List<ProductEntity>> =
        productDao.getProductsByLocationIncludingBoxes(locationId)

    fun getProductsByBoxId(boxId: Long): Flow<List<ProductEntity>> =
        productDao.getProductsByBoxId(boxId)
    
    fun getProductsAssignedToEmployee(employeeId: Long): Flow<List<ProductEntity>> = 
        productDao.getProductsAssignedToEmployee(employeeId)

    fun getProductsAssignedToContractorPoint(contractorPointId: Long): Flow<List<ProductEntity>> =
        productDao.getProductsAssignedToContractorPoint(contractorPointId)
    
    suspend fun getAssignedProductsCount(employeeId: Long): Int = 
        productDao.getAssignedProductsCount(employeeId)
    
    fun getProductsByStatus(status: ProductStatus): Flow<List<ProductEntity>> = 
        productDao.getProductsByStatus(status)
    
    fun searchProducts(query: String): Flow<List<ProductEntity>> = 
        productDao.searchProducts(query)
    
    fun getProductCount(): Flow<Int> = productDao.getProductCount()
    
    fun getProductCountByStatus(status: ProductStatus): Flow<Int> = 
        productDao.getProductCountByStatus(status)
    
    suspend fun insertProduct(product: ProductEntity): Long = 
        productDao.insertProduct(product)
    
    suspend fun insertProducts(products: List<ProductEntity>): List<Long> = 
        productDao.insertProducts(products)
    
    suspend fun updateProduct(product: ProductEntity) = 
        productDao.updateProduct(product)
    
    suspend fun deleteProduct(product: ProductEntity) = 
        productDao.deleteProduct(product)
    
    suspend fun deleteProductById(productId: Long) = 
        productDao.deleteProductById(productId)
    
    suspend fun assignToEmployee(productId: Long, employeeId: Long) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductByIdOnce(productId) ?: return
        val updated = product.copy(
            assignedToEmployeeId = employeeId,
            assignedToContractorPointId = null,
            assignmentDate = now,
            status = ProductStatus.ASSIGNED,
            shelf = null,
            bin = null,
            warehouseLocationId = null,
            boxId = null,
            movementHistory = MovementHistoryUtils.append(
                product.movementHistory,
                MovementHistoryUtils.entryForEmployee("ID $employeeId")
            ),
            updatedAt = now
        )
        productDao.updateProduct(updated)
    }
    
    suspend fun unassignFromEmployee(productId: Long) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductByIdOnce(productId) ?: return
        val updated = product.copy(
            assignedToEmployeeId = null,
            assignedToContractorPointId = null,
            assignmentDate = null,
            status = ProductStatus.UNASSIGNED,
            movementHistory = MovementHistoryUtils.append(product.movementHistory, MovementHistoryUtils.entryUnassigned()),
            updatedAt = now
        )
        productDao.updateProduct(updated)
    }

    suspend fun assignToContractorPoint(productId: Long, contractorPointId: Long, contractorPointName: String? = null) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductByIdOnce(productId) ?: return
        val updated = product.copy(
            assignedToEmployeeId = null,
            assignedToContractorPointId = contractorPointId,
            assignmentDate = now,
            status = ProductStatus.ASSIGNED,
            shelf = null,
            bin = null,
            warehouseLocationId = null,
            boxId = null,
            movementHistory = MovementHistoryUtils.append(
                product.movementHistory,
                MovementHistoryUtils.entryForContractorPoint(contractorPointName ?: "ID $contractorPointId")
            ),
            updatedAt = now
        )
        productDao.updateProduct(updated)
    }

    suspend fun unassignFromContractorPoint(productId: Long) {
        val now = System.currentTimeMillis()
        val product = productDao.getProductByIdOnce(productId) ?: return
        val updated = product.copy(
            assignedToEmployeeId = null,
            assignedToContractorPointId = null,
            assignmentDate = null,
            status = ProductStatus.UNASSIGNED,
            movementHistory = MovementHistoryUtils.append(product.movementHistory, MovementHistoryUtils.entryUnassigned()),
            updatedAt = now
        )
        productDao.updateProduct(updated)
    }

    suspend fun updateWithHistory(product: ProductEntity, historyEntry: String): ProductEntity {
        val updated = product.copy(
            movementHistory = MovementHistoryUtils.append(product.movementHistory, historyEntry),
            updatedAt = System.currentTimeMillis()
        )
        productDao.updateProduct(updated)
        return updated
    }

    // Centralized helpers to assign multiple products to locations or boxes
    suspend fun assignProductsToLocation(products: List<ProductEntity>, locationName: String, warehouseLocationId: Long?) {
        val shelf = locationName.substringBefore("/").trim()
        val bin = locationName.substringAfter("/", "").trim().takeIf { it.isNotEmpty() }
        val now = System.currentTimeMillis()

        products.forEach { product ->
            val updated = product.copy(
                shelf = shelf,
                bin = bin,
                warehouseLocationId = warehouseLocationId,
                boxId = null,
                assignedToEmployeeId = null,
                assignedToContractorPointId = null,
                assignmentDate = null,
                status = ProductStatus.IN_STOCK,
                movementHistory = MovementHistoryUtils.append(product.movementHistory, MovementHistoryUtils.entryForLocation(locationName)),
                updatedAt = now
            )
            productDao.updateProduct(updated)
        }
    }

    suspend fun assignProductsToBox(products: List<ProductEntity>, boxId: Long, warehouseLocationId: Long?, shelf: String?, bin: String?, boxName: String?) {
        val now = System.currentTimeMillis()

        products.forEach { product ->
            val updated = product.copy(
                boxId = boxId,
                warehouseLocationId = warehouseLocationId,
                shelf = shelf,
                bin = bin,
                assignedToEmployeeId = null,
                assignedToContractorPointId = null,
                assignmentDate = null,
                status = ProductStatus.IN_STOCK,
                movementHistory = MovementHistoryUtils.append(product.movementHistory, "Dodano do kartonu ${boxName ?: boxId}"),
                updatedAt = now
            )
            productDao.updateProduct(updated)
        }
    }
}
