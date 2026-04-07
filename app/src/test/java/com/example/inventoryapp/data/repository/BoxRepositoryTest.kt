package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.BoxDao
import com.example.inventoryapp.data.local.entities.BoxEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

class BoxRepositoryTest {

    private val boxDao: BoxDao = mock()
    private val repository = BoxRepository(boxDao)

    @Test
    fun insertBox_generatesQrUidWhenMissing() = runTest {
        val box = BoxEntity(
            id = 0L,
            name = "Test Box",
            code = "B1",
            qrUid = null,
            warehouseLocationId = 1L,
            createdAt = System.currentTimeMillis()
        )

        whenever(boxDao.insertBox(any())).thenReturn(42L)

        val result = repository.insertBox(box)

        assertEquals(42L, result)

        val captor = argumentCaptor<BoxEntity>()
        verify(boxDao).insertBox(captor.capture())
        val captured = captor.firstValue
        assertNotNull(captured.qrUid)
        assertTrue(captured.qrUid!!.isNotBlank())
        assertEquals(box.name, captured.name)
        assertEquals(box.code, captured.code)
        assertEquals(box.warehouseLocationId, captured.warehouseLocationId)
    }
}
