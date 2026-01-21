package com.example.inventoryapp.ui.warehouse

import android.content.Context

class LocationStorage(context: Context) {
    private val prefs = context.getSharedPreferences("locations_prefs", Context.MODE_PRIVATE)

    fun getLocations(): Set<String> = prefs.getStringSet(KEY_LOCATIONS, emptySet()) ?: emptySet()

    fun getLocationDescription(name: String): String = prefs.getString("desc_$name", "") ?: ""

    fun addLocation(name: String, description: String = "") {
        val updated = getLocations().toMutableSet().apply { add(name) }
        prefs.edit().apply {
            putStringSet(KEY_LOCATIONS, updated)
            if (description.isNotEmpty()) {
                putString("desc_$name", description)
            }
            apply()
        }
    }

    fun removeLocation(name: String) {
        val updated = getLocations().toMutableSet().apply { remove(name) }
        prefs.edit().apply {
            putStringSet(KEY_LOCATIONS, updated)
            remove("desc_$name")
            apply()
        }
    }

    fun renameLocation(oldName: String, newName: String) {
        val updated = getLocations().toMutableSet().apply {
            remove(oldName)
            add(newName)
        }
        val oldDescription = getLocationDescription(oldName)
        prefs.edit().apply {
            putStringSet(KEY_LOCATIONS, updated)
            if (oldDescription.isNotEmpty()) {
                remove("desc_$oldName")
                putString("desc_$newName", oldDescription)
            }
            apply()
        }
    }

    fun updateLocationDescription(name: String, description: String) {
        prefs.edit().apply {
            if (description.isNotEmpty()) {
                putString("desc_$name", description)
            } else {
                remove("desc_$name")
            }
            apply()
        }
    }

    companion object {
        private const val KEY_LOCATIONS = "location_names"
    }
}
