package com.example.inventoryapp.ui.boxes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.dao.BoxWithCount
import com.example.inventoryapp.data.repository.BoxRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


/**
 * ViewModel for managing the box list screen.
 * Handles box search, filtering, sorting, and deletion.
 */
class BoxListViewModel(
    private val boxRepository: BoxRepository
) : ViewModel() {

    // Search query for filtering boxes
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Filter by warehouse location
    private val _selectedLocations = MutableStateFlow<Set<String>>(emptySet())
    
    // Sort state
    private val _sortOrder = MutableStateFlow(BoxSortOrder.NAME_ASC)
    val sortOrder: StateFlow<BoxSortOrder> = _sortOrder

    // List of all boxes with their product counts
    private val allBoxes: StateFlow<List<BoxWithCount>> = 
        boxRepository.getAllBoxesWithCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Filtered and sorted boxes
    val filteredBoxes: StateFlow<List<BoxWithCount>> = combine(
        allBoxes,
        _searchQuery,
        _selectedLocations,
        _sortOrder
    ) { boxes, query, locations, sort ->
        var filtered = boxes
        
        // Filter by search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { boxWithCount ->
                boxWithCount.box.name.lowercase().contains(query.lowercase()) ||
                boxWithCount.box.description?.lowercase()?.contains(query.lowercase()) == true ||
                boxWithCount.box.warehouseLocation?.lowercase()?.contains(query.lowercase()) == true
            }
        }
        
        // Filter by location
        if (locations.isNotEmpty()) {
            filtered = filtered.filter { boxWithCount ->
                val location = boxWithCount.box.warehouseLocation
                if (location == null) {
                    "(Unassigned)" in locations
                } else {
                    location in locations
                }
            }
        }
        
        // Sort
        when (sort) {
            BoxSortOrder.NAME_ASC -> filtered.sortedBy { it.box.name.lowercase() }
            BoxSortOrder.NAME_DESC -> filtered.sortedByDescending { it.box.name.lowercase() }
            BoxSortOrder.LOCATION_ASC -> filtered.sortedBy { it.box.warehouseLocation?.lowercase() ?: "" }
            BoxSortOrder.LOCATION_DESC -> filtered.sortedByDescending { it.box.warehouseLocation?.lowercase() ?: "" }
            BoxSortOrder.PRODUCT_COUNT_ASC -> filtered.sortedBy { it.productCount }
            BoxSortOrder.PRODUCT_COUNT_DESC -> filtered.sortedByDescending { it.productCount }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Get all unique locations for filter
    val allLocations: StateFlow<List<String>> = allBoxes.map { boxes ->
        boxes.mapNotNull { it.box.warehouseLocation }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Update search query and filter boxes
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setLocationFilters(locations: Set<String>) {
        _selectedLocations.value = locations
    }
    
    fun setSortOrder(order: BoxSortOrder) {
        _sortOrder.value = order
    }

    /**
     * Delete a box by ID
     */
    fun deleteBox(boxId: Long) {
        viewModelScope.launch {
            boxRepository.deleteBox(boxId)
            // No need to manually reload - Flow will update automatically
        }
    }

    /**
     * Delete multiple boxes
     */
    fun deleteBoxes(boxIds: Set<Long>) {
        viewModelScope.launch {
            boxIds.forEach { boxId ->
                boxRepository.deleteBox(boxId)
            }
        }
    }
}

enum class BoxSortOrder {
    NAME_ASC,
    NAME_DESC,
    LOCATION_ASC,
    LOCATION_DESC,
    PRODUCT_COUNT_ASC,
    PRODUCT_COUNT_DESC
}

/**
 * Factory for creating BoxListViewModel with dependencies
 */
class BoxListViewModelFactory(
    private val boxRepository: BoxRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BoxListViewModel::class.java)) {
            return BoxListViewModel(boxRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
