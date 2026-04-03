package com.example.inventoryapp.data.local.dao

import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.PointType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContractorPointDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var companyDao: CompanyDao
    private lateinit var contractorPointDao: ContractorPointDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        database.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA foreign_keys = ON")).close()
        companyDao = database.companyDao()
        contractorPointDao = database.contractorPointDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and getById returns contractor point`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        val point = ContractorPointEntity(
            code = "CP-001",
            name = "Point Alpha",
            pointType = PointType.CP,
            companyId = companyId,
            city = "Warsaw"
        )

        val id = contractorPointDao.insert(point)
        val saved = contractorPointDao.getById(id)

        assertNotNull(saved)
        assertEquals("CP-001", saved?.code)
        assertEquals(PointType.CP, saved?.pointType)
        assertEquals(companyId, saved?.companyId)
        assertEquals("Warsaw", saved?.city)
    }

    @Test
    fun `getByPointType returns only matching rows`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-001",
                name = "CP Point",
                pointType = PointType.CP,
                companyId = companyId
            )
        )
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CC-001",
                name = "CC Point",
                pointType = PointType.CC,
                companyId = companyId
            )
        )

        val cpPoints = contractorPointDao.getByPointType(PointType.CP)
        assertEquals(1, cpPoints.size)
        assertEquals("CP-001", cpPoints.first().code)
    }

    @Test
    fun `getByCompany returns only matching company rows`() = runBlocking {
        val companyAId = insertCompany("Company A", "1111111111")
        val companyBId = insertCompany("Company B", "2222222222")

        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-A1",
                name = "Point A1",
                pointType = PointType.CP,
                companyId = companyAId
            )
        )
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-B1",
                name = "Point B1",
                pointType = PointType.CP,
                companyId = companyBId
            )
        )

        val pointsForA = contractorPointDao.getByCompany(companyAId)
        assertEquals(1, pointsForA.size)
        assertEquals("CP-A1", pointsForA.first().code)
    }

    @Test
    fun `getAll and getByCode return expected contractor points`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-010",
                name = "Point 10",
                pointType = PointType.CP,
                companyId = companyId
            )
        )
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CC-020",
                name = "Point 20",
                pointType = PointType.CC,
                companyId = companyId
            )
        )

        val all = contractorPointDao.getAll()
        val byCode = contractorPointDao.getByCode("CC-020")

        assertEquals(2, all.size)
        assertNotNull(byCode)
        assertEquals("Point 20", byCode?.name)
    }

    @Test
    fun `search finds by code name and city`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "DC-700",
                name = "Distribution Center",
                pointType = PointType.DC,
                companyId = companyId,
                city = "Krakow"
            )
        )
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CC-100",
                name = "Courier Center",
                pointType = PointType.CC,
                companyId = companyId,
                city = "Warsaw"
            )
        )

        val byCode = contractorPointDao.search("DC-700")
        val byName = contractorPointDao.search("Courier")
        val byCity = contractorPointDao.search("Krakow")

        assertEquals(1, byCode.size)
        assertEquals("DC-700", byCode.first().code)
        assertEquals(1, byName.size)
        assertEquals("CC-100", byName.first().code)
        assertEquals(1, byCity.size)
        assertEquals("DC-700", byCity.first().code)
    }

    @Test
    fun `update changes saved fields`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        val id = contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-001",
                name = "Old Name",
                pointType = PointType.CP,
                companyId = companyId,
                notes = "old"
            )
        )

        val existing = contractorPointDao.getById(id)!!
        contractorPointDao.update(
            existing.copy(
                name = "New Name",
                notes = "new"
            )
        )

        val updated = contractorPointDao.getById(id)
        assertEquals("New Name", updated?.name)
        assertEquals("new", updated?.notes)
    }

    @Test
    fun `delete removes row`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        val id = contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-001",
                name = "To Delete",
                pointType = PointType.CP,
                companyId = companyId
            )
        )
        val existing = contractorPointDao.getById(id)
        assertNotNull(existing)

        contractorPointDao.delete(existing!!)

        assertNull(contractorPointDao.getById(id))
    }

    @Test
    fun `deleteByIds removes only selected rows`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        val id1 = contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-101",
                name = "Point 101",
                pointType = PointType.CP,
                companyId = companyId
            )
        )
        val id2 = contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-102",
                name = "Point 102",
                pointType = PointType.CP,
                companyId = companyId
            )
        )
        val id3 = contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-103",
                name = "Point 103",
                pointType = PointType.CP,
                companyId = companyId
            )
        )

        contractorPointDao.deleteByIds(listOf(id1, id3))
        val remaining = contractorPointDao.getAll()

        assertEquals(1, remaining.size)
        assertEquals(id2, remaining.first().id)
        assertEquals("CP-102", remaining.first().code)
    }

    @Test
    fun `unique code constraint blocks duplicates`() = runBlocking {
        val companyId = insertCompany("Company A", "1111111111")
        contractorPointDao.insert(
            ContractorPointEntity(
                code = "CP-001",
                name = "Point One",
                pointType = PointType.CP,
                companyId = companyId
            )
        )

        try {
            contractorPointDao.insert(
                ContractorPointEntity(
                    code = "CP-001",
                    name = "Point Two",
                    pointType = PointType.CC,
                    companyId = companyId
                )
            )
            fail("Expected unique constraint violation for duplicate contractor point code")
        } catch (ex: Exception) {
            assertTrue(ex.message?.contains("UNIQUE", ignoreCase = true) == true)
        }
    }

    @Test
    fun `foreign key constraint blocks point without company`() = runBlocking {
        try {
            contractorPointDao.insert(
                ContractorPointEntity(
                    code = "CP-999",
                    name = "Orphan Point",
                    pointType = PointType.CP,
                    companyId = Long.MAX_VALUE
                )
            )
            fail("Expected foreign key violation for missing company")
        } catch (ex: Exception) {
            assertTrue(ex.message?.contains("FOREIGN KEY", ignoreCase = true) == true)
        }
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
