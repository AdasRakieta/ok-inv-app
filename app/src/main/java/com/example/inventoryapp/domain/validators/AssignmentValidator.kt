package com.example.inventoryapp.domain.validators

import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.DestinationType
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.ProductEntity

class AssignmentValidator {

    @Suppress("UNUSED_PARAMETER")
    fun canAssignToEmployee(
        product: ProductEntity,
        employee: EmployeeEntity,
        category: CategoryEntity?
    ): ValidationResult {
        // Employees can receive office and contractor equipment.
        return ValidationResult.Success
    }

    @Suppress("UNUSED_PARAMETER")
    fun canAssignToContractorPoint(
        product: ProductEntity,
        category: CategoryEntity?
    ): ValidationResult {
        if (category == null) {
            return ValidationResult.Error(
                "Nie można zweryfikować kategorii sprzętu dla punktu CP/CC/DC."
            )
        }

        if (category.destinationType == DestinationType.OFFICE) {
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
