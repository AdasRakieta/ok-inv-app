package com.example.inventoryapp.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.CompanyEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CompanyDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var companyDao: CompanyDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        companyDao = database.companyDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert company and retrieve by id`() = runBlocking {
        val company = CompanyEntity(
            name = "Test Company",
            nip = "1234567890",
            address = "Test Street 123",
            city = "Warsaw",
            postalCode = "00-001",
            contactPerson = "John Doe",
            email = "contact@test.com",
            phone = "+48123456789",
            notes = "Test notes"
        )

        val id = companyDao.insert(company)
        assertTrue(id > 0)

        val retrieved = companyDao.getById(id)
        assertNotNull(retrieved)
        assertEquals("Test Company", retrieved?.name)
        assertEquals("1234567890", retrieved?.nip)
        assertEquals("Test Street 123", retrieved?.address)
        assertEquals("Warsaw", retrieved?.city)
        assertEquals("00-001", retrieved?.postalCode)
        assertEquals("John Doe", retrieved?.contactPerson)
        assertEquals("contact@test.com", retrieved?.email)
        assertEquals("+48123456789", retrieved?.phone)
        assertEquals("Test notes", retrieved?.notes)
    }

    @Test
    fun `getAll returns all companies`() = runBlocking {
        val company1 = CompanyEntity(
            name = "Alpha Company",
            nip = "1111111111",
            address = "Alpha Street 1",
            city = "Warsaw",
            postalCode = "00-001"
        )
        val company2 = CompanyEntity(
            name = "Beta Company",
            nip = "2222222222",
            address = "Beta Street 2",
            city = "Krakow",
            postalCode = "30-001"
        )
        val company3 = CompanyEntity(
            name = "Gamma Company",
            nip = "3333333333",
            address = "Gamma Street 3",
            city = "Gdansk",
            postalCode = "80-001"
        )

        companyDao.insert(company1)
        companyDao.insert(company2)
        companyDao.insert(company3)

        val all = companyDao.getAll()
        assertEquals(3, all.size)
        assertEquals("Alpha Company", all[0].name)
        assertEquals("Beta Company", all[1].name)
        assertEquals("Gamma Company", all[2].name)
    }

    @Test
    fun `search by name returns matching companies`() = runBlocking {
        val company1 = CompanyEntity(
            name = "Tech Solutions Ltd",
            nip = "1111111111",
            address = "Tech Street 1",
            city = "Warsaw",
            postalCode = "00-001"
        )
        val company2 = CompanyEntity(
            name = "Digital Services Inc",
            nip = "2222222222",
            address = "Digital Avenue 2",
            city = "Krakow",
            postalCode = "30-001"
        )
        val company3 = CompanyEntity(
            name = "Tech Innovations",
            nip = "3333333333",
            address = "Innovation Blvd 3",
            city = "Gdansk",
            postalCode = "80-001"
        )

        companyDao.insert(company1)
        companyDao.insert(company2)
        companyDao.insert(company3)

        val results = companyDao.search("Tech")
        assertEquals(2, results.size)
        assertTrue(results.any { it.name == "Tech Solutions Ltd" })
        assertTrue(results.any { it.name == "Tech Innovations" })
    }

    @Test
    fun `search by city returns matching companies`() = runBlocking {
        val company1 = CompanyEntity(
            name = "Warsaw Corp",
            nip = "1111111111",
            address = "Street 1",
            city = "Warsaw",
            postalCode = "00-001"
        )
        val company2 = CompanyEntity(
            name = "Krakow Inc",
            nip = "2222222222",
            address = "Street 2",
            city = "Krakow",
            postalCode = "30-001"
        )
        val company3 = CompanyEntity(
            name = "Warsaw Ltd",
            nip = "3333333333",
            address = "Street 3",
            city = "Warsaw",
            postalCode = "00-002"
        )

        companyDao.insert(company1)
        companyDao.insert(company2)
        companyDao.insert(company3)

        val results = companyDao.search("Warsaw")
        assertEquals(2, results.size) // Matches both company names containing "Warsaw"
        assertTrue(results.any { it.name == "Warsaw Corp" })
        assertTrue(results.any { it.name == "Warsaw Ltd" })
    }

    @Test
    fun `search with empty query returns all companies`() = runBlocking {
        val company1 = CompanyEntity(
            name = "Company A",
            nip = "1111111111",
            address = "Street 1",
            city = "Warsaw",
            postalCode = "00-001"
        )
        val company2 = CompanyEntity(
            name = "Company B",
            nip = "2222222222",
            address = "Street 2",
            city = "Krakow",
            postalCode = "30-001"
        )

        companyDao.insert(company1)
        companyDao.insert(company2)

        val results = companyDao.search("")
        assertEquals(2, results.size)
    }

    @Test
    fun `update company changes data`() = runBlocking {
        val company = CompanyEntity(
            name = "Original Name",
            nip = "1234567890",
            address = "Old Street",
            city = "Warsaw",
            postalCode = "00-001"
        )

        val id = companyDao.insert(company)
        val updated = company.copy(
            id = id,
            name = "Updated Name",
            address = "New Street"
        )

        companyDao.update(updated)

        val retrieved = companyDao.getById(id)
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved?.name)
        assertEquals("New Street", retrieved?.address)
        assertEquals("1234567890", retrieved?.nip) // Unchanged
    }

    @Test
    fun `delete company removes it from database`() = runBlocking {
        val company = CompanyEntity(
            name = "To Delete",
            nip = "1234567890",
            address = "Delete Street",
            city = "Warsaw",
            postalCode = "00-001"
        )

        val id = companyDao.insert(company)
        var retrieved = companyDao.getById(id)
        assertNotNull(retrieved)

        companyDao.delete(retrieved!!)

        retrieved = companyDao.getById(id)
        assertNull(retrieved)
    }

    @Test
    fun `deleteByIds removes multiple companies`() = runBlocking {
        val company1 = CompanyEntity(
            name = "Company 1",
            nip = "1111111111",
            address = "Street 1",
            city = "Warsaw",
            postalCode = "00-001"
        )
        val company2 = CompanyEntity(
            name = "Company 2",
            nip = "2222222222",
            address = "Street 2",
            city = "Krakow",
            postalCode = "30-001"
        )
        val company3 = CompanyEntity(
            name = "Company 3",
            nip = "3333333333",
            address = "Street 3",
            city = "Gdansk",
            postalCode = "80-001"
        )

        val id1 = companyDao.insert(company1)
        val id2 = companyDao.insert(company2)
        val id3 = companyDao.insert(company3)

        companyDao.deleteByIds(listOf(id1, id3))

        val remaining = companyDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("Company 2", remaining[0].name)
    }

    @Test
    fun `getByNip returns company with matching NIP`() = runBlocking {
        val company = CompanyEntity(
            name = "NIP Test Company",
            nip = "9876543210",
            address = "NIP Street",
            city = "Warsaw",
            postalCode = "00-001"
        )

        companyDao.insert(company)

        val retrieved = companyDao.getByNip("9876543210")
        assertNotNull(retrieved)
        assertEquals("NIP Test Company", retrieved?.name)
    }

    @Test
    fun `getByNip returns null for non-existent NIP`() = runBlocking {
        val retrieved = companyDao.getByNip("0000000000")
        assertNull(retrieved)
    }
}
