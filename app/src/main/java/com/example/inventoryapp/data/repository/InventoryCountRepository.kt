package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.InventoryCountDao
import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.dao.SessionWithCount
import com.example.inventoryapp.data.local.entities.InventoryCountItemEntity
import com.example.inventoryapp.data.local.entities.InventoryCountSessionEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for inventory count operations.
 * Handles session management, product scanning, and statistics.
 */
class InventoryCountRepository(
    val inventoryCountDao: InventoryCountDao,
    private val productDao: ProductDao
) {
    suspend fun getItemsForSession(sessionId: Long): List<InventoryCountItemEntity> =
        inventoryCountDao.getItemsForSession(sessionId)
    
    // ===== SESSION OPERATIONS =====
    
    fun getAllSessions(): Flow<List<InventoryCountSessionEntity>> = 
        inventoryCountDao.getAllSessions()
    
    fun getAllSessionsWithCount(): Flow<List<SessionWithCount>> = 
        inventoryCountDao.getAllSessionsWithCount()
    
    fun getSessionById(sessionId: Long): Flow<InventoryCountSessionEntity?> = 
        inventoryCountDao.getSessionById(sessionId)
    
    suspend fun createSession(name: String, notes: String? = null): Long {
        val session = InventoryCountSessionEntity(
            name = name,
            notes = notes,
            status = "IN_PROGRESS"
        )
        return inventoryCountDao.insertSession(session)
    }
    
    suspend fun updateSession(session: InventoryCountSessionEntity) {
        inventoryCountDao.updateSession(session)
    }
    
    suspend fun completeSession(sessionId: Long) {
        inventoryCountDao.completeSession(sessionId, System.currentTimeMillis())
    }
    
    suspend fun deleteSession(session: InventoryCountSessionEntity) {
        inventoryCountDao.deleteSession(session)
    }
    
    suspend fun deleteSessionById(sessionId: Long) {
        inventoryCountDao.deleteSessionById(sessionId)
    }
    
    // ===== PRODUCT SCANNING =====
    
    /**
     * Scan a product by serial number and add it to the session.
     * Returns ScanResult indicating success or failure.
     */
    suspend fun scanProduct(sessionId: Long, serialNumber: String): ScanResult {
        // Check if product exists in database
        val product = productDao.getProductBySerialNumber(serialNumber)
        
        return if (product != null) {
            // Product exists - add to session
            addProductToSession(sessionId, product.id)
            ScanResult.Success(product)
        } else {
            // Product not found
            ScanResult.Error("Product with S/N '$serialNumber' not found in database")
        }
    }
    
    /**
     * Add a product to the session (internal helper).
     */
    private suspend fun addProductToSession(sessionId: Long, productId: Long) {
        // Get next sequence number
        val maxSequence = inventoryCountDao.getMaxSequenceNumber(sessionId) ?: 0
        val nextSequence = maxSequence + 1
        
        // Create and insert item
        val item = InventoryCountItemEntity(
            sessionId = sessionId,
            productId = productId,
            sequenceNumber = nextSequence
        )
        inventoryCountDao.insertItem(item)
    }
    
    // ===== DATA RETRIEVAL =====
    
    fun getProductsInSession(sessionId: Long): Flow<List<ProductEntity>> = 
        inventoryCountDao.getProductsInSession(sessionId)
    
    fun getItemCountInSession(sessionId: Long): Flow<Int> = 
        inventoryCountDao.getItemCountInSession(sessionId)
    
    fun getCategoryStatistics(sessionId: Long): Flow<Map<Long, Int>> = 
        inventoryCountDao.getCategoryStatistics(sessionId)
    
    suspend fun clearSession(sessionId: Long) {
        inventoryCountDao.deleteAllItemsInSession(sessionId)
    }
}

/**
 * Result of scanning a product.
 */
sealed class ScanResult {
    data class Success(val product: ProductEntity) : ScanResult()
    data class Error(val message: String) : ScanResult()
}
