package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.CompanyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllFlow(): Flow<List<CompanyEntity>>
    
    @Query("SELECT * FROM companies ORDER BY name ASC")
    suspend fun getAll(): List<CompanyEntity>

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getById(id: Long): CompanyEntity?

    @Query("SELECT * FROM companies WHERE nip = :nip LIMIT 1")
    suspend fun getByNip(nip: String): CompanyEntity?
    
    @Query("""
        SELECT * FROM companies 
        WHERE :searchQuery IS NULL OR :searchQuery = '' OR 
              name LIKE '%' || :searchQuery || '%' OR 
              city LIKE '%' || :searchQuery || '%' OR
              address LIKE '%' || :searchQuery || '%' OR
              contactPerson LIKE '%' || :searchQuery || '%'
        ORDER BY name ASC
    """)
    suspend fun search(searchQuery: String?): List<CompanyEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(company: CompanyEntity): Long

    @Update
    suspend fun update(company: CompanyEntity)

    @Delete
    suspend fun delete(company: CompanyEntity)
    
    @Query("DELETE FROM companies WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
