package com.example.inventoryapp.data.models

/**
 * Result of adding a product to a container (box or package)
 */
sealed class AddProductResult {
    object Success : AddProductResult()
    data class TransferredFromBox(val boxName: String) : AddProductResult()
    data class TransferredFromPackage(val packageName: String) : AddProductResult()
    data class AlreadyInActivePackage(val packageName: String, val status: String) : AddProductResult()
    data class Error(val message: String) : AddProductResult()
}