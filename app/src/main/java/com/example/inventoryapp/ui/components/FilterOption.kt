package com.example.inventoryapp.ui.components

import java.io.Serializable

data class FilterOption(
    val id: String,
    val label: String,
    val icon: String,
    val isSelected: Boolean = false
) : Serializable
