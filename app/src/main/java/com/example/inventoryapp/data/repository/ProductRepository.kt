package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    
    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()
    
    fun getProductById(productId: Long): Flow<ProductEntity?> = productDao.getProductById(productId)
    
    suspend fun getProductBySerialNumber(serialNumber: String): ProductEntity? = 
        productDao.getProductBySerialNumber(serialNumber)
    
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> = 
        productDao.getProductsByCategory(categoryId)
    
    fun getProductsByLocation(locationId: Long): Flow<List<ProductEntity>> = 
        productDao.getProductsByLocation(locationId)
    
    fun getProductsAssignedToEmployee(employeeId: Long): Flow<List<ProductEntity>> = 
        productDao.getProductsAssignedToEmployee(employeeId)
    
    fun getProductsByStatus(status: ProductStatus): Flow<List<ProductEntity>> = 
        productDao.getProductsByStatus(status)
    
    fun searchProducts(query: String): Flow<List<ProductEntity>> = 
        productDao.searchProducts(query)
    
    fun getProductCount(): Flow<Int> = productDao.getProductCount()
    
    fun getProductCountByStatus(status: ProductStatus): Flow<Int> = 
        productDao.getProductCountByStatus(status)
    
    suspend fun insertProduct(product: ProductEntity): Long = 
        productDao.insertProduct(product)
    
    suspend fun insertProducts(products: List<ProductEntity>): List<Long> = 
        productDao.insertProducts(products)
    
    suspend fun updateProduct(product: ProductEntity) = 
        productDao.updateProduct(product)
    
    suspend fun deleteProduct(product: ProductEntity) = 
        productDao.deleteProduct(product)
    
    suspend fun deleteProductById(productId: Long) = 
        productDao.deleteProductById(productId)
    
    suspend fun assignToEmployee(productId: Long, employeeId: Long) {
        val now = System.currentTimeMillis()
        productDao.assignToEmployee(productId, employeeId, now, ProductStatus.ASSIGNED, now)
    }
    
    suspend fun unassignFromEmployee(productId: Long) {
        val now = System.currentTimeMillis()
        productDao.unassignFromEmployee(productId, ProductStatus.IN_STOCK, now)
    }
}
