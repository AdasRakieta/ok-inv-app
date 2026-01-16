package com.example.inventoryapp.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.DeviceMovementEntity
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.ui.products.models.DisplayMovement
import com.example.inventoryapp.data.repository.DeviceMovementRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductDetailsViewModel(
    private val productRepository: ProductRepository,
    private val deviceMovementRepository: DeviceMovementRepository,
    private val boxRepository: BoxRepository,
    private val packageRepository: PackageRepository,
    private val productId: Long
) : ViewModel() {

    private val _product = MutableStateFlow<ProductEntity?>(null)
    val product: StateFlow<ProductEntity?> = _product.asStateFlow()

    private val _snUpdateError = MutableStateFlow<String?>(null)
    val snUpdateError: StateFlow<String?> = _snUpdateError.asStateFlow()

    private val _movements = MutableStateFlow<List<DisplayMovement>>(emptyList())
    val movements: StateFlow<List<DisplayMovement>> = _movements.asStateFlow()

    init {
        loadProduct()
        loadMovements()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            productRepository.getProductById(productId).collect { productEntity ->
                _product.value = productEntity
            }
        }
    }

    private fun loadMovements() {
        viewModelScope.launch {
            deviceMovementRepository.getMovementsForProduct(productId).collect { list ->
                // Map each DeviceMovementEntity to a DisplayMovement with resolved container names
                val displayList = list.map { mov ->
                    var fromName: String? = null
                    var toName: String? = null

                    when (mov.fromContainerType) {
                        "PACKAGE" -> mov.fromContainerId?.let { id ->
                            val pkg = packageRepository.getPackageById(id).first()
                            fromName = pkg?.name ?: "Package #$id"
                        }
                        "BOX" -> mov.fromContainerId?.let { id ->
                            val box = boxRepository.getBoxById(id).first()
                            fromName = box?.name ?: "Box #$id"
                        }
                        else -> if (mov.fromContainerType != null) fromName = mov.fromContainerType
                    }

                    when (mov.toContainerType) {
                        "PACKAGE" -> mov.toContainerId?.let { id ->
                            val pkg = packageRepository.getPackageById(id).first()
                            toName = pkg?.name ?: "Package #$id"
                        }
                        "BOX" -> mov.toContainerId?.let { id ->
                            val box = boxRepository.getBoxById(id).first()
                            toName = box?.name ?: "Box #$id"
                        }
                        else -> if (mov.toContainerType != null) toName = mov.toContainerType
                    }

                    DisplayMovement(
                        id = mov.id,
                        action = mov.action,
                        fromDisplay = fromName,
                        toDisplay = toName,
                        timestamp = mov.timestamp,
                        packageStatus = mov.packageStatus,
                        note = mov.note
                    )
                }
                _movements.value = displayList
            }
        }
    }

    fun updateSerialNumber(serialNumber: String) {
        viewModelScope.launch {
            val currentProduct = _product.value ?: return@launch
            
            // Check if SN already exists (and it's not our own product)
            val existingProduct = productRepository.getProductBySerialNumber(serialNumber)
            if (existingProduct != null && existingProduct.id != currentProduct.id) {
                _snUpdateError.value = "This Serial Number is already in use"
                return@launch
            }
            
            // Clear error and update
            _snUpdateError.value = null
            val updatedProduct = currentProduct.copy(
                serialNumber = serialNumber,
                updatedAt = System.currentTimeMillis()
            )
            productRepository.updateProduct(updatedProduct)
        }
    }

    fun clearSnError() {
        _snUpdateError.value = null
    }

    fun updateQuantity(newQuantity: Int) {
        viewModelScope.launch {
            val currentProduct = _product.value ?: return@launch
            productRepository.updateQuantity(currentProduct.id, newQuantity)
        }
    }

    fun deleteProduct() {
        viewModelScope.launch {
            val currentProduct = _product.value ?: return@launch
            productRepository.deleteProduct(currentProduct)
        }
    }
}

class ProductDetailsViewModelFactory(
    private val productRepository: ProductRepository,
    private val deviceMovementRepository: DeviceMovementRepository,
    private val boxRepository: BoxRepository,
    private val packageRepository: PackageRepository,
    private val productId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductDetailsViewModel(productRepository, deviceMovementRepository, boxRepository, packageRepository, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
