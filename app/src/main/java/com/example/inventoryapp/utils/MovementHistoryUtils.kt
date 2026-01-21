package com.example.inventoryapp.utils

object MovementHistoryUtils {
    private const val SEPARATOR = "||"
    private const val MAX_ENTRIES = 15

    fun parse(history: String?): MutableList<String> {
        if (history.isNullOrBlank()) return mutableListOf()
        return history.split(SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
    }

    fun append(history: String?, entry: String): String {
        val items = parse(history)
        items.add(entry)
        val trimmed = if (items.size > MAX_ENTRIES) items.takeLast(MAX_ENTRIES) else items
        return trimmed.joinToString(SEPARATOR)
    }

    fun formatForDisplay(history: String?): String {
        val items = parse(history)
        return if (items.isEmpty()) "" else items.joinToString(" → ")
    }

    fun entryForLocation(locationName: String?): String {
        val name = locationName?.takeIf { it.isNotBlank() } ?: "Magazyn"
        return "Magazyn ($name)"
    }

    fun entryForEmployee(employeeName: String?): String {
        val name = employeeName?.takeIf { it.isNotBlank() } ?: "Nieznany"
        return "Pracownik: $name"
    }

    fun entryUnassigned(): String = "Brak przypisania"
}
