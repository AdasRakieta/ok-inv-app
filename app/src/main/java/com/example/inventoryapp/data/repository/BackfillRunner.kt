package com.example.inventoryapp.data.repository

import android.util.Log
import com.example.inventoryapp.data.local.dao.BoxDao
import com.example.inventoryapp.data.local.dao.PackageDao
import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.entities.DeviceMovementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

/**
 * BackfillRunner: creates historical DeviceMovementEntity rows inferred from existing
 * box/package cross-ref tables and package timestamps. The routine is idempotent per-product:
 * it will skip products which already have any DeviceMovement rows.
 */
class BackfillRunner(
    private val productDao: ProductDao,
    private val packageDao: PackageDao,
    private val boxDao: BoxDao,
    private val deviceMovementRepository: DeviceMovementRepository
){
    suspend fun runFullBackfill(progress: (String) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            progress("Starting backfill")

            val products = productDao.getAllProducts().first()

            var processed = 0
            for (product in products) {
                processed++
                progress("Product ${product.id} (${product.serialNumber ?: product.name}): checking")

                // If any movement exists for product, skip to avoid duplicating history
                val last = deviceMovementRepository.getLastMovement(product.id)
                if (last != null) {
                    progress("Product ${product.id}: already has history, skipping")
                    continue
                }

                val inferred = mutableListOf<DeviceMovementEntity>()

                // Box assignments have timestamps in cross-ref
                try {
                    val boxCrossRefs = boxDao.getBoxCrossRefsForProduct(product.id)
                    for (br in boxCrossRefs) {
                        inferred.add(
                            DeviceMovementEntity(
                                productId = product.id,
                                action = "ASSIGN",
                                fromContainerType = "WAREHOUSE",
                                toContainerType = "BOX",
                                toContainerId = br.boxId,
                                timestamp = br.addedAt
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("BackfillRunner", "Failed to read box cross-refs for product ${product.id}", e)
                }

                // Package assignments: no cross-ref timestamp -> use package.createdAt (or product.createdAt if later)
                try {
                    val pkgCrossRefs = packageDao.getPackageCrossRefsForProduct(product.id)
                    for (pr in pkgCrossRefs) {
                        val pkg = packageDao.getPackageById(pr.packageId).first()
                        val ts = pkg?.createdAt ?: product.createdAt
                        inferred.add(
                            DeviceMovementEntity(
                                productId = product.id,
                                action = "ASSIGN",
                                fromContainerType = "WAREHOUSE",
                                toContainerType = "PACKAGE",
                                toContainerId = pr.packageId,
                                timestamp = ts
                            )
                        )

                        // Package status timestamps -> PACKAGE_STATUS_CHANGE events
                        if (pkg != null) {
                            pkg.shippedAt?.let { shipped ->
                                inferred.add(
                                    DeviceMovementEntity(
                                        productId = product.id,
                                        action = "PACKAGE_STATUS_CHANGE",
                                        toContainerType = "PACKAGE",
                                        toContainerId = pkg.id,
                                        packageStatus = "SHIPPED",
                                        timestamp = shipped
                                    )
                                )
                            }
                            pkg.deliveredAt?.let { delivered ->
                                inferred.add(
                                    DeviceMovementEntity(
                                        productId = product.id,
                                        action = "PACKAGE_STATUS_CHANGE",
                                        toContainerType = "PACKAGE",
                                        toContainerId = pkg.id,
                                        packageStatus = "DELIVERED",
                                        timestamp = delivered
                                    )
                                )
                            }
                            pkg.returnedAt?.let { returned ->
                                inferred.add(
                                    DeviceMovementEntity(
                                        productId = product.id,
                                        action = "PACKAGE_STATUS_CHANGE",
                                        toContainerType = "PACKAGE",
                                        toContainerId = pkg.id,
                                        packageStatus = "RETURNED",
                                        timestamp = returned
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("BackfillRunner", "Failed to read package cross-refs for product ${product.id}", e)
                }

                // Sort inferred events by timestamp and insert
                inferred.sortBy { it.timestamp }
                for (m in inferred) {
                    try {
                        deviceMovementRepository.insertMovement(m)
                    } catch (e: Exception) {
                        Log.w("BackfillRunner", "Failed to insert movement for product ${product.id}", e)
                    }
                }

                progress("Product ${product.id}: backfilled ${inferred.size} movements")
            }

            progress("Backfill complete: processed $processed products")
        }
    }
}
