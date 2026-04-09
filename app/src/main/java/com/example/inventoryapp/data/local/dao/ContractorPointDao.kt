package com.example.inventoryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.PointType
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractorPointDao {
    @Query("SELECT * FROM contractor_points ORDER BY name ASC")
    fun getAllFlow(): Flow<List<ContractorPointEntity>>

    @Query("SELECT * FROM contractor_points ORDER BY name ASC")
    suspend fun getAll(): List<ContractorPointEntity>

    @Query("SELECT * FROM contractor_points WHERE id = :id")
    suspend fun getById(id: Long): ContractorPointEntity?


    @Query("SELECT * FROM contractor_points WHERE pointType = :pointType ORDER BY name ASC")
    suspend fun getByPointType(pointType: PointType): List<ContractorPointEntity>

    @Query("SELECT * FROM contractor_points WHERE companyId = :companyId ORDER BY name ASC")
    suspend fun getByCompany(companyId: Long): List<ContractorPointEntity>

    @Query(
        """
        SELECT * FROM contractor_points
        WHERE :searchQuery IS NULL OR :searchQuery = '' OR
              name LIKE '%' || :searchQuery || '%' OR
              city LIKE '%' || :searchQuery || '%' OR
              address LIKE '%' || :searchQuery || '%' OR
              phone LIKE '%' || :searchQuery || '%' OR
              marketNumber LIKE '%' || :searchQuery || '%'
        ORDER BY name ASC
    """
    )
    suspend fun search(searchQuery: String?): List<ContractorPointEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(contractorPoint: ContractorPointEntity): Long

    @Update
    suspend fun update(contractorPoint: ContractorPointEntity)

    @Delete
    suspend fun delete(contractorPoint: ContractorPointEntity)

    @Query("DELETE FROM contractor_points WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
