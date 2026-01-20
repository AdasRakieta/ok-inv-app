package com.example.inventoryapp.ui.warehouse

import android.content.Context

class LocationStorage(context: Context) {
    private val prefs = context.getSharedPreferences("locations_prefs", Context.MODE_PRIVATE)

    fun getLocations(): Set<String> = prefs.getStringSet(KEY_LOCATIONS, emptySet()) ?: emptySet()

    fun addLocation(name: String) {
        val updated = getLocations().toMutableSet().apply { add(name) }
        prefs.edit().putStringSet(KEY_LOCATIONS, updated).apply()
    }

    fun removeLocation(name: String) {
        val updated = getLocations().toMutableSet().apply { remove(name) }
        prefs.edit().putStringSet(KEY_LOCATIONS, updated).apply()
    }

    fun renameLocation(oldName: String, newName: String) {
        val updated = getLocations().toMutableSet().apply {
            remove(oldName)
            add(newName)
        }
        prefs.edit().putStringSet(KEY_LOCATIONS, updated).apply()
    }

    companion object {
        private const val KEY_LOCATIONS = "location_names"
    }
}
