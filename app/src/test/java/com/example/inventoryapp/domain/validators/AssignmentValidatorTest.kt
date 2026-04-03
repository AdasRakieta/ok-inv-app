package com.example.inventoryapp.domain.validators

import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssignmentValidatorTest {

    private lateinit var validator: AssignmentValidator

    @Before
    fun setup() {
        validator = AssignmentValidator()
    }

    @Test
    fun `canAssignToEmployee allows office equipment`() {
        val category = CategoryEntity(name = "Laptop", parentId = 1L)
        val product = ProductEntity(name = "Laptop A", serialNumber = "LAP001")
        val employee = EmployeeEntity(firstName = "Jan", lastName = "Kowalski", email = null, phone = null, department = null, position = null, notes = null)

        val result = validator.canAssignToEmployee(product, employee, category)

        assertTrue(result is AssignmentValidator.ValidationResult.Success)
    }

    @Test
    fun `canAssignToEmployee allows contractor equipment`() {
        val category = CategoryEntity(
            name = "Skaner",
            parentId = CategoryEntity.CONTRACTOR_CATEGORY_PARENT_ID
        )
        val product = ProductEntity(name = "Skaner A", serialNumber = "SKN001")
        val employee = EmployeeEntity(firstName = "Anna", lastName = "Nowak", email = null, phone = null, department = null, position = null, notes = null)

        val result = validator.canAssignToEmployee(product, employee, category)

        assertTrue(result is AssignmentValidator.ValidationResult.Success)
    }

    @Test
    fun `canAssignToEmployee allows assignment when category missing`() {
        val product = ProductEntity(name = "Laptop A", serialNumber = "LAP001")
        val employee = EmployeeEntity(firstName = "Jan", lastName = "Kowalski", email = null, phone = null, department = null, position = null, notes = null)

        val result = validator.canAssignToEmployee(product, employee, null)

        assertTrue(result is AssignmentValidator.ValidationResult.Success)
    }

    @Test
    fun `canAssignToContractorPoint rejects office equipment`() {
        val category = CategoryEntity(
            name = "Laptop",
            parentId = CategoryEntity.OFFICE_CATEGORY_PARENT_ID
        )
        val product = ProductEntity(name = "Laptop A", serialNumber = "LAP001")

        val result = validator.canAssignToContractorPoint(product, category)

        assertTrue(result is AssignmentValidator.ValidationResult.Error)
        assertEquals(
            "Punkty CP/CC/DC mogą otrzymać tylko sprzęt kontrahencki.",
            (result as AssignmentValidator.ValidationResult.Error).message
        )
    }

    @Test
    fun `canAssignToContractorPoint allows contractor equipment`() {
        val category = CategoryEntity(
            name = "Skaner",
            parentId = CategoryEntity.CONTRACTOR_CATEGORY_PARENT_ID
        )
        val product = ProductEntity(name = "Skaner A", serialNumber = "SKN001")

        val result = validator.canAssignToContractorPoint(product, category)

        assertTrue(result is AssignmentValidator.ValidationResult.Success)
    }

    @Test
    fun `canAssignToContractorPoint rejects when category missing`() {
        val product = ProductEntity(name = "Unknown", serialNumber = "UNK001")

        val result = validator.canAssignToContractorPoint(product, null)

        assertTrue(result is AssignmentValidator.ValidationResult.Error)
        assertEquals(
            "Nie można zweryfikować kategorii sprzętu dla punktu CP/CC/DC.",
            (result as AssignmentValidator.ValidationResult.Error).message
        )
    }

    @Test
    fun `canAssignToContractorPoint rejects unknown destination category as office`() {
        val category = CategoryEntity(name = "Inna", parentId = 999L)
        val product = ProductEntity(name = "Unknown", serialNumber = "UNK002")

        val result = validator.canAssignToContractorPoint(product, category)

        assertTrue(result is AssignmentValidator.ValidationResult.Error)
        assertEquals(
            "Punkty CP/CC/DC mogą otrzymać tylko sprzęt kontrahencki.",
            (result as AssignmentValidator.ValidationResult.Error).message
        )
    }
}
