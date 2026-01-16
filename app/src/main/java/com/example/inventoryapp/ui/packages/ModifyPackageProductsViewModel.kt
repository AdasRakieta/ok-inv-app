package com.example.inventoryapp.ui.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModifyPackageProductsViewModel(
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository,
    private val packageId: Long
) : ViewModel() {

    private val _productsInPackage = MutableStateFlow<List<ProductEntity>>(emptyList())
    val productsInPackage: StateFlow<List<ProductEntity>> = _productsInPackage.asStateFlow()

    init {
        loadProductsInPackage()
    }

    private fun loadProductsInPackage() {
        viewModelScope.launch {
            packageRepository.getProductsInPackage(packageId).collect { products ->
                _productsInPackage.value = products
            }
        }
    }

    suspend fun removeProductsFromPackage(productIds: Set<Long>) {
        productIds.forEach { productId ->
            packageRepository.removeProductFromPackage(packageId, productId)
        }
    }
}

class ModifyPackageProductsViewModelFactory(
    private val productRepository: ProductRepository,
    private val packageRepository: PackageRepository,
    private val packageId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModifyPackageProductsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ModifyPackageProductsViewModel(productRepository, packageRepository, packageId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
