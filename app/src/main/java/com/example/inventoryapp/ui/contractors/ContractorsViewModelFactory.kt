package com.example.inventoryapp.ui.contractors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.inventoryapp.data.repository.ContractorRepository

class ContractorsViewModelFactory(private val contractorRepository: ContractorRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContractorsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContractorsViewModel(contractorRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}