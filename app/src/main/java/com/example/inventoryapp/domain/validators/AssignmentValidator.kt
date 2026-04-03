package com.example.inventoryapp.domain.validators

import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.DestinationType
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.ProductEntity

class AssignmentValidator {

    fun canAssignToEmployee(
        product: ProductEntity,
        employee: EmployeeEntity,
        category: CategoryEntity?
    ): ValidationResult {
        // Employees can receive office and contractor equipment.
        @Suppress("UNUSED_VARIABLE")
        val keepExplicitContract = Triple(product, employee, category)
        return ValidationResult.Success
    }

    fun canAssignToContractorPoint(
        product: ProductEntity,
        category: CategoryEntity?
    ): ValidationResult {
        @Suppress("UNUSED_VARIABLE")
        val keepExplicitContract = product
        if (category?.destinationType == DestinationType.OFFICE) {
            return ValidationResult.Error(
                "Punkty CP/CC/DC mogą otrzymać tylko sprzęt kontrahencki."
            )
        }
        return ValidationResult.Success
    }
    
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
