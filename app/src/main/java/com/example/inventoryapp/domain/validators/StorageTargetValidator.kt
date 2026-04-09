package com.example.inventoryapp.domain.validators

import com.example.inventoryapp.data.local.entities.ProductStatus

class StorageTargetValidator {

    fun validate(
        status: ProductStatus,
        storageTypeIsBox: Boolean,
        warehouseLocation: String?,
        selectedBoxId: Long?
    ): ValidationResult {
        if (status != ProductStatus.IN_STOCK) {
            return ValidationResult.Success
        }

        return if (storageTypeIsBox) {
            if (selectedBoxId == null) {
                ValidationResult.Error("Wybierz karton dla statusu Magazyn")
            } else {
                ValidationResult.Success
            }
        } else {
            if (warehouseLocation.isNullOrBlank()) {
                ValidationResult.Error("Wybierz lokalizację dla statusu Magazyn")
            } else {
                ValidationResult.Success
            }
        }
    }

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
