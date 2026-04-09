package com.example.inventoryapp.domain.validators

import com.example.inventoryapp.data.local.entities.ProductStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StorageTargetValidatorTest {

    private lateinit var validator: StorageTargetValidator

    @Before
    fun setup() {
        validator = StorageTargetValidator()
    }

    @Test
    fun `requires location when in stock and storage type is location`() {
        val result = validator.validate(
            status = ProductStatus.IN_STOCK,
            storageTypeIsBox = false,
            warehouseLocation = null,
            selectedBoxId = null
        )

        assertTrue(result is StorageTargetValidator.ValidationResult.Error)
        assertEquals(
            "Wybierz lokalizację dla statusu Magazyn",
            (result as StorageTargetValidator.ValidationResult.Error).message
        )
    }

    @Test
    fun `allows location when in stock and storage type is location`() {
        val result = validator.validate(
            status = ProductStatus.IN_STOCK,
            storageTypeIsBox = false,
            warehouseLocation = "A1 / R1",
            selectedBoxId = null
        )

        assertTrue(result is StorageTargetValidator.ValidationResult.Success)
    }

    @Test
    fun `requires box when in stock and storage type is box`() {
        val result = validator.validate(
            status = ProductStatus.IN_STOCK,
            storageTypeIsBox = true,
            warehouseLocation = null,
            selectedBoxId = null
        )

        assertTrue(result is StorageTargetValidator.ValidationResult.Error)
        assertEquals(
            "Wybierz karton dla statusu Magazyn",
            (result as StorageTargetValidator.ValidationResult.Error).message
        )
    }

    @Test
    fun `allows box when in stock and storage type is box`() {
        val result = validator.validate(
            status = ProductStatus.IN_STOCK,
            storageTypeIsBox = true,
            warehouseLocation = null,
            selectedBoxId = 10L
        )

        assertTrue(result is StorageTargetValidator.ValidationResult.Success)
    }

    @Test
    fun `does not require storage target when status is not in stock`() {
        val result = validator.validate(
            status = ProductStatus.ASSIGNED,
            storageTypeIsBox = false,
            warehouseLocation = null,
            selectedBoxId = null
        )

        assertTrue(result is StorageTargetValidator.ValidationResult.Success)
    }
}
