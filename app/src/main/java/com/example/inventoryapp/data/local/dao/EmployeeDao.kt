package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY firstName ASC, lastName ASC")
    fun getAllFlow(): Flow<List<EmployeeEntity>>
    
    @Query("SELECT * FROM employees ORDER BY firstName ASC, lastName ASC")
    suspend fun getAll(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): EmployeeEntity?
    
    @Query("""
        SELECT * FROM employees 
        WHERE (:searchQuery IS NULL OR :searchQuery = '' OR 
               firstName LIKE '%' || :searchQuery || '%' OR 
               lastName LIKE '%' || :searchQuery || '%' OR
               department LIKE '%' || :searchQuery || '%' OR
               position LIKE '%' || :searchQuery || '%')
        AND (:department IS NULL OR :department = '' OR department = :department)
        ORDER BY firstName ASC, lastName ASC
    """)
    fun searchEmployees(searchQuery: String?, department: String?): Flow<List<EmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(employee: EmployeeEntity): Long

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Delete
    suspend fun delete(employee: EmployeeEntity)
    
    @Query("DELETE FROM employees WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    @Query("SELECT DISTINCT department FROM employees WHERE department IS NOT NULL AND department != '' ORDER BY department ASC")
    suspend fun getAllDepartments(): List<String>
}
