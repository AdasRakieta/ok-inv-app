package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.DepartmentEntity

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY companyId ASC, name ASC")
    suspend fun getAll(): List<DepartmentEntity>

    @Query("SELECT * FROM departments WHERE companyId = :companyId ORDER BY name ASC")
    suspend fun getByCompany(companyId: Long): List<DepartmentEntity>

    @Query("SELECT name FROM departments WHERE companyId = :companyId ORDER BY name ASC")
    suspend fun getNamesByCompany(companyId: Long): List<String>

    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getById(id: Long): DepartmentEntity?

    @Query("SELECT * FROM departments WHERE companyId = :companyId AND name = :name LIMIT 1")
    suspend fun getByCompanyAndName(companyId: Long, name: String): DepartmentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(department: DepartmentEntity): Long

    @Update
    suspend fun update(department: DepartmentEntity)

    @Delete
    suspend fun delete(department: DepartmentEntity)

    @Query("DELETE FROM departments WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM departments WHERE companyId = :companyId")
    suspend fun deleteByCompany(companyId: Long)
}
