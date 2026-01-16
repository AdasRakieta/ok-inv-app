package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.ContractorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractorDao {

    @Query("SELECT * FROM contractors ORDER BY name ASC")
    fun getAllContractors(): Flow<List<ContractorEntity>>

    @Query("SELECT * FROM contractors WHERE id = :contractorId")
    fun getContractorById(contractorId: Long): Flow<ContractorEntity?>

    @Query("SELECT * FROM contractors WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getContractorByName(name: String): ContractorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContractor(contractor: ContractorEntity): Long

    @Update
    suspend fun updateContractor(contractor: ContractorEntity)

    @Delete
    suspend fun deleteContractor(contractor: ContractorEntity)

    @Query("SELECT COUNT(*) FROM contractors")
    suspend fun getContractorCount(): Int
}