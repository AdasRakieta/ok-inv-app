package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.CompanyDao
import com.example.inventoryapp.data.local.entities.CompanyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CompanyRepository(private val companyDao: CompanyDao) {

    fun getAllCompaniesFlow(): Flow<List<CompanyEntity>> = companyDao.getAllFlow()

    suspend fun getAllCompanies(): List<CompanyEntity> = withContext(Dispatchers.IO) {
        companyDao.getAll()
    }

    suspend fun getCompanyById(id: Long): CompanyEntity? = withContext(Dispatchers.IO) {
        companyDao.getById(id)
    }

    suspend fun getCompanyByTaxId(taxId: String?): CompanyEntity? = withContext(Dispatchers.IO) {
        companyDao.getByTaxId(taxId)
    }

    suspend fun searchCompanies(searchQuery: String?): List<CompanyEntity> = withContext(Dispatchers.IO) {
        companyDao.search(searchQuery)
    }

    suspend fun insertCompany(company: CompanyEntity): Long = withContext(Dispatchers.IO) {
        companyDao.insert(company)
    }

    suspend fun updateCompany(company: CompanyEntity) = withContext(Dispatchers.IO) {
        companyDao.update(company.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCompany(company: CompanyEntity) = withContext(Dispatchers.IO) {
        companyDao.delete(company)
    }

    suspend fun deleteCompanies(ids: List<Long>) = withContext(Dispatchers.IO) {
        companyDao.deleteByIds(ids)
    }
}
