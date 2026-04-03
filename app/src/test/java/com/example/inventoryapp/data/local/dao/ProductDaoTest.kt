package com.example.inventoryapp.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProductDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var productDao: ProductDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        productDao = database.productDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `assignToContractorPoint sets contractor assignment and clears employee assignment`() = runBlocking {
        val productId = insertProduct(
            ProductEntity(
                name = "Scanner 1",
                serialNumber = "SN-CON-001",
                status = ProductStatus.UNASSIGNED,
                assignedToEmployeeId = 10L
            )
        )

        val now = System.currentTimeMillis()
        productDao.assignToContractorPoint(
            productId = productId,
            contractorPointId = 22L,
            assignmentDate = now,
            status = ProductStatus.ASSIGNED,
            updatedAt = now
        )

        val updated = productDao.getProductByIdOnce(productId)!!
        assertEquals(22L, updated.assignedToContractorPointId)
        assertNull(updated.assignedToEmployeeId)
        assertEquals(ProductStatus.ASSIGNED, updated.status)
    }

    @Test
    fun `getProductsAssignedToContractorPoint and count return only matching rows`() = runBlocking {
        val now = System.currentTimeMillis()

        val assignedTo100 = insertProduct(
            ProductEntity(
                name = "Device A",
                serialNumber = "SN-CON-100",
                status = ProductStatus.ASSIGNED,
                assignedToContractorPointId = 100L,
                assignmentDate = now
            )
        )
        insertProduct(
            ProductEntity(
                name = "Device B",
                serialNumber = "SN-CON-101",
                status = ProductStatus.ASSIGNED,
                assignedToContractorPointId = 101L,
                assignmentDate = now
            )
        )

        val list = productDao.getProductsAssignedToContractorPoint(100L).first()
        val count = productDao.getAssignedProductsCountForContractorPoint(100L)

        assertEquals(1, list.size)
        assertEquals(assignedTo100, list.first().id)
        assertEquals(1, count)
    }

    @Test
    fun `assignToEmployee clears contractor assignment to keep compatibility`() = runBlocking {
        val productId = insertProduct(
            ProductEntity(
                name = "Laptop 1",
                serialNumber = "SN-EMP-001",
                status = ProductStatus.ASSIGNED,
                assignedToContractorPointId = 55L
            )
        )

        val now = System.currentTimeMillis()
        productDao.assignToEmployee(
            productId = productId,
            employeeId = 33L,
            assignmentDate = now,
            status = ProductStatus.ASSIGNED,
            updatedAt = now
        )

        val updated = productDao.getProductByIdOnce(productId)!!
        assertEquals(33L, updated.assignedToEmployeeId)
        assertNull(updated.assignedToContractorPointId)
        assertEquals(ProductStatus.ASSIGNED, updated.status)
    }

    @Test
    fun `insert get by serial update and delete support core CRUD`() = runBlocking {
        val id = insertProduct(
            ProductEntity(
                name = "Monitor 1",
                serialNumber = "SN-CRUD-001",
                status = ProductStatus.IN_STOCK
            )
        )

        val byId = productDao.getProductByIdOnce(id)
        val bySerial = productDao.getProductBySerialNumber("SN-CRUD-001")
        assertNotNull(byId)
        assertNotNull(bySerial)
        assertEquals(id, bySerial?.id)

        productDao.updateProduct(byId!!.copy(name = "Monitor 1 Updated"))
        assertEquals("Monitor 1 Updated", productDao.getProductByIdOnce(id)?.name)

        productDao.deleteProductById(id)
        assertNull(productDao.getProductByIdOnce(id))
    }

    @Test
    fun `search and status filters return expected rows`() = runBlocking {
        insertProduct(
            ProductEntity(
                name = "Alpha Laptop",
                serialNumber = "SN-SRCH-001",
                status = ProductStatus.ASSIGNED
            )
        )
        insertProduct(
            ProductEntity(
                name = "Beta Phone",
                serialNumber = "SN-SRCH-002",
                status = ProductStatus.IN_STOCK
            )
        )
        insertProduct(
            ProductEntity(
                name = "Gamma Device",
                serialNumber = "INV-GAMMA",
                status = ProductStatus.ASSIGNED
            )
        )

        val byText = productDao.searchProducts("Alpha").first()
        val bySerial = productDao.searchProducts("INV-GAM").first()
        val assigned = productDao.getProductsByStatus(ProductStatus.ASSIGNED).first()
        val assignedCount = productDao.getProductCountByStatus(ProductStatus.ASSIGNED).first()
        val totalCount = productDao.getProductCount().first()

        assertEquals(1, byText.size)
        assertEquals("SN-SRCH-001", byText.first().serialNumber)
        assertEquals(1, bySerial.size)
        assertEquals("Gamma Device", bySerial.first().name)
        assertEquals(2, assigned.size)
        assertEquals(2, assignedCount)
        assertEquals(3, totalCount)
    }

    @Test
    fun `contractor and employee assignment updates remain mutually exclusive`() = runBlocking {
        val productId = insertProduct(
            ProductEntity(
                name = "Tablet 1",
                serialNumber = "SN-XASSIGN-001",
                status = ProductStatus.UNASSIGNED
            )
        )
        val now = System.currentTimeMillis()

        productDao.assignToEmployee(
            productId = productId,
            employeeId = 700L,
            assignmentDate = now,
            status = ProductStatus.ASSIGNED,
            updatedAt = now
        )
        val afterEmployeeAssign = productDao.getProductByIdOnce(productId)!!
        assertEquals(700L, afterEmployeeAssign.assignedToEmployeeId)
        assertNull(afterEmployeeAssign.assignedToContractorPointId)

        productDao.assignToContractorPoint(
            productId = productId,
            contractorPointId = 900L,
            assignmentDate = now + 1,
            status = ProductStatus.ASSIGNED,
            updatedAt = now + 1
        )
        val afterContractorAssign = productDao.getProductByIdOnce(productId)!!
        assertNull(afterContractorAssign.assignedToEmployeeId)
        assertEquals(900L, afterContractorAssign.assignedToContractorPointId)

        productDao.unassignFromContractorPoint(
            productId = productId,
            status = ProductStatus.UNASSIGNED,
            updatedAt = now + 2
        )
        val unassigned = productDao.getProductByIdOnce(productId)!!
        assertNull(unassigned.assignedToEmployeeId)
        assertNull(unassigned.assignedToContractorPointId)
        assertNull(unassigned.assignmentDate)
        assertEquals(ProductStatus.UNASSIGNED, unassigned.status)
    }

    private suspend fun insertProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }
}

