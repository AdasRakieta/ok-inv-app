package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.EmployeeDao
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EmployeeRepository(private val employeeDao: EmployeeDao) {
    
    fun getAllEmployeesFlow(): Flow<List<EmployeeEntity>> = employeeDao.getAllFlow()
    
    suspend fun getAllEmployees(): List<EmployeeEntity> = withContext(Dispatchers.IO) {
        employeeDao.getAll()
    }
    
    suspend fun getEmployeeById(id: Long): EmployeeEntity? = withContext(Dispatchers.IO) {
        employeeDao.getById(id)
    }
    
    suspend fun getEmployeeByEmail(email: String): EmployeeEntity? = withContext(Dispatchers.IO) {
        employeeDao.getByEmail(email)
    }
    
    fun searchEmployees(searchQuery: String?, department: String?): Flow<List<EmployeeEntity>> = 
        employeeDao.searchEmployees(searchQuery, department)

    suspend fun getEmployeesByCompany(companyId: Long): List<EmployeeEntity> = withContext(Dispatchers.IO) {
        employeeDao.getByCompany(companyId)
    }

    fun searchEmployeesByCompany(companyId: Long, searchQuery: String?, department: String?): Flow<List<EmployeeEntity>> =
        employeeDao.searchEmployeesByCompany(companyId, searchQuery, department)
    
    suspend fun insertEmployee(employee: EmployeeEntity): Long = withContext(Dispatchers.IO) {
        employeeDao.insert(employee)
    }
    
    suspend fun updateEmployee(employee: EmployeeEntity) = withContext(Dispatchers.IO) {
        employeeDao.update(employee.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteEmployee(employee: EmployeeEntity) = withContext(Dispatchers.IO) {
        employeeDao.delete(employee)
    }
    
    suspend fun deleteEmployees(ids: List<Long>) = withContext(Dispatchers.IO) {
        employeeDao.deleteByIds(ids)
    }
    
    suspend fun getAllDepartments(): List<String> = withContext(Dispatchers.IO) {
        employeeDao.getAllDepartments()
    }
}
