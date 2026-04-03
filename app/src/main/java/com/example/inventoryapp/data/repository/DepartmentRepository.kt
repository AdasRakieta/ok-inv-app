package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.DepartmentDao
import com.example.inventoryapp.data.local.entities.DepartmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DepartmentRepository(private val departmentDao: DepartmentDao) {

    suspend fun getAll(): List<DepartmentEntity> = withContext(Dispatchers.IO) {
        departmentDao.getAll()
    }

    suspend fun getByCompany(companyId: Long): List<DepartmentEntity> = withContext(Dispatchers.IO) {
        departmentDao.getByCompany(companyId)
    }

    suspend fun getNamesByCompany(companyId: Long): List<String> = withContext(Dispatchers.IO) {
        departmentDao.getNamesByCompany(companyId)
    }

    suspend fun insert(companyId: Long, name: String): Long = withContext(Dispatchers.IO) {
        val existing = departmentDao.getByCompanyAndName(companyId, name)
        if (existing != null) {
            existing.id
        } else {
            departmentDao.insert(DepartmentEntity(companyId = companyId, name = name))
        }
    }

    suspend fun update(id: Long, name: String) = withContext(Dispatchers.IO) {
        val current = departmentDao.getById(id) ?: return@withContext
        departmentDao.update(current.copy(name = name))
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        departmentDao.getById(id)?.let { departmentDao.delete(it) }
    }

    suspend fun deleteByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        departmentDao.deleteByIds(ids)
    }

    suspend fun deleteByCompany(companyId: Long) = withContext(Dispatchers.IO) {
        departmentDao.deleteByCompany(companyId)
    }
}
