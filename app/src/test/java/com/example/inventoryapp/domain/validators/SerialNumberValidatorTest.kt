package com.example.inventoryapp.domain.validators

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SerialNumberValidatorTest {

    private lateinit var validator: SerialNumberValidator

    @Before
    fun setup() {
        validator = SerialNumberValidator()
    }

    @Test
    fun `validate empty serial number returns error`() {
        val result = validator.validate("")
        assertTrue(result is SerialNumberValidator.ValidationResult.Error)
        assertEquals("Serial number cannot be empty", (result as SerialNumberValidator.ValidationResult.Error).message)
    }

    @Test
    fun `validate blank serial number returns error`() {
        val result = validator.validate("   ")
        assertTrue(result is SerialNumberValidator.ValidationResult.Error)
    }

    @Test
    fun `validate too short serial number returns error`() {
        val result = validator.validate("AB")
        assertTrue(result is SerialNumberValidator.ValidationResult.Error)
        assertEquals("Serial number too short (min 3 characters)", (result as SerialNumberValidator.ValidationResult.Error).message)
    }

    @Test
    fun `validate too long serial number returns error`() {
        val result = validator.validate("A".repeat(101))
        assertTrue(result is SerialNumberValidator.ValidationResult.Error)
        assertEquals("Serial number too long (max 100 characters)", (result as SerialNumberValidator.ValidationResult.Error).message)
    }

    @Test
    fun `validate serial number with invalid characters returns error`() {
        val result = validator.validate("ABC@123")
        assertTrue(result is SerialNumberValidator.ValidationResult.Error)
    }

    @Test
    fun `validate valid alphanumeric serial number returns success`() {
        val result = validator.validate("ABC123")
        assertTrue(result is SerialNumberValidator.ValidationResult.Success)
    }

    @Test
    fun `validate valid serial number with hyphens returns success`() {
        val result = validator.validate("ABC-123-XYZ")
        assertTrue(result is SerialNumberValidator.ValidationResult.Success)
    }

    @Test
    fun `validate valid serial number with underscores returns success`() {
        val result = validator.validate("ABC_123_XYZ")
        assertTrue(result is SerialNumberValidator.ValidationResult.Success)
    }

    @Test
    fun `validate minimum length serial number returns success`() {
        val result = validator.validate("ABC")
        assertTrue(result is SerialNumberValidator.ValidationResult.Success)
    }

    @Test
    fun `validate maximum length serial number returns success`() {
        val result = validator.validate("A".repeat(100))
        assertTrue(result is SerialNumberValidator.ValidationResult.Success)
    }
}
