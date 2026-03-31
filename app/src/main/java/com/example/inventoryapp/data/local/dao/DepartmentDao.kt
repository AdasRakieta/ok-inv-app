package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.DepartmentEntity

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY name ASC")
    suspend fun getAll(): List<DepartmentEntity>

    @Query("SELECT name FROM departments ORDER BY name ASC")
    suspend fun getAllNames(): List<String>

    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getById(id: Long): DepartmentEntity?

    @Query("SELECT * FROM departments WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): DepartmentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(department: DepartmentEntity): Long

    @Update
    suspend fun update(department: DepartmentEntity)

    @Delete
    suspend fun delete(department: DepartmentEntity)

    @Query("DELETE FROM departments WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
