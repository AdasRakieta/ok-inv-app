package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.EmployeeDao
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmployeeRepository(private val employeeDao: EmployeeDao) {
    
    suspend fun getAllEmployees(): List<EmployeeEntity> = withContext(Dispatchers.IO) {
        employeeDao.getAll()
    }
    
    suspend fun getEmployeeById(id: Long): EmployeeEntity? = withContext(Dispatchers.IO) {
        employeeDao.getById(id)
    }
    
    suspend fun getEmployeeByEmail(email: String): EmployeeEntity? = withContext(Dispatchers.IO) {
        employeeDao.getByEmail(email)
    }
    
    suspend fun insertEmployee(employee: EmployeeEntity): Long = withContext(Dispatchers.IO) {
        employeeDao.insert(employee)
    }
    
    suspend fun updateEmployee(employee: EmployeeEntity) = withContext(Dispatchers.IO) {
        employeeDao.update(employee.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteEmployee(employee: EmployeeEntity) = withContext(Dispatchers.IO) {
        employeeDao.delete(employee)
    }
}
