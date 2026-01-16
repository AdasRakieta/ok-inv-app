package com.example.inventoryapp.ui.contractors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.inventoryapp.data.repository.ContractorRepository

class ContractorDetailsViewModelFactory(
    private val contractorRepository: ContractorRepository,
    private val contractorId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContractorDetailsViewModel::class.java)) {
            return ContractorDetailsViewModel(contractorRepository, contractorId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
