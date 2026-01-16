package com.example.inventoryapp.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.dao.PackageWithCount
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.ui.packages.PackageWithCountAndContractor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for Archive screen - manages archived packages display, filtering, and sorting
 * Matches functionality of BoxListViewModel for consistency
 */
class ArchiveViewModel(
    private val packageRepository: PackageRepository,
    private val contractorRepository: ContractorRepository
) : ViewModel() {

    // Search query for filtering archived packages
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Filter by status
    private val _selectedStatuses = MutableStateFlow<Set<String>>(emptySet())
    
    // Sort state
    private val _sortOrder = MutableStateFlow(ArchiveSortOrder.NAME_ASC)
    val sortOrder: StateFlow<ArchiveSortOrder> = _sortOrder

    // All archived packages from database
    private val allArchivedPackages = packageRepository.getArchivedPackagesWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered and sorted packages (mapped to include contractor)
    val filteredPackages: StateFlow<List<PackageWithCountAndContractor>> = combine(
        allArchivedPackages,
        _searchQuery,
        _selectedStatuses,
        _sortOrder
    ) { packages, query, statuses, sort ->
        var filtered = packages
        
        // Filter by search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { pkgWithCount ->
                pkgWithCount.packageEntity.name.contains(query, ignoreCase = true) ||
                pkgWithCount.packageEntity.status.contains(query, ignoreCase = true)
            }
        }
        
        // Filter by status
        if (statuses.isNotEmpty()) {
            filtered = filtered.filter { pkgWithCount ->
                pkgWithCount.packageEntity.status in statuses
            }
        }
        
        // Sort
        val sorted = when (sort) {
            ArchiveSortOrder.NAME_ASC -> filtered.sortedBy { it.packageEntity.name.lowercase() }
            ArchiveSortOrder.NAME_DESC -> filtered.sortedByDescending { it.packageEntity.name.lowercase() }
            ArchiveSortOrder.STATUS_ASC -> filtered.sortedBy { it.packageEntity.status }
            ArchiveSortOrder.STATUS_DESC -> filtered.sortedByDescending { it.packageEntity.status }
            ArchiveSortOrder.PRODUCT_COUNT_ASC -> filtered.sortedBy { it.productCount }
            ArchiveSortOrder.PRODUCT_COUNT_DESC -> filtered.sortedByDescending { it.productCount }
            ArchiveSortOrder.DATE_ASC -> filtered.sortedBy { it.packageEntity.createdAt }
            ArchiveSortOrder.DATE_DESC -> filtered.sortedByDescending { it.packageEntity.createdAt }
        }
        
        // Map to include contractor info
        sorted.map { pkgWithCount ->
            val contractor = pkgWithCount.packageEntity.contractorId?.let { contractorId ->
                contractorRepository.getContractorById(contractorId).first()
            }
            PackageWithCountAndContractor(pkgWithCount, contractor)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Get all unique statuses for filter
    val allStatuses: StateFlow<List<String>> = allArchivedPackages.map { packages ->
        packages.map { it.packageEntity.status }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Set search query for filtering packages
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setStatusFilters(statuses: Set<String>) {
        _selectedStatuses.value = statuses
    }
    
    fun setSortOrder(order: ArchiveSortOrder) {
        _sortOrder.value = order
    }

    /**
     * Unarchive a package (restore to active packages)
     */
    fun unarchivePackage(packageId: Long) {
        viewModelScope.launch {
            packageRepository.unarchivePackage(packageId)
        }
    }
    
    /**
     * Unarchive multiple packages
     */
    fun unarchivePackages(packageIds: Set<Long>) {
        viewModelScope.launch {
            packageIds.forEach { packageId ->
                packageRepository.unarchivePackage(packageId)
            }
        }
    }
    
    /**
     * Delete multiple packages permanently
     */
    fun deletePackages(packageIds: Set<Long>) {
        viewModelScope.launch {
            packageIds.forEach { packageId ->
                packageRepository.deletePackageById(packageId)
            }
        }
    }
}

enum class ArchiveSortOrder {
    NAME_ASC,
    NAME_DESC,
    STATUS_ASC,
    STATUS_DESC,
    PRODUCT_COUNT_ASC,
    PRODUCT_COUNT_DESC,
    DATE_ASC,
    DATE_DESC
}

/**
 * Factory for creating ArchiveViewModel with dependencies
 */
class ArchiveViewModelFactory(
    private val packageRepository: PackageRepository,
    private val contractorRepository: ContractorRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            return ArchiveViewModel(packageRepository, contractorRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
