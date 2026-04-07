package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.ContractorPointDao
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.PointType
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

class ContractorPointRepositoryTest {

    private lateinit var contractorPointDao: ContractorPointDao
    private lateinit var repository: ContractorPointRepository

    @Before
    fun setup() {
        contractorPointDao = mock()
        repository = ContractorPointRepository(contractorPointDao)
    }

    @Test
    fun `getAllContractorPointsFlow delegates to dao`() {
        val points = listOf(
            ContractorPointEntity(id = 1, code = "FLOW-001", name = "Flow Point", pointType = PointType.CP, companyId = 10)
        )
        val flow = flowOf(points)
        whenever(contractorPointDao.getAllFlow()).thenReturn(flow)

        val result = repository.getAllContractorPointsFlow()

        assertEquals(flow, result)
        verify(contractorPointDao).getAllFlow()
    }

    @Test
    fun `getAllContractorPoints delegates to dao`() {
        runBlocking {
            val points = listOf(
                ContractorPointEntity(id = 1, code = "CP-001", name = "Point A", pointType = PointType.CP, companyId = 10),
                ContractorPointEntity(id = 2, code = "CC-001", name = "Point B", pointType = PointType.CC, companyId = 11)
            )
            whenever(contractorPointDao.getAll()).thenReturn(points)

            val result = repository.getAllContractorPoints()

            assertEquals(points, result)
            verify(contractorPointDao).getAll()
        }
    }

    @Test
    fun `getContractorPointById delegates to dao`() {
        runBlocking {
            val point = ContractorPointEntity(id = 5, code = "DC-005", name = "DC Point", pointType = PointType.DC, companyId = 99)
            whenever(contractorPointDao.getById(5)).thenReturn(point)

            val result = repository.getContractorPointById(5)

            assertNotNull(result)
            assertEquals("DC-005", result?.code)
            verify(contractorPointDao).getById(5)
        }
    }

    @Test
    fun `getContractorPointByCode delegates to dao`() {
        runBlocking {
            val point = ContractorPointEntity(id = 9, code = "CP-009", name = "Code Point", pointType = PointType.CP, companyId = 1)
            whenever(contractorPointDao.getByCode("CP-009")).thenReturn(point)

            val result = repository.getContractorPointByCode("CP-009")

            assertEquals(point, result)
            verify(contractorPointDao).getByCode("CP-009")
        }
    }

    @Test
    fun `getContractorPointsByType delegates to dao`() {
        runBlocking {
            val points = listOf(
                ContractorPointEntity(id = 1, code = "CP-001", name = "CP 1", pointType = PointType.CP, companyId = 1)
            )
            whenever(contractorPointDao.getByPointType(PointType.CP)).thenReturn(points)

            val result = repository.getContractorPointsByType(PointType.CP)

            assertEquals(1, result.size)
            assertEquals("CP-001", result.first().code)
            verify(contractorPointDao).getByPointType(PointType.CP)
        }
    }

    @Test
    fun `getContractorPointsByCompany delegates to dao`() {
        runBlocking {
            val points = listOf(
                ContractorPointEntity(id = 1, code = "CP-010", name = "Company point", pointType = PointType.CP, companyId = 77)
            )
            whenever(contractorPointDao.getByCompany(77)).thenReturn(points)

            val result = repository.getContractorPointsByCompany(77)

            assertEquals(1, result.size)
            assertEquals("CP-010", result.first().code)
            verify(contractorPointDao).getByCompany(77)
        }
    }

    @Test
    fun `searchContractorPoints delegates to dao`() {
        runBlocking {
            val points = listOf(
                ContractorPointEntity(id = 1, code = "DC-100", name = "Distribution Center", pointType = PointType.DC, companyId = 8, city = "Krakow")
            )
            whenever(contractorPointDao.search("Krakow")).thenReturn(points)

            val result = repository.searchContractorPoints("Krakow")

            assertEquals(1, result.size)
            assertEquals("DC-100", result[0].code)
            verify(contractorPointDao).search("Krakow")
        }
    }

    @Test
    fun `insertContractorPoint delegates to dao and returns id`() {
        runBlocking {
            val point = ContractorPointEntity(code = "CP-200", name = "Insert Point", pointType = PointType.CP, companyId = 3)
            whenever(contractorPointDao.insert(point)).thenReturn(42L)

            val result = repository.insertContractorPoint(point)

            assertEquals(42L, result)
            verify(contractorPointDao).insert(point)
        }
    }

    @Test
    fun `updateContractorPoint sets updatedAt and delegates to dao`() {
        runBlocking {
            val original = ContractorPointEntity(
                id = 7,
                code = "CC-007",
                name = "Update Point",
                pointType = PointType.CC,
                companyId = 4,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        

            repository.updateContractorPoint(original)

            val captor = argumentCaptor<ContractorPointEntity>()
            verify(contractorPointDao).update(captor.capture())
            val updated = captor.firstValue
            assertEquals(original.id, updated.id)
            assertEquals(original.code, updated.code)
            assertEquals(original.name, updated.name)
            assertTrue(updated.updatedAt >= original.updatedAt)
        }
    }

    @Test
    fun `deleteContractorPoint delegates to dao`() {
        runBlocking {
            val point = ContractorPointEntity(id = 3, code = "CP-003", name = "Delete Point", pointType = PointType.CP, companyId = 2)
        

            repository.deleteContractorPoint(point)

            verify(contractorPointDao).delete(point)
        }
    }

    @Test
    fun `deleteContractorPoints delegates to dao`() {
        runBlocking {
            val ids = listOf(1L, 2L, 3L)
        

            repository.deleteContractorPoints(ids)

            verify(contractorPointDao).deleteByIds(ids)
        }
    }

    @Test
    fun `getContractorPointById returns null when dao returns null`() {
        runBlocking {
            whenever(contractorPointDao.getById(404)).thenReturn(null)

            val result = repository.getContractorPointById(404)

            assertNull(result)
            verify(contractorPointDao).getById(404)
        }
    }
}
