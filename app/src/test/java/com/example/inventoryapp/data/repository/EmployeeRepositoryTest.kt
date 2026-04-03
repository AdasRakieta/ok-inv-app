package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.EmployeeDao
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EmployeeRepositoryTest {

    private lateinit var employeeDao: EmployeeDao
    private lateinit var repository: EmployeeRepository

    @Before
    fun setup() {
        employeeDao = mock()
        repository = EmployeeRepository(employeeDao)
    }

    @Test
    fun `getAllEmployeesFlow delegates to dao`() {
        val employees = listOf(
            EmployeeEntity(id = 1, firstName = "Jan", lastName = "Kowalski", companyId = 10, email = "jan@acme.com", phone = null, department = "IT", position = null, notes = null)
        )
        val flow = flowOf(employees)
        whenever(employeeDao.getAllFlow()).thenReturn(flow)

        val result = repository.getAllEmployeesFlow()

        assertEquals(flow, result)
        verify(employeeDao).getAllFlow()
    }

    @Test
    fun `getAllEmployees delegates to dao`() = runBlocking {
        val employees = listOf(
            EmployeeEntity(id = 1, firstName = "Anna", lastName = "Nowak", companyId = 10, email = "anna@acme.com", phone = null, department = "HR", position = null, notes = null)
        )
        whenever(employeeDao.getAll()).thenReturn(employees)

        val result = repository.getAllEmployees()

        assertEquals(employees, result)
        verify(employeeDao).getAll()
    }

    @Test
    fun `getEmployeeById delegates to dao`() = runBlocking {
        val employee = EmployeeEntity(id = 5, firstName = "Piotr", lastName = "Nowak", companyId = 11, email = "piotr@acme.com", phone = null, department = "Ops", position = null, notes = null)
        whenever(employeeDao.getById(5)).thenReturn(employee)

        val result = repository.getEmployeeById(5)

        assertNotNull(result)
        assertEquals("Piotr", result?.firstName)
        verify(employeeDao).getById(5)
    }

    @Test
    fun `getEmployeeById returns null when dao returns null`() = runBlocking {
        whenever(employeeDao.getById(404)).thenReturn(null)

        val result = repository.getEmployeeById(404)

        assertNull(result)
        verify(employeeDao).getById(404)
    }

    @Test
    fun `getEmployeeByEmail delegates to dao`() = runBlocking {
        val employee = EmployeeEntity(id = 3, firstName = "Ewa", lastName = "Lis", companyId = 8, email = "ewa@acme.com", phone = null, department = "Sales", position = null, notes = null)
        whenever(employeeDao.getByEmail("ewa@acme.com")).thenReturn(employee)

        val result = repository.getEmployeeByEmail("ewa@acme.com")

        assertEquals(employee, result)
        verify(employeeDao).getByEmail("ewa@acme.com")
    }

    @Test
    fun `searchEmployees delegates to dao with filters`() {
        val flow = flowOf(emptyList<EmployeeEntity>())
        whenever(employeeDao.searchEmployees("jan", "IT")).thenReturn(flow)

        val result = repository.searchEmployees("jan", "IT")

        assertEquals(flow, result)
        verify(employeeDao).searchEmployees("jan", "IT")
    }

    @Test
    fun `getEmployeesByCompany delegates to dao`() = runBlocking {
        val employees = listOf(
            EmployeeEntity(id = 2, firstName = "Ola", lastName = "Kot", companyId = 77, email = null, phone = null, department = null, position = null, notes = null)
        )
        whenever(employeeDao.getByCompany(77)).thenReturn(employees)

        val result = repository.getEmployeesByCompany(77)

        assertEquals(1, result.size)
        assertEquals(77L, result.first().companyId)
        verify(employeeDao).getByCompany(77)
    }

    @Test
    fun `searchEmployeesByCompany delegates to dao with company and filters`() {
        val flow = flowOf(emptyList<EmployeeEntity>())
        whenever(employeeDao.searchEmployeesByCompany(8, "Now", "Finance")).thenReturn(flow)

        val result = repository.searchEmployeesByCompany(8, "Now", "Finance")

        assertEquals(flow, result)
        verify(employeeDao).searchEmployeesByCompany(8, "Now", "Finance")
    }

    @Test
    fun `insertEmployee delegates to dao and returns id`() = runBlocking {
        val employee = EmployeeEntity(firstName = "Insert", lastName = "User", companyId = 1, email = null, phone = null, department = null, position = null, notes = null)
        whenever(employeeDao.insert(employee)).thenReturn(42L)

        val result = repository.insertEmployee(employee)

        assertEquals(42L, result)
        verify(employeeDao).insert(employee)
    }

    @Test
    fun `updateEmployee sets updatedAt and delegates to dao`() = runBlocking {
        val original = EmployeeEntity(
            id = 7,
            firstName = "Update",
            lastName = "Me",
            companyId = 4,
            email = "update@acme.com",
            phone = null,
            department = "IT",
            position = "Dev",
            notes = null,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        doNothing().whenever(employeeDao).update(any())

        repository.updateEmployee(original)

        val captor = argumentCaptor<EmployeeEntity>()
        verify(employeeDao).update(captor.capture())
        val updated = captor.firstValue
        assertEquals(original.id, updated.id)
        assertEquals(original.firstName, updated.firstName)
        assertEquals(original.lastName, updated.lastName)
        assertEquals(original.companyId, updated.companyId)
        assertTrue(updated.updatedAt >= original.updatedAt)
    }

    @Test
    fun `deleteEmployee delegates to dao`() = runBlocking {
        val employee = EmployeeEntity(id = 3, firstName = "Delete", lastName = "Me", companyId = 2, email = null, phone = null, department = null, position = null, notes = null)
        doNothing().whenever(employeeDao).delete(employee)

        repository.deleteEmployee(employee)

        verify(employeeDao).delete(employee)
    }

    @Test
    fun `deleteEmployees delegates to dao`() = runBlocking {
        val ids = listOf(1L, 2L, 3L)
        doNothing().whenever(employeeDao).deleteByIds(ids)

        repository.deleteEmployees(ids)

        verify(employeeDao).deleteByIds(ids)
    }

    @Test
    fun `getAllDepartments delegates to dao`() = runBlocking {
        val departments = listOf("HR", "IT")
        whenever(employeeDao.getAllDepartments()).thenReturn(departments)

        val result = repository.getAllDepartments()

        assertEquals(departments, result)
        verify(employeeDao).getAllDepartments()
    }
}
