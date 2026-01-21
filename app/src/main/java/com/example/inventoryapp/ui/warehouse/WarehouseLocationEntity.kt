package com.example.inventoryapp.ui.warehouse

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WarehouseLocationEntity(
    val id: String = "${System.currentTimeMillis()}_${(0..1000).random()}",
    val name: String,
    val description: String = "",
    val shelf: String = "",
    val bin: String = ""
) : Parcelable
