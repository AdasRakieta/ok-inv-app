package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.BoxDao
import com.example.inventoryapp.data.local.entities.BoxEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class BoxRepository(private val boxDao: BoxDao) {

    fun getAllBoxes(): Flow<List<BoxEntity>> = boxDao.getAllBoxes()

    fun getBoxesByLocation(locationId: Long): Flow<List<BoxEntity>> = boxDao.getBoxesByLocation(locationId)

    fun getBoxById(boxId: Long): Flow<BoxEntity?> = boxDao.getBoxById(boxId)

    suspend fun getBoxByIdOnce(boxId: Long): BoxEntity? = boxDao.getBoxByIdOnce(boxId)

    suspend fun getBoxByQrUid(qrUid: String): BoxEntity? = boxDao.getBoxByQrUid(qrUid)

    suspend fun insertBox(box: BoxEntity): Long {
        val withQr = if (!box.qrUid.isNullOrBlank()) box else box.copy(qrUid = UUID.randomUUID().toString())
        return boxDao.insertBox(withQr)
    }

    suspend fun updateBox(box: BoxEntity) = boxDao.updateBox(box)

    suspend fun deleteBox(box: BoxEntity) = boxDao.deleteBox(box)
}
