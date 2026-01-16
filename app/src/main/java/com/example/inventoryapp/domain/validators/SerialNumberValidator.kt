package com.example.inventoryapp.domain.validators

class SerialNumberValidator {
    
    fun validate(serialNumber: String): ValidationResult {
        return when {
            serialNumber.isBlank() -> ValidationResult.Error("Serial number cannot be empty")
            serialNumber.length < 3 -> ValidationResult.Error("Serial number too short (min 3 characters)")
            serialNumber.length > 100 -> ValidationResult.Error("Serial number too long (max 100 characters)")
            !serialNumber.matches(Regex("^[a-zA-Z0-9-_]+$")) -> 
                ValidationResult.Error("Serial number can only contain letters, numbers, hyphens and underscores")
            else -> ValidationResult.Success
        }
    }
    
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
