package com.example.inventoryapp.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.dao.CategoryCount
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.utils.AppLogger
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ProductSortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_NEWEST,
    DATE_OLDEST,
    CATEGORY
}

class ProductsViewModel(
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // Changed to Set for multiple category selection
    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<Long>> = _selectedCategoryIds
    
    // Package status filter
    private val _selectedPackageStatuses = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackageStatuses: StateFlow<Set<String>> = _selectedPackageStatuses
    
    private val _sortOrder = MutableStateFlow(ProductSortOrder.NAME_ASC)
    val sortOrder: StateFlow<ProductSortOrder> = _sortOrder

    private val allProducts: StateFlow<List<ProductWithPackage>> = productRepository.getAllProducts()
        .map { products ->
            products.map { product ->
                ProductWithPackage(product, null) // Initially without packages
            }
        }
        .flatMapLatest { products ->
            // For each product, combine with its package
            if (products.isEmpty()) {
                flowOf(emptyList())
            } else {
                val productFlows = products.map { productWithPackage ->
                    packageRepository.getPackageForProduct(productWithPackage.productEntity.id)
                        .map { pkg -> productWithPackage.copy(packageEntity = pkg) }
                }
                combine(productFlows) { it.toList() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val products: StateFlow<List<ProductWithPackage>> = combine(
        allProducts,
        _searchQuery,
        _selectedCategoryIds,
        _selectedPackageStatuses,
        _sortOrder
    ) { products, query, categoryIds, packageStatuses, sortOrder ->
        var filtered = products
        
        // Apply search filter
        if (query.isNotBlank()) {
            filtered = filtered.filter { productWithPackage ->
                val product = productWithPackage.productEntity
                product.name.contains(query, ignoreCase = true) ||
                product.serialNumber?.contains(query, ignoreCase = true) == true
            }
        }
        
        // Apply category filter (multiple selection)
        if (categoryIds.isNotEmpty()) {
            filtered = filtered.filter { it.productEntity.categoryId in categoryIds }
        }
        
        // Apply package status filter (multiple selection)
        if (packageStatuses.isNotEmpty()) {
            filtered = filtered.filter { productWithPackage ->
                val status = productWithPackage.packageEntity?.status ?: "UNASSIGNED"
                status in packageStatuses
            }
        }
        
        // Apply sorting
        when (sortOrder) {
            ProductSortOrder.NAME_ASC -> filtered.sortedBy { it.productEntity.name.toLowerCase() }
            ProductSortOrder.NAME_DESC -> filtered.sortedByDescending { it.productEntity.name.toLowerCase() }
            ProductSortOrder.DATE_NEWEST -> filtered.sortedByDescending { it.productEntity.createdAt }
            ProductSortOrder.DATE_OLDEST -> filtered.sortedBy { it.productEntity.createdAt }
            ProductSortOrder.CATEGORY -> filtered.sortedBy { it.productEntity.categoryId ?: Long.MAX_VALUE }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            AppLogger.logAction("Product Search", "Query: $query")
        }
    }
    
    fun setCategoryFilters(categoryIds: Set<Long>) {
        _selectedCategoryIds.value = categoryIds
        viewModelScope.launch {
            AppLogger.logAction("Product Category Filters", "Categories: ${categoryIds.joinToString()}")
        }
    }
    
    fun setPackageStatusFilters(statuses: Set<String>) {
        _selectedPackageStatuses.value = statuses
        viewModelScope.launch {
            AppLogger.logAction("Product Package Status Filters", "Statuses: ${statuses.joinToString()}")
        }
    }
    
    fun setSortOrder(sortOrder: ProductSortOrder) {
        _sortOrder.value = sortOrder
        viewModelScope.launch {
            AppLogger.logAction("Product Sort", "Sort Order: ${sortOrder.name}")
        }
    }

    fun addProduct(
        name: String,
        categoryId: Long? = null,
        serialNumber: String?, // Nullable for "Other" category
        description: String? = null
    ) {
        viewModelScope.launch {
            // Check for duplicate SN if provided
            if (serialNumber != null) {
                val existingProduct = productRepository.getProductBySerialNumber(serialNumber)
                if (existingProduct != null) {
                    // TODO: Handle duplicate SN error - this should be handled in UI
                    AppLogger.logAction("Duplicate SN Attempted", "SN: $serialNumber")
                    return@launch
                }
            }

            // Check if this is "Other" category and we should aggregate by name
            if (categoryId != null && !CategoryHelper.requiresSerialNumber(categoryId) && serialNumber == null) {
                // Try to find existing product with same name and category
                val existingProduct = productRepository.findProductByNameAndCategory(name, categoryId)
                if (existingProduct != null) {
                    // Increment quantity of existing product
                    val newQuantity = existingProduct.quantity + 1
                    productRepository.updateQuantity(existingProduct.id, newQuantity)
                    AppLogger.logAction("Product Quantity Updated", "Name: $name, New Quantity: $newQuantity")
                    return@launch
                }
            }
            
            // Create new product
            val product = ProductEntity(
                name = name,
                categoryId = categoryId,
                serialNumber = serialNumber,
                description = description,
                quantity = 1
            )
            productRepository.insertProduct(product)
            AppLogger.logAction("Product Added", "Name: $name, SN: ${serialNumber ?: "N/A"}")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepository.deleteProduct(product)
            AppLogger.logAction("Product Deleted", "Name: ${product.name}")
        }
    }
    
    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            productRepository.deleteProductById(productId)
            AppLogger.logAction("Product Deleted", "ID: $productId")
        }
    }

    suspend fun getCategoryStatistics(): List<CategoryStatistic> {
        val counts = productRepository.getCategoryStatistics()
        val allCategories = CategoryHelper.getAllCategories()
        
        // Create map of categoryId -> totalQuantity
        val countMap = counts.associateBy({ it.categoryId }, { it.totalQuantity })
        
        // Return all categories with their counts (0 if not in map)
        return allCategories.map { category ->
            CategoryStatistic(
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.icon,
                count = countMap[category.id] ?: 0
            )
        }
    }
}

class ProductsViewModelFactory(
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductsViewModel(productRepository, packageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
