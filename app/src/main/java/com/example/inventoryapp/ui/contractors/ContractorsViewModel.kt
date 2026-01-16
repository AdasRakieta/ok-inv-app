package com.example.inventoryapp.ui.contractors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.data.repository.ContractorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ContractorsViewModel(private val contractorRepository: ContractorRepository) : ViewModel() {

    val allContractors: Flow<List<ContractorEntity>> = contractorRepository.getAllContractors()

    fun addContractor(
        name: String,
        phone: String? = null,
        email: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            val contractor = ContractorEntity(
                name = name,
                phone = phone,
                email = email,
                description = description
            )
            contractorRepository.insertContractor(contractor)
        }
    }

    fun updateContractor(contractor: ContractorEntity) {
        viewModelScope.launch {
            val updatedContractor = contractor.copy(updatedAt = System.currentTimeMillis())
            contractorRepository.updateContractor(updatedContractor)
        }
    }

    fun deleteContractor(contractor: ContractorEntity) {
        viewModelScope.launch {
            contractorRepository.deleteContractor(contractor)
        }
    }
}