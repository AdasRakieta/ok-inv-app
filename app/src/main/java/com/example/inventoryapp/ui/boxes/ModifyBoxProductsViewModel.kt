package com.example.inventoryapp.ui.boxes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModifyBoxProductsViewModel(
    private val productRepository: ProductRepository,
    private val boxRepository: BoxRepository,
    private val boxId: Long
) : ViewModel() {

    private val _productsInBox = MutableStateFlow<List<ProductEntity>>(emptyList())
    val productsInBox: StateFlow<List<ProductEntity>> = _productsInBox.asStateFlow()

    init {
        loadProductsInBox()
    }

    private fun loadProductsInBox() {
        viewModelScope.launch {
            boxRepository.getProductsInBox(boxId).collect { products ->
                _productsInBox.value = products
            }
        }
    }

    suspend fun removeProductsFromBox(productIds: Set<Long>) {
        productIds.forEach { productId ->
            boxRepository.removeProductFromBox(boxId, productId)
        }
    }
}

class ModifyBoxProductsViewModelFactory(
    private val productRepository: ProductRepository,
    private val boxRepository: BoxRepository,
    private val boxId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModifyBoxProductsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ModifyBoxProductsViewModel(productRepository, boxRepository, boxId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
