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

    @Test
    fun `insert and getById and getByEmail return saved employee`() = runBlocking {
        val companyA = insertCompany("Company A", "1111111111")
        val id = employeeDao.insert(
            EmployeeEntity(
                firstName = "Adam",
                lastName = "Nowicki",
                companyId = companyA,
                email = "adam@a.pl",
                phone = "+48100100100",
                department = "IT",
                position = "QA",
                notes = "note"
            )
        )

        val byId = employeeDao.getById(id)
        val byEmail = employeeDao.getByEmail("adam@a.pl")

        assertNotNull(byId)
        assertNotNull(byEmail)
        assertEquals("Adam", byId?.firstName)
        assertEquals(id, byEmail?.id)
        assertEquals(companyA, byEmail?.companyId)
    }

    @Test
    fun `searchEmployees filters by query and department`() = runBlocking {
        val companyA = insertCompany("Company A", "1111111111")
        employeeDao.insert(
            EmployeeEntity(
                firstName = "Anna",
                lastName = "Lis",
                companyId = companyA,
                email = "anna@a.pl",
                phone = null,
                department = "HR",
                position = "Manager",
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "Aneta",
                lastName = "Kaczmarek",
                companyId = companyA,
                email = "aneta@a.pl",
                phone = null,
                department = "IT",
                position = "Developer",
                notes = null
            )
        )

        val filtered = employeeDao.searchEmployees("An", "IT").first()

        assertEquals(1, filtered.size)
        assertEquals("Aneta", filtered.first().firstName)
        assertEquals("IT", filtered.first().department)
    }

    @Test
    fun `update delete and deleteByIds keep CRUD behavior consistent`() = runBlocking {
        val companyA = insertCompany("Company A", "1111111111")
        val id1 = employeeDao.insert(
            EmployeeEntity(
                firstName = "Jan",
                lastName = "Kowal",
                companyId = companyA,
                email = "jan1@a.pl",
                phone = null,
                department = "OPS",
                position = "Specialist",
                notes = null
            )
        )
        val id2 = employeeDao.insert(
            EmployeeEntity(
                firstName = "Ewa",
                lastName = "Kot",
                companyId = companyA,
                email = "ewa@a.pl",
                phone = null,
                department = "OPS",
                position = "Lead",
                notes = null
            )
        )
        val id3 = employeeDao.insert(
            EmployeeEntity(
                firstName = "Ola",
                lastName = "Lis",
                companyId = companyA,
                email = "ola@a.pl",
                phone = null,
                department = "HR",
                position = "Specialist",
                notes = null
            )
        )

        val existing = employeeDao.getById(id1)!!
        employeeDao.update(existing.copy(position = "Senior Specialist"))
        assertEquals("Senior Specialist", employeeDao.getById(id1)?.position)

        val toDelete = employeeDao.getById(id2)!!
        employeeDao.delete(toDelete)
        assertNull(employeeDao.getById(id2))

        employeeDao.deleteByIds(listOf(id3))
        val all = employeeDao.getAll()
        assertEquals(1, all.size)
        assertEquals(id1, all.first().id)
    }

    @Test
    fun `getAllDepartments returns distinct sorted non-empty values`() = runBlocking {
        val companyA = insertCompany("Company A", "1111111111")
        employeeDao.insert(
            EmployeeEntity(
                firstName = "A",
                lastName = "A",
                companyId = companyA,
                email = "a@a.pl",
                phone = null,
                department = "IT",
                position = null,
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "B",
                lastName = "B",
                companyId = companyA,
                email = "b@a.pl",
                phone = null,
                department = "HR",
                position = null,
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "C",
                lastName = "C",
                companyId = companyA,
                email = "c@a.pl",
                phone = null,
                department = "IT",
                position = null,
                notes = null
            )
        )
        employeeDao.insert(
            EmployeeEntity(
                firstName = "D",
                lastName = "D",
                companyId = companyA,
                email = "d@a.pl",
                phone = null,
                department = "",
                position = null,
                notes = null
            )
        )

        val departments = employeeDao.getAllDepartments()

        assertEquals(listOf("HR", "IT"), departments)
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
