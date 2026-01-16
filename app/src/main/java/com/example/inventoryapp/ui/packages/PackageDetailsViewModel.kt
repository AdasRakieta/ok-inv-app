package com.example.inventoryapp.ui.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.models.AddProductResult
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PackageDetailsViewModel(
    private val packageRepository: PackageRepository,
    val productRepository: ProductRepository,
    private val contractorRepository: ContractorRepository,
    private val packageId: Long
) : ViewModel() {

    private val _packageEntity = MutableStateFlow<PackageEntity?>(null)
    val packageEntity: StateFlow<PackageEntity?> = _packageEntity.asStateFlow()

    private val _productsInPackage = MutableStateFlow<List<ProductEntity>>(emptyList())
    val productsInPackage: StateFlow<List<ProductEntity>> = _productsInPackage.asStateFlow()

    private val _contractor = MutableStateFlow<ContractorEntity?>(null)
    val contractor: StateFlow<ContractorEntity?> = _contractor.asStateFlow()

    private val _addProductResult = MutableStateFlow<AddProductResult?>(null)
    val addProductResult: StateFlow<AddProductResult?> = _addProductResult.asStateFlow()

    fun resetAddProductResult() {
        _addProductResult.value = null
    }

    init {
        loadPackage()
        loadProducts()
    }

    private fun loadPackage() {
        viewModelScope.launch {
            packageRepository.getPackageById(packageId).collect { pkg ->
                _packageEntity.value = pkg
                // Load contractor if package has one assigned
                pkg?.contractorId?.let { contractorId ->
                    loadContractorById(contractorId)
                } ?: run {
                    _contractor.value = null
                }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            packageRepository.getProductsInPackage(packageId).collect { products ->
                _productsInPackage.value = products
            }
        }
    }

    private fun loadContractorById(contractorId: Long) {
        viewModelScope.launch {
            contractorRepository.getContractorById(contractorId).collect { contractor ->
                _contractor.value = contractor
            }
        }
    }

    fun updatePackageName(name: String) {
        viewModelScope.launch {
            val currentPackage = _packageEntity.value ?: return@launch
            val updatedPackage = currentPackage.copy(name = name)
            packageRepository.updatePackage(updatedPackage)
        }
    }

    fun updatePackageContractor(contractorId: Long?) {
        viewModelScope.launch {
            val currentPackage = _packageEntity.value ?: return@launch
            val updatedPackage = currentPackage.copy(contractorId = contractorId)
            packageRepository.updatePackage(updatedPackage)
        }
    }

    // Ensure contractor is refreshed immediately after updating contractor assignment
    fun updatePackageContractorAndRefresh(contractorId: Long?) {
        viewModelScope.launch {
            val currentPackage = _packageEntity.value ?: return@launch
            val updatedPackage = currentPackage.copy(contractorId = contractorId)
            packageRepository.updatePackage(updatedPackage)
            // Refresh contractor view state
            if (contractorId != null) {
                loadContractorById(contractorId)
            } else {
                _contractor.value = null
            }
        }
    }
    
    fun updatePackageDates(shippedAt: Long?, returnedAt: Long?) {
        viewModelScope.launch {
            val currentPackage = _packageEntity.value ?: return@launch
            val updatedPackage = currentPackage.copy(
                shippedAt = shippedAt,
                returnedAt = returnedAt
            )
            packageRepository.updatePackage(updatedPackage)
        }
    }

    fun addNewProductToPackage(serialNumber: String, categoryId: Long, productName: String? = null) {
        viewModelScope.launch {
            try {
                // Check if product with this serial number already exists
                val existingProduct = productRepository.getProductBySerialNumber(serialNumber)
                
                val productToAdd = if (existingProduct != null) {
                    // Use existing product
                    existingProduct
                } else {
                    // Create new product
                    val finalProductName = productName?.takeIf { it.isNotBlank() } ?: serialNumber
                    val newProduct = ProductEntity(
                        name = finalProductName,
                        categoryId = categoryId,
                        serialNumber = serialNumber
                    )
                    val productId = productRepository.insertProduct(newProduct)
                    newProduct.copy(id = productId)
                }
                
                // Add product to package
                val result = packageRepository.addProductToPackage(packageId, productToAdd.id)
                
                // Return result for fragment to handle
                _addProductResult.value = result
                
            } catch (e: Exception) {
                // Error will be handled in the fragment
                throw e
            }
        }
    }

    fun removeProductFromPackage(productId: Long) {
        viewModelScope.launch {
            try {
                packageRepository.removeProductFromPackage(packageId, productId)
            } catch (e: Exception) {
                // Error will be handled in the fragment
                throw e
            }
        }
    }
    
    suspend fun addProductToPackage(productId: Long): AddProductResult {
        return packageRepository.addProductToPackage(packageId, productId)
    }

    fun updatePackageStatus(newStatus: String) {
        viewModelScope.launch {
            try {
                val currentPackage = _packageEntity.value ?: return@launch
                // Use repository method that records per-product package status change movements
                packageRepository.updatePackageStatus(packageId, newStatus)
            } catch (e: Exception) {
                // Error will be handled in the fragment
                throw e
            }
        }
    }

    /**
     * Archive the package and suspend until completion.
     * Use this from callers that need to wait for the operation to finish.
     */
    suspend fun archivePackageBlocking() {
        packageRepository.archivePackage(packageId)
    }

    /**
     * Unarchive the package and suspend until completion.
     */
    suspend fun unarchivePackageBlocking() {
        packageRepository.unarchivePackage(packageId)
    }

    fun deletePackage() {
        viewModelScope.launch {
            val currentPackage = _packageEntity.value ?: return@launch
            packageRepository.deletePackage(currentPackage)
        }
    }
}

class PackageDetailsViewModelFactory(
    private val packageRepository: PackageRepository,
    private val productRepository: ProductRepository,
    private val contractorRepository: ContractorRepository,
    private val packageId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackageDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PackageDetailsViewModel(packageRepository, productRepository, contractorRepository, packageId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
