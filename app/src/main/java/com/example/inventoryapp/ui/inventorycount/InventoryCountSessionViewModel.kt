package com.example.inventoryapp.ui.inventorycount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.InventoryCountSessionEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductWithPackageInfo
import com.example.inventoryapp.data.repository.InventoryCountRepository
import com.example.inventoryapp.data.repository.ScanResult
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository

/**
 * ViewModel for Inventory Count session details.
 * Manages product scanning, statistics, and session completion.
 */
class InventoryCountSessionViewModel(
    private val inventoryCountRepository: InventoryCountRepository,
    private val packageRepository: PackageRepository,
    private val productRepository: ProductRepository,
    private val sessionId: Long
) : ViewModel() {

    // Current session
    private val _session = MutableStateFlow<InventoryCountSessionEntity?>(null)
    val session: StateFlow<InventoryCountSessionEntity?> = _session.asStateFlow()

    // Scanned products in this session with package info
    private val _scannedProducts = MutableStateFlow<List<ProductWithPackageInfo>>(emptyList())
    val scannedProducts: StateFlow<List<ProductWithPackageInfo>> = _scannedProducts.asStateFlow()

    // Total count of scanned items
    val totalCount: StateFlow<Int> = _scannedProducts.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Category statistics (category name -> count)
    val categoryStatistics: StateFlow<Map<String, Int>> = 
        inventoryCountRepository.getCategoryStatistics(sessionId)
            .map { categoryIdMap ->
                // Convert categoryId -> count to categoryName -> count
                categoryIdMap.mapKeys { (categoryId, _) ->
                    CategoryHelper.getCategoryById(categoryId)?.name ?: "Unknown"
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

    // Last scan result
    private val _lastScanResult = MutableStateFlow<ScanResult?>(null)
    val lastScanResult: StateFlow<ScanResult?> = _lastScanResult.asStateFlow()

    init {
        loadSession()
        loadProducts()
    }

    private fun loadSession() {
        viewModelScope.launch {
            inventoryCountRepository.getSessionById(sessionId).collect { session ->
                _session.value = session
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            inventoryCountRepository.getProductsInSession(sessionId).collect { products ->
                // Get package info for each scanned product
                val productsWithPackageInfo = products.map { product ->
                    val packageInfo = packageRepository.getPackageForProduct(product.id).first()
                    ProductWithPackageInfo(product, packageInfo)
                }
                _scannedProducts.value = productsWithPackageInfo
            }
        }
    }

    /**
     * Scan a product by serial number.
     * Validates if product exists in database before adding to session.
     */
    suspend fun scanProduct(serialNumber: String): ScanResult {
        val result = inventoryCountRepository.scanProduct(sessionId, serialNumber)
        
        if (result is ScanResult.Success) {
            // Immediately update the scanned products list for real-time UI update
            val products = inventoryCountRepository.getProductsInSession(sessionId).first()
            val productsWithPackageInfo = products.map { product ->
                val packageInfo = packageRepository.getPackageForProduct(product.id).first()
                ProductWithPackageInfo(product, packageInfo)
            }
            _scannedProducts.value = productsWithPackageInfo
        }
        
        _lastScanResult.value = result
        return result
    }

    /**
     * Complete the session (mark as COMPLETED).
     */
    fun completeSession() {
        viewModelScope.launch {
            inventoryCountRepository.completeSession(sessionId)
        }
    }

    /**
     * Clear all scanned products from session.
     */
    fun clearSession() {
        viewModelScope.launch {
            inventoryCountRepository.clearSession(sessionId)
        }
    }

    /**
     * Clear last scan result message.
     */
    fun clearScanResult() {
        _lastScanResult.value = null
    }

    /**
     * Get detailed category statistics for dialog display.
     */
    suspend fun getDetailedStatistics(): List<InventoryCountStatistic> {
        val categoryMap = inventoryCountRepository.getCategoryStatistics(sessionId).first()
        
        return categoryMap.map { (categoryId, count) ->
            val category = CategoryHelper.getCategoryById(categoryId)
            InventoryCountStatistic(
                categoryId = categoryId,
                categoryName = category?.name ?: "Unknown",
                categoryIcon = category?.icon ?: "📦",
                count = count
            )
        }.sortedByDescending { it.count }
    }

    /**
     * Get all available packages for bulk assignment.
     */
    suspend fun getAllPackages(): List<com.example.inventoryapp.data.local.entities.PackageEntity> {
        return packageRepository.getAllPackages().first()
    }

    /**
     * Assign a product to a package.
     */
    suspend fun assignProductToPackage(productId: Long, packageId: Long) {
        packageRepository.addProductToPackage(packageId, productId)
    }

    /**
     * Remove a product from any package (unassign).
     */
    suspend fun unassignProductFromPackage(productId: Long) {
        // Find which package the product is in
        val packageEntity = packageRepository.getPackageForProduct(productId).first()
        if (packageEntity != null) {
            packageRepository.removeProductFromPackage(packageEntity.id, productId)
        }
    }

    /**
     * Get all products in the database (for finding missing products).
     */
    suspend fun getAllProducts(): List<ProductEntity> {
        return productRepository.getAllProducts().first()
    }

    /**
     * Get missing products (products in database but not scanned in this session).
     */
    suspend fun getMissingProducts(): List<ProductWithPackageInfo> {
        val allProducts = getAllProducts()
        val scannedProducts = scannedProducts.value
        val scannedIds = scannedProducts.map { it.product.id }.toSet()

        val missingProducts = allProducts.filter { it.id !in scannedIds }

        // Get package info for each missing product
        return missingProducts.map { product ->
            val packageInfo = packageRepository.getPackageForProduct(product.id).first()
            ProductWithPackageInfo(product, packageInfo)
        }
    }
}

class InventoryCountSessionViewModelFactory(
    private val inventoryCountRepository: InventoryCountRepository,
    private val packageRepository: PackageRepository,
    private val productRepository: ProductRepository,
    private val sessionId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryCountSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryCountSessionViewModel(inventoryCountRepository, packageRepository, productRepository, sessionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
