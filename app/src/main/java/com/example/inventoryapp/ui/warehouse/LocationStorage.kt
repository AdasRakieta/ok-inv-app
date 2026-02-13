package com.example.inventoryapp.ui.warehouse

import android.content.Context

class LocationStorage(context: Context) {
    private val prefs = context.getSharedPreferences("locations_prefs", Context.MODE_PRIVATE)

    fun getLocations(): Set<String> = prefs.getStringSet(KEY_LOCATIONS, emptySet()) ?: emptySet()

    fun getLocationDescription(name: String): String = prefs.getString("desc_$name", "") ?: ""

    fun getLocationColor(name: String): String? = prefs.getString("color_$name", null)

    fun addLocation(name: String, description: String = "") {
        val updated = getLocations().toMutableSet().apply { add(name) }
        prefs.edit().apply {
            putStringSet(KEY_LOCATIONS, updated)
            if (description.isNotEmpty()) {
                putString("desc_$name", description)
            }
            if (getLocationColor(name).isNullOrBlank()) {
                putString("color_$name", getNextPaletteColor())
            }
            apply()
        }
    }

    fun removeLocation(name: String) {
        val updated = getLocations().toMutableSet().apply { remove(name) }
        prefs.edit().apply {
            putStringSet(KEY_LOCATIONS, updated)
            remove("desc_$name")
            remove("color_$name")
            apply()
        }
    }

    fun renameLocation(oldName: String, newName: String) {
        val updated = getLocations().toMutableSet().apply {
            remove(oldName)
            add(newName)
        }
        val oldDescription = getLocationDescription(oldName)
        val oldColor = getLocationColor(oldName)
        prefs.edit().apply {
            putStringSet(KEY_LOCATIONS, updated)
            if (oldDescription.isNotEmpty()) {
                remove("desc_$oldName")
                putString("desc_$newName", oldDescription)
            }
            if (!oldColor.isNullOrBlank()) {
                remove("color_$oldName")
                putString("color_$newName", oldColor)
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

    fun getOrAssignLocationColor(name: String): String {
        val existing = getLocationColor(name)
        if (!existing.isNullOrBlank()) return existing
        val color = getNextPaletteColor()
        prefs.edit().putString("color_$name", color).apply()
        return color
    }

    private fun getNextPaletteColor(): String {
        val index = prefs.getInt(KEY_COLOR_INDEX, 0)
        val color = COLOR_PALETTE[index % COLOR_PALETTE.size]
        prefs.edit().putInt(KEY_COLOR_INDEX, index + 1).apply()
        return color
    }

    companion object {
        private const val KEY_LOCATIONS = "location_names"
        private const val KEY_COLOR_INDEX = "location_color_index"
        private val COLOR_PALETTE = listOf(
            "#1261FF",
            "#EC4899",
            "#F59E0B",
            "#10B981",
            "#8B5CF6",
            "#0EA5E9",
            "#6B7280"
        )
    }
}
