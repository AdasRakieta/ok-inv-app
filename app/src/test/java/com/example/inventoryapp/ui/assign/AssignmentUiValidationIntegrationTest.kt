package com.example.inventoryapp.ui.assign

import com.example.inventoryapp.domain.validators.AssignmentValidator
import com.example.inventoryapp.ui.employees.EmployeeDetailsFragment
import com.example.inventoryapp.ui.products.AddProductFragment
import com.example.inventoryapp.ui.templates.BulkAddFragment
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentUiValidationIntegrationTest {

    @Test
    fun `employee details fragment uses assignment validator`() {
        val hasField = EmployeeDetailsFragment::class.java.declaredFields.any { field ->
            field.type == AssignmentValidator::class.java
        }
        assertTrue(hasField)
    }

    @Test
    fun `assign by scan fragment uses assignment validator`() {
        val hasField = AssignByScanFragment::class.java.declaredFields.any { field ->
            field.type == AssignmentValidator::class.java
        }
        assertTrue(hasField)
    }

    @Test
    fun `add product fragment uses assignment validator`() {
        val hasField = AddProductFragment::class.java.declaredFields.any { field ->
            field.type == AssignmentValidator::class.java
        }
        assertTrue(hasField)
    }

    @Test
    fun `bulk add fragment uses assignment validator`() {
        val hasField = BulkAddFragment::class.java.declaredFields.any { field ->
            field.type == AssignmentValidator::class.java
        }
        assertTrue(hasField)
    }
}
