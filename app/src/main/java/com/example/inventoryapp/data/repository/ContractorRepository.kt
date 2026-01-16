package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ContractorDao
import com.example.inventoryapp.data.local.entities.ContractorEntity
import kotlinx.coroutines.flow.Flow

class ContractorRepository(private val contractorDao: ContractorDao) {

    fun getAllContractors(): Flow<List<ContractorEntity>> = contractorDao.getAllContractors()

    fun getContractorById(contractorId: Long): Flow<ContractorEntity?> = contractorDao.getContractorById(contractorId)

    suspend fun getContractorByName(name: String): ContractorEntity? =
        contractorDao.getContractorByName(name)

    suspend fun insertContractor(contractor: ContractorEntity): Long {
        // Check if contractor with same name already exists
        val existing = contractorDao.getContractorByName(contractor.name)
        if (existing != null) {
            throw IllegalArgumentException("Contractor with name '${contractor.name}' already exists")
        }
        return contractorDao.insertContractor(contractor)
    }

    suspend fun updateContractor(contractor: ContractorEntity) = contractorDao.updateContractor(contractor)

    suspend fun deleteContractor(contractor: ContractorEntity) = contractorDao.deleteContractor(contractor)

    suspend fun getContractorCount(): Int = contractorDao.getContractorCount()
}