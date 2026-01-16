package com.example.inventoryapp.ui.inventorycount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.dao.SessionWithCount
import com.example.inventoryapp.data.local.entities.InventoryCountSessionEntity
import com.example.inventoryapp.data.repository.InventoryCountRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Inventory Count sessions list.
 * Manages session creation, deletion, search/filter, and sorting.
 */
class InventoryCountListViewModel(
    private val inventoryCountRepository: InventoryCountRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Filter by status
    private val _selectedStatuses = MutableStateFlow<Set<String>>(emptySet())
    
    // Sort state
    private val _sortOrder = MutableStateFlow(SessionSortOrder.DATE_DESC)
    val sortOrder: StateFlow<SessionSortOrder> = _sortOrder

    // All sessions from database with item count
    private val allSessions: StateFlow<List<SessionWithCount>> = 
        inventoryCountRepository.getAllSessionsWithCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Filtered and sorted sessions
    val sessions: StateFlow<List<SessionWithCount>> = combine(
        allSessions,
        _searchQuery,
        _selectedStatuses,
        _sortOrder
    ) { sessions, query, statuses, sort ->
        var filtered = sessions
        
        // Filter by search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { sessionWithCount ->
                sessionWithCount.session.name.contains(query, ignoreCase = true) ||
                sessionWithCount.session.status.contains(query, ignoreCase = true) ||
                sessionWithCount.session.notes?.contains(query, ignoreCase = true) == true
            }
        }
        
        // Filter by status
        if (statuses.isNotEmpty()) {
            filtered = filtered.filter { sessionWithCount ->
                sessionWithCount.session.status in statuses
            }
        }
        
        // Sort
        when (sort) {
            SessionSortOrder.NAME_ASC -> filtered.sortedBy { it.session.name.lowercase() }
            SessionSortOrder.NAME_DESC -> filtered.sortedByDescending { it.session.name.lowercase() }
            SessionSortOrder.STATUS_ASC -> filtered.sortedBy { it.session.status }
            SessionSortOrder.STATUS_DESC -> filtered.sortedByDescending { it.session.status }
            SessionSortOrder.ITEM_COUNT_ASC -> filtered.sortedBy { it.itemCount }
            SessionSortOrder.ITEM_COUNT_DESC -> filtered.sortedByDescending { it.itemCount }
            SessionSortOrder.DATE_ASC -> filtered.sortedBy { it.session.createdAt }
            SessionSortOrder.DATE_DESC -> filtered.sortedByDescending { it.session.createdAt }
        }
    }.stateIn(
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
    
    fun setSortOrder(order: SessionSortOrder) {
        _sortOrder.value = order
    }

    fun createSession(name: String, notes: String? = null) {
        viewModelScope.launch {
            inventoryCountRepository.createSession(name, notes)
        }
    }

    fun deleteSession(session: InventoryCountSessionEntity) {
        viewModelScope.launch {
            inventoryCountRepository.deleteSession(session)
        }
    }

    fun deleteSessionById(sessionId: Long) {
        viewModelScope.launch {
            inventoryCountRepository.deleteSessionById(sessionId)
        }
    }
}

enum class SessionSortOrder {
    NAME_ASC,
    NAME_DESC,
    STATUS_ASC,
    STATUS_DESC,
    ITEM_COUNT_ASC,
    ITEM_COUNT_DESC,
    DATE_ASC,
    DATE_DESC
}

class InventoryCountListViewModelFactory(
    private val inventoryCountRepository: InventoryCountRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryCountListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryCountListViewModel(inventoryCountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
