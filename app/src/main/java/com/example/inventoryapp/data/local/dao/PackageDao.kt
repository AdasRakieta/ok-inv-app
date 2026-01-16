package com.example.inventoryapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.local.entities.PackageProductCrossRef
import com.example.inventoryapp.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages WHERE archived = 0 ORDER BY createdAt DESC")
    fun getAllPackages(): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE archived = 1 ORDER BY createdAt DESC")
    fun getArchivedPackages(): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE id = :packageId")
    fun getPackageById(packageId: Long): Flow<PackageEntity?>

    @Query("SELECT * FROM packages WHERE LOWER(name) = LOWER(:name) AND status != 'RETURNED' LIMIT 1")
    suspend fun getPackageByName(name: String): PackageEntity?

    @Query("SELECT * FROM packages WHERE packageCode = :packageCode LIMIT 1")
    suspend fun getPackageByCode(packageCode: String): PackageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(packageEntity: PackageEntity): Long

    @Update
    suspend fun updatePackage(packageEntity: PackageEntity)

    @Delete
    suspend fun deletePackage(packageEntity: PackageEntity)
    
    @Query("DELETE FROM packages WHERE id = :packageId")
    suspend fun deletePackageById(packageId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProductToPackage(crossRef: PackageProductCrossRef)

    @Delete
    suspend fun removeProductFromPackage(crossRef: PackageProductCrossRef)

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM products INNER JOIN package_product_cross_ref ON products.id = package_product_cross_ref.productId WHERE package_product_cross_ref.packageId = :packageId")
    fun getProductsInPackage(packageId: Long): Flow<List<ProductEntity>>

    @Query("DELETE FROM package_product_cross_ref WHERE packageId = :packageId")
    suspend fun removeAllProductsFromPackage(packageId: Long)

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM packages INNER JOIN package_product_cross_ref ON packages.id = package_product_cross_ref.packageId WHERE package_product_cross_ref.productId = :productId ORDER BY packages.archived ASC LIMIT 1")
    fun getPackageForProduct(productId: Long): Flow<PackageEntity?>
    
    @Query("SELECT * FROM package_product_cross_ref WHERE productId = :productId")
    suspend fun getPackageCrossRefsForProduct(productId: Long): List<com.example.inventoryapp.data.local.entities.PackageProductCrossRef>
    
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM package_product_cross_ref 
            WHERE packageId = :packageId AND productId = :productId
        )
    """)
    suspend fun isProductInPackage(packageId: Long, productId: Long): Boolean
    
    @Query("""
        SELECT packages.*, COUNT(package_product_cross_ref.productId) as productCount
        FROM packages
        LEFT JOIN package_product_cross_ref ON packages.id = package_product_cross_ref.packageId
        WHERE packages.archived = 0
        GROUP BY packages.id
        ORDER BY packages.createdAt DESC
    """)
    fun getAllPackagesWithCount(): Flow<List<PackageWithCount>>
    
    @Query("""
        SELECT packages.*, COUNT(package_product_cross_ref.productId) as productCount
        FROM packages
        LEFT JOIN package_product_cross_ref ON packages.id = package_product_cross_ref.packageId
        WHERE packages.archived = 1
        GROUP BY packages.id
        ORDER BY packages.createdAt DESC
    """)
    fun getArchivedPackagesWithCount(): Flow<List<PackageWithCount>>
}

data class PackageWithCount(
    @Embedded val packageEntity: PackageEntity,
    val productCount: Int
)
