package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ContractorPointDao
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.PointType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContractorPointRepository(private val contractorPointDao: ContractorPointDao) {

    fun getAllContractorPointsFlow(): Flow<List<ContractorPointEntity>> = contractorPointDao.getAllFlow()

    suspend fun getAllContractorPoints(): List<ContractorPointEntity> = withContext(Dispatchers.IO) {
        contractorPointDao.getAll()
    }

    suspend fun getContractorPointById(id: Long): ContractorPointEntity? = withContext(Dispatchers.IO) {
        contractorPointDao.getById(id)
    }

    suspend fun getContractorPointByCode(code: String): ContractorPointEntity? = withContext(Dispatchers.IO) {
        contractorPointDao.getByCode(code)
    }

    suspend fun getContractorPointsByType(pointType: PointType): List<ContractorPointEntity> = withContext(Dispatchers.IO) {
        contractorPointDao.getByPointType(pointType)
    }

    suspend fun getContractorPointsByCompany(companyId: Long): List<ContractorPointEntity> = withContext(Dispatchers.IO) {
        contractorPointDao.getByCompany(companyId)
    }

    suspend fun searchContractorPoints(searchQuery: String?): List<ContractorPointEntity> = withContext(Dispatchers.IO) {
        contractorPointDao.search(searchQuery)
    }

    suspend fun insertContractorPoint(contractorPoint: ContractorPointEntity): Long = withContext(Dispatchers.IO) {
        contractorPointDao.insert(contractorPoint)
    }

    suspend fun updateContractorPoint(contractorPoint: ContractorPointEntity) = withContext(Dispatchers.IO) {
        contractorPointDao.update(contractorPoint.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteContractorPoint(contractorPoint: ContractorPointEntity) = withContext(Dispatchers.IO) {
        contractorPointDao.delete(contractorPoint)
    }

    suspend fun deleteContractorPoints(ids: List<Long>) = withContext(Dispatchers.IO) {
        contractorPointDao.deleteByIds(ids)
    }
}
