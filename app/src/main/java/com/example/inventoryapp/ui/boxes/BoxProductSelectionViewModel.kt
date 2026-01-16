package com.example.inventoryapp.ui.boxes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.models.AddProductResult
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BoxProductSelectionViewModel(
    private val productRepository: ProductRepository,
    private val boxRepository: BoxRepository,
    private val boxId: Long
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
                // Get products already in box
                boxRepository.getProductsInBox(boxId).collect { productsInBox ->
                    val productsInBoxIds = productsInBox.map { it.id }.toSet()
                    // Filter out products already in box
                    allAvailableProducts = allProducts.filter { 
                        it.id !in productsInBoxIds 
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

    suspend fun addProductsToBox(productIds: Set<Long>): List<AddProductResult> {
        return productIds.map { productId: Long ->
            boxRepository.addProductToBox(boxId, productId)
        }
    }
}

class BoxProductSelectionViewModelFactory(
    private val productRepository: ProductRepository,
    private val boxRepository: BoxRepository,
    private val boxId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BoxProductSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BoxProductSelectionViewModel(productRepository, boxRepository, boxId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
