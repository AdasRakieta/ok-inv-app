package com.example.inventoryapp.ui.assign

import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.PointType
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.data.repository.CategoryRepository
import com.example.inventoryapp.data.repository.CompanyRepository
import com.example.inventoryapp.data.repository.ContractorPointRepository
import com.example.inventoryapp.data.repository.EmployeeRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.domain.validators.AssignmentValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AssignmentFlowIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var productRepository: ProductRepository
    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var contractorPointRepository: ContractorPointRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var companyRepository: CompanyRepository
    private lateinit var assignmentValidator: AssignmentValidator

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        database.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA foreign_keys = ON")).close()

        productRepository = ProductRepository(database.productDao())
        employeeRepository = EmployeeRepository(database.employeeDao())
        contractorPointRepository = ContractorPointRepository(database.contractorPointDao())
        categoryRepository = CategoryRepository(database.categoryDao())
        companyRepository = CompanyRepository(database.companyDao())
        assignmentValidator = AssignmentValidator()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `assign contractor equipment to employee should pass and persist`() = runBlocking {
        val categoryId = categoryRepository.insertCategory(
            CategoryEntity(
                name = "Skaner test employee",
                parentId = CategoryEntity.CONTRACTOR_CATEGORY_PARENT_ID
            )
        )
        val employeeId = employeeRepository.insertEmployee(
            EmployeeEntity(
                firstName = "Anna",
                lastName = "Nowak",
                email = "anna.integration@test.pl",
                phone = null,
                department = "OPS",
                position = "Courier",
                notes = null
            )
        )
        val productId = productRepository.insertProduct(
            ProductEntity(
                name = "Skaner M1",
                serialNumber = "INT-EMP-CON-001",
                categoryId = categoryId,
                status = ProductStatus.UNASSIGNED
            )
        )

        val productBefore = database.productDao().getProductByIdOnce(productId)!!
        val employee = employeeRepository.getEmployeeById(employeeId)!!
        val category = categoryRepository.getCategoryByName("Skaner test employee")

        val validation = assignmentValidator.canAssignToEmployee(productBefore, employee, category)
        assertTrue(validation is AssignmentValidator.ValidationResult.Success)

        productRepository.assignToEmployee(productId, employeeId)

        val persisted = database.productDao().getProductByIdOnce(productId)
        assertNotNull(persisted)
        assertEquals(employeeId, persisted?.assignedToEmployeeId)
        assertNull(persisted?.assignedToContractorPointId)
        assertEquals(ProductStatus.ASSIGNED, persisted?.status)
    }

    @Test
    fun `assign office equipment to employee should pass and persist`() = runBlocking {
        val categoryId = categoryRepository.insertCategory(
            CategoryEntity(
                name = "Laptop test employee",
                parentId = CategoryEntity.OFFICE_CATEGORY_PARENT_ID
            )
        )
        val employeeId = employeeRepository.insertEmployee(
            EmployeeEntity(
                firstName = "Jan",
                lastName = "Kowalski",
                email = "jan.integration@test.pl",
                phone = null,
                department = "IT",
                position = "Developer",
                notes = null
            )
        )
        val productId = productRepository.insertProduct(
            ProductEntity(
                name = "Laptop X",
                serialNumber = "INT-EMP-OFF-001",
                categoryId = categoryId,
                status = ProductStatus.UNASSIGNED
            )
        )

        val productBefore = database.productDao().getProductByIdOnce(productId)!!
        val employee = employeeRepository.getEmployeeById(employeeId)!!
        val category = categoryRepository.getCategoryByName("Laptop test employee")

        val validation = assignmentValidator.canAssignToEmployee(productBefore, employee, category)
        assertTrue(validation is AssignmentValidator.ValidationResult.Success)

        productRepository.assignToEmployee(productId, employeeId)

        val persisted = database.productDao().getProductByIdOnce(productId)
        assertNotNull(persisted)
        assertEquals(employeeId, persisted?.assignedToEmployeeId)
        assertNull(persisted?.assignedToContractorPointId)
        assertEquals(ProductStatus.ASSIGNED, persisted?.status)
    }

    @Test
    fun `assign contractor equipment to contractor point should pass and be queryable`() = runBlocking {
        val companyId = companyRepository.insertCompany(
            CompanyEntity(
                name = "Contractor Co",
                taxId = "1234567890",
                address = "Street 1",
                city = "Warsaw",
                postalCode = "00-001"
            )
        )
        val contractorPointId = contractorPointRepository.insertContractorPoint(
            ContractorPointEntity(
                code = "CP-INT-001",
                name = "CP Integration",
                pointType = PointType.CP,
                companyId = companyId
            )
        )
        val categoryId = categoryRepository.insertCategory(
            CategoryEntity(
                name = "Skaner test contractor point",
                parentId = CategoryEntity.CONTRACTOR_CATEGORY_PARENT_ID
            )
        )
        val productId = productRepository.insertProduct(
            ProductEntity(
                name = "Skaner CP",
                serialNumber = "INT-CP-CON-001",
                categoryId = categoryId,
                status = ProductStatus.UNASSIGNED
            )
        )

        val productBefore = database.productDao().getProductByIdOnce(productId)!!
        val category = categoryRepository.getCategoryByName("Skaner test contractor point")

        val validation = assignmentValidator.canAssignToContractorPoint(productBefore, category)
        assertTrue(validation is AssignmentValidator.ValidationResult.Success)

        productRepository.assignToContractorPoint(
            productId = productId,
            contractorPointId = contractorPointId,
            contractorPointName = "CP Integration"
        )

        val persisted = database.productDao().getProductByIdOnce(productId)
        assertNotNull(persisted)
        assertEquals(contractorPointId, persisted?.assignedToContractorPointId)
        assertNull(persisted?.assignedToEmployeeId)
        assertEquals(ProductStatus.ASSIGNED, persisted?.status)
        assertTrue(persisted?.movementHistory?.contains("Punkt kontrahenta: CP Integration") == true)

        val assignedToPoint = productRepository.getProductsAssignedToContractorPoint(contractorPointId).first()
        assertEquals(1, assignedToPoint.size)
        assertEquals(productId, assignedToPoint.first().id)
    }

    @Test
    fun `assign office equipment to contractor point should fail validation and keep unassigned state`() = runBlocking {
        val companyId = companyRepository.insertCompany(
            CompanyEntity(
                name = "Office Co",
                taxId = "1234567891",
                address = "Street 2",
                city = "Krakow",
                postalCode = "30-001"
            )
        )
        contractorPointRepository.insertContractorPoint(
            ContractorPointEntity(
                code = "CP-INT-002",
                name = "CP Office",
                pointType = PointType.CP,
                companyId = companyId
            )
        )
        val categoryId = categoryRepository.insertCategory(
            CategoryEntity(
                name = "Laptop test contractor point",
                parentId = CategoryEntity.OFFICE_CATEGORY_PARENT_ID
            )
        )
        val productId = productRepository.insertProduct(
            ProductEntity(
                name = "Laptop CP",
                serialNumber = "INT-CP-OFF-001",
                categoryId = categoryId,
                status = ProductStatus.UNASSIGNED
            )
        )

        val productBefore = database.productDao().getProductByIdOnce(productId)!!
        val category = categoryRepository.getCategoryByName("Laptop test contractor point")

        val validation = assignmentValidator.canAssignToContractorPoint(productBefore, category)
        assertTrue(validation is AssignmentValidator.ValidationResult.Error)
        assertEquals(
            "Punkty CP/CC/DC mogą otrzymać tylko sprzęt kontrahencki.",
            (validation as AssignmentValidator.ValidationResult.Error).message
        )

        val persisted = database.productDao().getProductByIdOnce(productId)
        assertNotNull(persisted)
        assertNull(persisted?.assignedToEmployeeId)
        assertNull(persisted?.assignedToContractorPointId)
        assertEquals(ProductStatus.UNASSIGNED, persisted?.status)
    }
}
