package com.example.inventoryapp.ui.products.models

data class DisplayMovement(
    val id: Long,
    val action: String,
    val fromDisplay: String?,
    val toDisplay: String?,
    val timestamp: Long,
    val packageStatus: String?,
    val note: String?
)
