package com.example.inventoryapp.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EmployeeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var companyDao: CompanyDao
    private lateinit var employeeDao: EmployeeDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        companyDao = database.companyDao()
        employeeDao = database.employeeDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `getByCompany returns only employees for selected company`() = runBlocking {
        val companyA = insertCompany("Company A", "1111111111")
        val companyB = insertCompany("Company B", "2222222222")

        employeeDao.insert(
            EmployeeEntity(
                firstName = "Jan",
                lastName = "Kowalski",
                companyId = companyA,
                email = "jan@a.pl",
                phone = null,
                department = "IT",
                position = "Dev",
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "Anna",
                lastName = "Nowak",
                companyId = companyB,
                email = "anna@b.pl",
                phone = null,
                department = "HR",
                position = "Lead",
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "Piotr",
                lastName = "Wiśniewski",
                companyId = null,
                email = "piotr@none.pl",
                phone = null,
                department = "OPS",
                position = "Spec",
                notes = null
            )
        )

        val result = employeeDao.getByCompany(companyA)

        assertEquals(1, result.size)
        assertEquals("Jan", result.first().firstName)
        assertEquals(companyA, result.first().companyId)
    }

    @Test
    fun `searchEmployeesByCompany filters by company and query`() = runBlocking {
        val companyA = insertCompany("Company A", "1111111111")
        val companyB = insertCompany("Company B", "2222222222")

        employeeDao.insert(
            EmployeeEntity(
                firstName = "Jan",
                lastName = "Kowalski",
                companyId = companyA,
                email = "jan@a.pl",
                phone = null,
                department = "IT",
                position = "Developer",
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "Joanna",
                lastName = "Kowalska",
                companyId = companyA,
                email = "joanna@a.pl",
                phone = null,
                department = "HR",
                position = "Manager",
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "Jan",
                lastName = "Nowak",
                companyId = companyB,
                email = "jan@b.pl",
                phone = null,
                department = "IT",
                position = "Developer",
                notes = null
            )
        )

        val result = employeeDao.searchEmployeesByCompany(companyA, "Jan", "IT").first()

        assertEquals(1, result.size)
        assertTrue(result.all { it.companyId == companyA })
        assertEquals("Kowalski", result.first().lastName)
    }

    private suspend fun insertCompany(name: String, taxId: String): Long {
        return companyDao.insert(
            CompanyEntity(
                name = name,
                taxId = taxId,
                address = "Test Street 1",
                city = "Warsaw",
                postalCode = "00-001"
            )
        )
    }
}
