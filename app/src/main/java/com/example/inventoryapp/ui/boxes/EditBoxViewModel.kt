package com.example.inventoryapp.ui.boxes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for editing a box.
 */
class EditBoxViewModel(
    private val boxRepository: BoxRepository,
    private val productRepository: ProductRepository,
    private val boxId: Long
) : ViewModel() {

    // Box data
    private val _box = MutableStateFlow<BoxEntity?>(null)
    val box: StateFlow<BoxEntity?> = _box.asStateFlow()

    // All available products
    private val _allProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val allProducts: StateFlow<List<ProductEntity>> = _allProducts.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected category filter (null means all categories)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // Filtered products based on search and category
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        _allProducts,
        _searchQuery,
        _selectedCategoryId
    ) { products, query, categoryId ->
        products.filter { product ->
            // Filter by search query
            val matchesSearch = query.isBlank() || 
                product.serialNumber?.contains(query, ignoreCase = true) == true ||
                product.name.contains(query, ignoreCase = true)
            
            // Filter by category
            val matchesCategory = categoryId == null || product.categoryId == categoryId
            
            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected product IDs
    private val _selectedProductIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedProductIds: StateFlow<Set<Long>> = _selectedProductIds.asStateFlow()

    // Box name input
    private val _boxName = MutableStateFlow("")
    val boxName: StateFlow<String> = _boxName.asStateFlow()

    // Box description input
    private val _boxDescription = MutableStateFlow("")
    val boxDescription: StateFlow<String> = _boxDescription.asStateFlow()

    // Warehouse location input
    private val _warehouseLocation = MutableStateFlow("")
    val warehouseLocation: StateFlow<String> = _warehouseLocation.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success flag (box saved)
    private val _boxSaved = MutableStateFlow(false)
    val boxSaved: StateFlow<Boolean> = _boxSaved.asStateFlow()

    init {
        loadBox()
        loadProducts()
        loadBoxProducts()
    }

    /**
     * Load box from database
     */
    private fun loadBox() {
        viewModelScope.launch {
            boxRepository.getBoxById(boxId).collect { boxEntity ->
                _box.value = boxEntity
                boxEntity?.let {
                    _boxName.value = it.name
                    _boxDescription.value = it.description ?: ""
                    _warehouseLocation.value = it.warehouseLocation ?: ""
                }
            }
        }
    }

    /**
     * Load all products from database
     */
    private fun loadProducts() {
        viewModelScope.launch {
            productRepository.getAllProducts().collect { products ->
                _allProducts.value = products
            }
        }
    }

    /**
     * Load products currently in the box
     */
    private fun loadBoxProducts() {
        viewModelScope.launch {
            boxRepository.getProductsInBox(boxId).collect { products ->
                _selectedProductIds.value = products.map { it.id }.toSet()
            }
        }
    }

    /**
     * Toggle product selection
     */
    fun toggleProductSelection(productId: Long) {
        val currentSelection = _selectedProductIds.value.toMutableSet()
        if (currentSelection.contains(productId)) {
            currentSelection.remove(productId)
        } else {
            currentSelection.add(productId)
        }
        _selectedProductIds.value = currentSelection
    }

    /**
     * Update box name
     */
    fun setBoxName(name: String) {
        _boxName.value = name
    }

    /**
     * Update box description
     */
    fun setBoxDescription(description: String) {
        _boxDescription.value = description
    }

    /**
     * Update warehouse location
     */
    fun setWarehouseLocation(location: String) {
        _warehouseLocation.value = location
    }

    /**
     * Update search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Set category filter (null for all categories)
     */
    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    /**
     * Select all products (from filtered list)
     */
    fun selectAll() {
        val currentIds = _selectedProductIds.value.toMutableSet()
        currentIds.addAll(filteredProducts.value.map { it.id })
        _selectedProductIds.value = currentIds
    }

    /**
     * Deselect all products (from filtered list)
     */
    fun deselectAll() {
        val currentIds = _selectedProductIds.value.toMutableSet()
        currentIds.removeAll(filteredProducts.value.map { it.id }.toSet())
        _selectedProductIds.value = currentIds
    }

    /**
     * Save box with updated data
     */
    fun saveBox() {
        viewModelScope.launch {
            try {
                // Validate inputs
                if (_boxName.value.isBlank()) {
                    _errorMessage.value = "Box name is required"
                    return@launch
                }

                val currentBox = _box.value
                if (currentBox == null) {
                    _errorMessage.value = "Box not found"
                    return@launch
                }

                // Update box
                val updatedBox = currentBox.copy(
                    name = _boxName.value,
                    description = _boxDescription.value.takeIf { it.isNotBlank() },
                    warehouseLocation = _warehouseLocation.value.takeIf { it.isNotBlank() }
                )

                boxRepository.updateBox(updatedBox)

                // Update products in box
                // First, get current products
                val currentProductIds = boxRepository.getProductsInBox(boxId).first().map { it.id }.toSet()
                val newProductIds = _selectedProductIds.value

                // Remove products that were deselected
                currentProductIds.filter { it !in newProductIds }.forEach { productId ->
                    boxRepository.removeProductFromBox(boxId, productId)
                }

                // Add products that were newly selected
                newProductIds.filter { it !in currentProductIds }.forEach { productId ->
                    boxRepository.addProductToBox(boxId, productId)
                }

                // Set success flag
                _boxSaved.value = true

            } catch (e: Exception) {
                _errorMessage.value = "Failed to update box: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for creating EditBoxViewModel with dependencies
 */
class EditBoxViewModelFactory(
    private val boxRepository: BoxRepository,
    private val productRepository: ProductRepository,
    private val boxId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditBoxViewModel::class.java)) {
            return EditBoxViewModel(boxRepository, productRepository, boxId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
