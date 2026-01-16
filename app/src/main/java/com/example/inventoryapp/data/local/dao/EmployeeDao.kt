package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.EmployeeEntity

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    suspend fun getAll(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: EmployeeEntity): Long

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Delete
    suspend fun delete(employee: EmployeeEntity)
}
