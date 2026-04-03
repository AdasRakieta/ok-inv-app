package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProductRepositoryTest {

    private lateinit var productDao: ProductDao
    private lateinit var repository: ProductRepository

    @Before
    fun setup() {
        productDao = mock()
        repository = ProductRepository(productDao)
    }

    @Test
    fun `getProductsAssignedToEmployee delegates to dao`() {
        val flow = flowOf(emptyList<ProductEntity>())
        whenever(productDao.getProductsAssignedToEmployee(15L)).thenReturn(flow)

        val result = repository.getProductsAssignedToEmployee(15L)

        assertEquals(flow, result)
        verify(productDao).getProductsAssignedToEmployee(15L)
    }

    @Test
    fun `getAssignedProductsCount delegates to dao`() = runBlocking {
        whenever(productDao.getAssignedProductsCount(22L)).thenReturn(3)

        val result = repository.getAssignedProductsCount(22L)

        assertEquals(3, result)
        verify(productDao).getAssignedProductsCount(22L)
    }

    @Test
    fun `assignToEmployee updates assignment fields and clears contractor assignment`() = runBlocking {
        val product = ProductEntity(
            id = 9,
            name = "Laptop",
            serialNumber = "SN-9",
            assignedToEmployeeId = null,
            assignedToContractorPointId = 77L,
            assignmentDate = null,
            status = ProductStatus.IN_STOCK,
            shelf = "A1",
            bin = "B1",
            movementHistory = null,
            updatedAt = 1000L
        )
        whenever(productDao.getProductByIdOnce(9L)).thenReturn(product)
        doNothing().whenever(productDao).updateProduct(any())

        repository.assignToEmployee(9L, 42L)

        val captor = argumentCaptor<ProductEntity>()
        verify(productDao).updateProduct(captor.capture())
        val updated = captor.firstValue
        assertEquals(42L, updated.assignedToEmployeeId)
        assertNull(updated.assignedToContractorPointId)
        assertEquals(ProductStatus.ASSIGNED, updated.status)
        assertNull(updated.shelf)
        assertNull(updated.bin)
        assertTrue(updated.assignmentDate != null)
        assertTrue(updated.updatedAt >= product.updatedAt)
        assertTrue(updated.movementHistory?.contains("Pracownik: ID 42") == true)
    }

    @Test
    fun `assignToEmployee does nothing when product not found`() = runBlocking {
        whenever(productDao.getProductByIdOnce(404L)).thenReturn(null)

        repository.assignToEmployee(404L, 1L)

        verify(productDao).getProductByIdOnce(404L)
        verify(productDao, never()).updateProduct(any())
    }

    @Test
    fun `unassignFromEmployee clears assignment and sets unassigned status`() = runBlocking {
        val product = ProductEntity(
            id = 8,
            name = "Monitor",
            serialNumber = "SN-8",
            assignedToEmployeeId = 7L,
            assignmentDate = 1500L,
            status = ProductStatus.ASSIGNED,
            movementHistory = null,
            updatedAt = 1000L
        )
        whenever(productDao.getProductByIdOnce(8L)).thenReturn(product)
        doNothing().whenever(productDao).updateProduct(any())

        repository.unassignFromEmployee(8L)

        val captor = argumentCaptor<ProductEntity>()
        verify(productDao).updateProduct(captor.capture())
        val updated = captor.firstValue
        assertNull(updated.assignedToEmployeeId)
        assertNull(updated.assignmentDate)
        assertEquals(ProductStatus.UNASSIGNED, updated.status)
        assertTrue(updated.updatedAt >= product.updatedAt)
        assertTrue(updated.movementHistory?.contains("Brak przypisania") == true)
    }
}
