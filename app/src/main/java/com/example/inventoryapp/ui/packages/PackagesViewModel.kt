package com.example.inventoryapp.ui.packages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.dao.PackageWithCount
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.data.repository.PackageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PackagesViewModel(
    private val packageRepository: PackageRepository,
    private val contractorRepository: ContractorRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // Filter states
    private val _selectedStatuses = MutableStateFlow<Set<String>>(emptySet())
    private val _selectedContractorIds = MutableStateFlow<Set<Long>>(emptySet())
    
    // Sort state
    private val _sortOrder = MutableStateFlow(PackageSortOrder.NAME_ASC)
    val sortOrder: StateFlow<PackageSortOrder> = _sortOrder

    // Combine packages with their product counts and contractors
    private val allPackagesWithCount: StateFlow<List<PackageWithCountAndContractor>> = 
        combine(
            packageRepository.getAllPackagesWithCount(),
            contractorRepository.getAllContractors()
        ) { packagesWithCount, contractors ->
            packagesWithCount.map { packageWithCount ->
                val contractor = packageWithCount.packageEntity.contractorId?.let { contractorId ->
                    contractors.find { it.id == contractorId }
                }
                PackageWithCountAndContractor(packageWithCount, contractor)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val packagesWithCount: StateFlow<List<PackageWithCountAndContractor>> = combine(
        allPackagesWithCount,
        _searchQuery,
        _selectedStatuses,
        _selectedContractorIds,
        _sortOrder
    ) { packages, query, statuses, contractorIds, sort ->
        var filtered = packages
        
        // Filter by search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { item ->
                item.packageWithCount.packageEntity.name.contains(query, ignoreCase = true) ||
                item.packageWithCount.packageEntity.status.contains(query, ignoreCase = true) ||
                item.contractor?.name?.contains(query, ignoreCase = true) == true
            }
        }
        
        // Filter by status
        if (statuses.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.packageWithCount.packageEntity.status in statuses
            }
        }
        
        // Filter by contractor
        if (contractorIds.isNotEmpty()) {
            filtered = filtered.filter { item ->
                val contractorId = item.packageWithCount.packageEntity.contractorId
                if (contractorId == null) {
                    // Include unassigned if "UNASSIGNED" (represented by -1L) is selected
                    -1L in contractorIds
                } else {
                    contractorId in contractorIds
                }
            }
        }
        
        // Sort
        when (sort) {
            PackageSortOrder.NAME_ASC -> filtered.sortedBy { it.packageWithCount.packageEntity.name.lowercase() }
            PackageSortOrder.NAME_DESC -> filtered.sortedByDescending { it.packageWithCount.packageEntity.name.lowercase() }
            PackageSortOrder.STATUS_ASC -> filtered.sortedBy { it.packageWithCount.packageEntity.status }
            PackageSortOrder.STATUS_DESC -> filtered.sortedByDescending { it.packageWithCount.packageEntity.status }
            PackageSortOrder.PRODUCT_COUNT_ASC -> filtered.sortedBy { it.packageWithCount.productCount }
            PackageSortOrder.PRODUCT_COUNT_DESC -> filtered.sortedByDescending { it.packageWithCount.productCount }
            PackageSortOrder.DATE_ASC -> filtered.sortedBy { it.packageWithCount.packageEntity.createdAt }
            PackageSortOrder.DATE_DESC -> filtered.sortedByDescending { it.packageWithCount.packageEntity.createdAt }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Get all contractors for filter dialog
    val allContractors = contractorRepository.getAllContractors()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setStatusFilters(statuses: Set<String>) {
        _selectedStatuses.value = statuses
    }
    
    fun setContractorFilters(contractorIds: Set<Long>) {
        _selectedContractorIds.value = contractorIds
    }
    
    fun setSortOrder(order: PackageSortOrder) {
        _sortOrder.value = order
    }

    fun createPackage(name: String, status: String = "PREPARATION") {
        viewModelScope.launch {
            val packageEntity = PackageEntity(
                name = name,
                status = status
            )
            packageRepository.insertPackage(packageEntity)
        }
    }

    fun deletePackage(packageEntity: PackageEntity) {
        viewModelScope.launch {
            packageRepository.deletePackage(packageEntity)
        }
    }
    
    fun deletePackage(packageId: Long) {
        viewModelScope.launch {
            packageRepository.deletePackageById(packageId)
        }
    }
    
    /**
     * Archive packages (only RETURNED packages can be archived)
     */
    fun archivePackages(packageIds: List<Long>) {
        viewModelScope.launch {
            packageRepository.archivePackages(packageIds)
        }
    }
    
    /**
     * Get package by ID for validation
     */
    suspend fun getPackageById(packageId: Long): PackageEntity? {
        return packageRepository.getPackageById(packageId).first()
    }
    
    /**
     * Bulk update status for multiple packages
     */
    fun bulkUpdateStatus(packageIds: Set<Long>, newStatus: String) {
        viewModelScope.launch {
            packageIds.forEach { packageId ->
                packageRepository.updatePackageStatus(packageId, newStatus)
            }
        }
    }
    
    /**
     * Bulk update contractor for multiple packages
     */
    fun bulkUpdateContractor(packageIds: Set<Long>, contractorId: Long?) {
        viewModelScope.launch {
            packageIds.forEach { packageId ->
                packageRepository.updatePackageContractor(packageId, contractorId)
            }
        }
    }
}

enum class PackageSortOrder {
    NAME_ASC,
    NAME_DESC,
    STATUS_ASC,
    STATUS_DESC,
    PRODUCT_COUNT_ASC,
    PRODUCT_COUNT_DESC,
    DATE_ASC,
    DATE_DESC
}

class PackagesViewModelFactory(
    private val packageRepository: PackageRepository,
    private val contractorRepository: ContractorRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackagesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PackagesViewModel(packageRepository, contractorRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
