package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.BoxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoxDao {
    @Query("SELECT * FROM boxes ORDER BY createdAt DESC")
    fun getAllBoxes(): Flow<List<BoxEntity>>

    @Query("SELECT * FROM boxes WHERE id = :boxId LIMIT 1")
    fun getBoxById(boxId: Long): Flow<BoxEntity?>

    @Query("SELECT * FROM boxes WHERE id = :boxId LIMIT 1")
    suspend fun getBoxByIdOnce(boxId: Long): BoxEntity?

    @Query("SELECT * FROM boxes WHERE qrUid = :qrUid LIMIT 1")
    suspend fun getBoxByQrUid(qrUid: String): BoxEntity?

    @Query("SELECT * FROM boxes WHERE warehouseLocationId = :locationId ORDER BY createdAt DESC")
    fun getBoxesByLocation(locationId: Long): Flow<List<BoxEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: BoxEntity): Long

    @Update
    suspend fun updateBox(box: BoxEntity)

    @Delete
    suspend fun deleteBox(box: BoxEntity)
}
