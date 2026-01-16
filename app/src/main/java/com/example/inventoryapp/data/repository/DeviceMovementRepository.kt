package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.DeviceMovementDao
import com.example.inventoryapp.data.local.entities.DeviceMovementEntity
import kotlinx.coroutines.flow.Flow

class DeviceMovementRepository(private val dao: DeviceMovementDao) {

    fun getMovementsForProduct(productId: Long): Flow<List<DeviceMovementEntity>> =
        dao.getMovementsByProduct(productId)

    fun getAllMovements(): Flow<List<DeviceMovementEntity>> = dao.getAllMovements()

    suspend fun getLastMovement(productId: Long): DeviceMovementEntity? = dao.getLastMovement(productId)

    suspend fun insertMovement(movement: DeviceMovementEntity): Long = dao.insertMovement(movement)

    // Convenience helpers
    suspend fun recordAssignToBox(productId: Long, boxId: Long, timestamp: Long = System.currentTimeMillis()) {
        // infer previous location (if any) to produce a transfer-style movement
        val last = getLastMovement(productId)
        val fromType = when {
            last == null -> "WAREHOUSE"
            !last.toContainerType.isNullOrEmpty() -> last.toContainerType
            !last.fromContainerType.isNullOrEmpty() -> last.fromContainerType
            else -> "WAREHOUSE"
        }
        val fromId = when {
            last == null -> null
            last.toContainerId != null -> last.toContainerId
            else -> last.fromContainerId
        }
        insertMovement(
            DeviceMovementEntity(
                productId = productId,
                action = "ASSIGN",
                fromContainerType = fromType,
                fromContainerId = fromId,
                toContainerType = "BOX",
                toContainerId = boxId,
                timestamp = timestamp
            )
        )
    }

    suspend fun recordUnassignFromBox(productId: Long, boxId: Long, timestamp: Long = System.currentTimeMillis()) {
        insertMovement(DeviceMovementEntity(productId = productId, action = "UNASSIGN", fromContainerType = "BOX", fromContainerId = boxId, timestamp = timestamp))
    }

    suspend fun recordAssignToPackage(productId: Long, packageId: Long, timestamp: Long = System.currentTimeMillis()) {
        // infer previous location to record transfer from previous container or warehouse
        val last = getLastMovement(productId)
        val fromType = when {
            last == null -> "WAREHOUSE"
            !last.toContainerType.isNullOrEmpty() -> last.toContainerType
            !last.fromContainerType.isNullOrEmpty() -> last.fromContainerType
            else -> "WAREHOUSE"
        }
        val fromId = when {
            last == null -> null
            last.toContainerId != null -> last.toContainerId
            else -> last.fromContainerId
        }
        insertMovement(
            DeviceMovementEntity(
                productId = productId,
                action = "ASSIGN",
                fromContainerType = fromType,
                fromContainerId = fromId,
                toContainerType = "PACKAGE",
                toContainerId = packageId,
                timestamp = timestamp
            )
        )
    }

    suspend fun recordUnassignFromPackage(productId: Long, packageId: Long, timestamp: Long = System.currentTimeMillis()) {
        insertMovement(DeviceMovementEntity(productId = productId, action = "UNASSIGN", fromContainerType = "PACKAGE", fromContainerId = packageId, timestamp = timestamp))
    }

    suspend fun recordPackageStatusChange(productId: Long, packageId: Long, status: String, timestamp: Long = System.currentTimeMillis()) {
        insertMovement(DeviceMovementEntity(productId = productId, action = "PACKAGE_STATUS_CHANGE", toContainerType = "PACKAGE", toContainerId = packageId, packageStatus = status, timestamp = timestamp))
    }
}
