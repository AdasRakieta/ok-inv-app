package com.example.inventoryapp.ui.contractors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.data.repository.ContractorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContractorDetailsViewModel(
    private val contractorRepository: ContractorRepository,
    private val contractorId: Long
) : ViewModel() {

    private val _contractor = MutableStateFlow<ContractorEntity?>(null)
    val contractor: StateFlow<ContractorEntity?> = _contractor

    init {
        loadContractor()
    }

    private fun loadContractor() {
        viewModelScope.launch {
            contractorRepository.getContractorById(contractorId).collect {
                _contractor.value = it
            }
        }
    }

    fun updateContractor(contractor: ContractorEntity) {
        viewModelScope.launch {
            contractorRepository.updateContractor(contractor)
        }
    }

    fun deleteContractor(contractor: ContractorEntity) {
        viewModelScope.launch {
            contractorRepository.deleteContractor(contractor)
        }
    }
}
