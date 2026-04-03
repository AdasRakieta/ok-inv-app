package com.example.inventoryapp.utils

object MovementHistoryUtils {
    private const val SEPARATOR = "||"
    private const val MAX_ENTRIES = 15
    private const val TIME_SEPARATOR = "##"

    private data class HistoryEntry(val timestamp: Long?, val text: String)

    fun parse(history: String?): MutableList<String> {
        if (history.isNullOrBlank()) return mutableListOf()
        return history.split(SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
    }

    private fun parseEntries(history: String?): List<HistoryEntry> {
        return parse(history).map { raw ->
            if (raw.contains(TIME_SEPARATOR)) {
                val parts = raw.split(TIME_SEPARATOR, limit = 2)
                val time = parts.firstOrNull()?.toLongOrNull()
                val text = parts.getOrNull(1)?.trim().orEmpty()
                HistoryEntry(time, text)
            } else {
                HistoryEntry(null, raw)
            }
        }.filter { it.text.isNotBlank() }
    }

    fun append(history: String?, entry: String, timestamp: Long = System.currentTimeMillis()): String {
        val items = parse(history)
        items.add("$timestamp$TIME_SEPARATOR$entry")
        val trimmed = if (items.size > MAX_ENTRIES) items.takeLast(MAX_ENTRIES) else items
        return trimmed.joinToString(SEPARATOR)
    }

    fun formatForDisplay(history: String?): String {
        val items = parseEntries(history)
        if (items.isEmpty()) return ""
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return items.joinToString("\n") { entry ->
            val date = entry.timestamp?.let { formatter.format(java.util.Date(it)) }
            if (date != null) "• ${entry.text} ($date)" else "• ${entry.text}"
        }
    }

    fun entryForLocation(locationName: String?): String {
        val name = locationName?.takeIf { it.isNotBlank() } ?: "Magazyn"
        return "Magazyn ($name)"
    }

    fun entryForEmployee(employeeName: String?): String {
        val name = employeeName?.takeIf { it.isNotBlank() } ?: "Nieznany"
        return "Pracownik: $name"
    }

    fun entryForContractorPoint(contractorPointName: String?): String {
        val name = contractorPointName?.takeIf { it.isNotBlank() } ?: "Nieznany punkt"
        return "Punkt kontrahenta: $name"
    }

    fun entryForStatus(statusLabel: String): String = statusLabel

    fun entryUnassigned(): String = "Brak przypisania"
}
