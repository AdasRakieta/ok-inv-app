package com.example.inventoryapp.ui.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.models.AddProductResult
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductSelectionViewModel(
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository,
    private val packageId: Long
) : ViewModel() {

    private val _availableProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val availableProducts: StateFlow<List<ProductEntity>> = _availableProducts.asStateFlow()

    private var allAvailableProducts: List<ProductEntity> = emptyList()
    private var searchQuery: String = ""
    private var categoryFilter: String? = null

    init {
        loadAvailableProducts()
    }

    private fun loadAvailableProducts() {
        viewModelScope.launch {
            // Get all products
            productRepository.getAllProducts().collect { allProducts ->
                // Get products already in package
                packageRepository.getProductsInPackage(packageId).collect { productsInPackage ->
                    val productsInPackageIds = productsInPackage.map { it.id }.toSet()
                    // Filter out products already in package
                    allAvailableProducts = allProducts.filter { 
                        it.id !in productsInPackageIds 
                    }
                    applyFilters()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    fun setCategoryFilter(category: String?) {
        categoryFilter = category
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allAvailableProducts

        // Apply search query
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                (product.serialNumber?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        // Apply category filter
        if (categoryFilter != null) {
            val categoryId = com.example.inventoryapp.utils.CategoryHelper.getCategoryIdByName(categoryFilter!!)
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        _availableProducts.value = filtered
    }

    suspend fun addProductsToPackage(productIds: Set<Long>): List<AddProductResult> {
        return productIds.map { productId: Long ->
            packageRepository.addProductToPackage(packageId, productId)
        }
    }
}

class ProductSelectionViewModelFactory(
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository,
    private val packageId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductSelectionViewModel(productRepository, packageRepository, packageId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
