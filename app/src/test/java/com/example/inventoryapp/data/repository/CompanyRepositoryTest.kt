package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.CompanyDao
import com.example.inventoryapp.data.local.entities.CompanyEntity
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

class CompanyRepositoryTest {

    private lateinit var companyDao: CompanyDao
    private lateinit var repository: CompanyRepository

    @Before
    fun setup() {
        companyDao = mock()
        repository = CompanyRepository(companyDao)
    }

    @Test
    fun `getAllCompaniesFlow delegates to dao`() {
        val companies = listOf(
            CompanyEntity(id = 1, name = "Flow Co", taxId = "1234567890", address = null, city = null, postalCode = null)
        )
        val flow = flowOf(companies)
        whenever(companyDao.getAllFlow()).thenReturn(flow)

        val result = repository.getAllCompaniesFlow()

        assertEquals(flow, result)
        verify(companyDao).getAllFlow()
    }

    @Test
    fun `getAllCompanies delegates to dao`() = runBlocking {
        val companies = listOf(
            CompanyEntity(id = 1, name = "Alpha", taxId = "1111111111", address = null, city = null, postalCode = null),
            CompanyEntity(id = 2, name = "Beta", taxId = "2222222222", address = null, city = null, postalCode = null)
        )
        whenever(companyDao.getAll()).thenReturn(companies)

        val result = repository.getAllCompanies()

        assertEquals(companies, result)
        verify(companyDao).getAll()
    }

    @Test
    fun `getCompanyById delegates to dao`() = runBlocking {
        val company = CompanyEntity(id = 5, name = "Acme", taxId = "5555555555", address = "Street", city = "City", postalCode = "00-001")
        whenever(companyDao.getById(5)).thenReturn(company)

        val result = repository.getCompanyById(5)

        assertNotNull(result)
        assertEquals("Acme", result?.name)
        verify(companyDao).getById(5)
    }

    @Test
    fun `getCompanyByTaxId delegates to dao`() = runBlocking {
        val company = CompanyEntity(id = 9, name = "Tax Co", taxId = "9999999999", address = null, city = null, postalCode = null)
        whenever(companyDao.getByTaxId("9999999999")).thenReturn(company)

        val result = repository.getCompanyByTaxId("9999999999")

        assertEquals(company, result)
        verify(companyDao).getByTaxId("9999999999")
    }

    @Test
    fun `searchCompanies delegates to dao`() = runBlocking {
        val matches = listOf(
            CompanyEntity(id = 1, name = "Tech Solutions", taxId = "1010101010", address = null, city = "Warsaw", postalCode = null)
        )
        whenever(companyDao.search("Tech")).thenReturn(matches)

        val result = repository.searchCompanies("Tech")

        assertEquals(1, result.size)
        assertEquals("Tech Solutions", result[0].name)
        verify(companyDao).search("Tech")
    }

    @Test
    fun `insertCompany delegates to dao and returns id`() = runBlocking {
        val company = CompanyEntity(name = "Insert Co", taxId = "1231231231", address = null, city = null, postalCode = null)
        whenever(companyDao.insert(company)).thenReturn(42L)

        val result = repository.insertCompany(company)

        assertEquals(42L, result)
        verify(companyDao).insert(company)
    }

    @Test
    fun `updateCompany sets updatedAt and delegates to dao`() = runBlocking {
        val original = CompanyEntity(
            id = 7,
            name = "Update Co",
            taxId = "7777777777",
            address = "Old",
            city = "City",
            postalCode = "00-001",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        doNothing().whenever(companyDao).update(any())

        repository.updateCompany(original)

        val captor = argumentCaptor<CompanyEntity>()
        verify(companyDao).update(captor.capture())
        val updated = captor.firstValue
        assertEquals(original.id, updated.id)
        assertEquals(original.name, updated.name)
        assertEquals(original.taxId, updated.taxId)
        assertTrue(updated.updatedAt >= original.updatedAt)
    }

    @Test
    fun `deleteCompany delegates to dao`() = runBlocking {
        val company = CompanyEntity(id = 3, name = "Delete Co", taxId = "3333333333", address = null, city = null, postalCode = null)
        doNothing().whenever(companyDao).delete(company)

        repository.deleteCompany(company)

        verify(companyDao).delete(company)
    }

    @Test
    fun `deleteCompanies delegates to dao`() = runBlocking {
        val ids = listOf(1L, 2L, 3L)
        doNothing().whenever(companyDao).deleteByIds(ids)

        repository.deleteCompanies(ids)

        verify(companyDao).deleteByIds(ids)
    }

    @Test
    fun `getCompanyById returns null when dao returns null`() = runBlocking {
        whenever(companyDao.getById(404)).thenReturn(null)

        val result = repository.getCompanyById(404)

        assertNull(result)
        verify(companyDao).getById(404)
    }
}
