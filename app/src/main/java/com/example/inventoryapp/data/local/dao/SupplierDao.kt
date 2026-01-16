package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>
    
    @Query("SELECT * FROM suppliers WHERE id = :supplierId")
    fun getSupplierById(supplierId: Long): Flow<SupplierEntity?>
    
    @Query("SELECT * FROM suppliers WHERE name LIKE '%' || :query || '%'")
    fun searchSuppliers(query: String): Flow<List<SupplierEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long
    
    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)
    
    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)
}
