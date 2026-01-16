package com.example.inventoryapp.utils

/**
 * Helper class for managing product categories with icons
 */
object CategoryHelper {
    
    data class Category(
        val id: Long,
        val name: String,
        val icon: String,
        val requiresSerialNumber: Boolean = true
    )
    
    private val categories = listOf(
        Category(1L, "Scanner", "🔍", requiresSerialNumber = true),
        Category(2L, "Printer", "🖨️", requiresSerialNumber = true),
            Category(3L, "Scanner Docking Station", "🪫", requiresSerialNumber = true),
        Category(4L, "Printer Docking Station", "🔌", requiresSerialNumber = true),
        Category(5L, "Other", "📦", requiresSerialNumber = false)
    )
    
    fun getAllCategories(): List<Category> = categories
    
    fun getCategoryById(id: Long?): Category? {
        return categories.find { it.id == id }
    }
    
    fun getCategoryName(id: Long?): String {
        return getCategoryById(id)?.name ?: "Uncategorized"
    }
    
    fun getCategoryIcon(id: Long?): String {
        return getCategoryById(id)?.icon ?: "📦"
    }
    
    fun getCategoryIdByName(name: String): Long? {
        return categories.find { it.name.equals(name, ignoreCase = true) }?.id
    }
    
    /**
     * Alias for getCategoryIdByName for consistency
     */
    fun getCategoryId(name: String): Long? = getCategoryIdByName(name)
    
    fun getCategoryNames(): List<String> {
        return categories.map { it.name }
    }
    
    /**
     * Check if category requires serial number
     * @param categoryId Category ID to check
     * @return true if serial number is required, false otherwise
     */
    fun requiresSerialNumber(categoryId: Long?): Boolean {
        return getCategoryById(categoryId)?.requiresSerialNumber ?: true // Default to required
    }
    
    /**
     * Check if category requires serial number by name
     * @param categoryName Category name to check
     * @return true if serial number is required, false otherwise
     */
    fun requiresSerialNumber(categoryName: String): Boolean {
        val categoryId = getCategoryIdByName(categoryName)
        return requiresSerialNumber(categoryId)
    }
    
    /**
     * Validate serial number format based on category
     * - Scanners & Scanner Docking Stations: must start with 'S'
     * - Printers & Printer Docking Stations: must start with 'X'
     * - Other: no format restrictions
     * @param serialNumber Serial number to validate
     * @param categoryId Category ID to determine validation rules
     * @return true if valid or empty, false if invalid format
     */
    fun isValidSerialNumber(serialNumber: String?, categoryId: Long?): Boolean {
        if (serialNumber.isNullOrBlank()) return true // Empty is allowed
        
        // If category doesn't require SN, no format validation
        if (!requiresSerialNumber(categoryId)) return true
        
        // Determine required prefix based on category
        val requiredPrefix = when (categoryId) {
            1L, 3L -> "S" // Scanner, Scanner Docking Station
            2L, 4L -> "X" // Printer, Printer Docking Station
            else -> return true // Other categories or unknown - no restrictions
        }
        
        return serialNumber.startsWith(requiredPrefix, ignoreCase = true)
    }
    
    /**
     * Get validation error message for invalid serial number
     * @param serialNumber Serial number that failed validation
     * @param categoryId Category ID to determine validation rules
     * @return Error message or null if valid
     */
    fun getSerialNumberValidationError(serialNumber: String?, categoryId: Long?): String? {
        if (isValidSerialNumber(serialNumber, categoryId)) return null
        
        val requiredPrefix = when (categoryId) {
            1L, 3L -> "S" // Scanner, Scanner Docking Station
            2L, 4L -> "X" // Printer, Printer Docking Station
            else -> return null // Other categories - no restrictions
        }
        
        return "Serial number must start with '$requiredPrefix' (e.g., ${requiredPrefix}001, ${requiredPrefix}12345)"
    }

    // Package status constants
    object PackageStatus {
        const val WAREHOUSE = "WAREHOUSE"
        const val PREPARATION = "PREPARATION"
        const val READY = "READY"
        const val ISSUED = "ISSUED"
        const val RETURNED = "RETURNED"
        const val UNASSIGNED = "UNASSIGNED"

        val ALL_STATUSES = arrayOf(WAREHOUSE, PREPARATION, READY, ISSUED, RETURNED)
        val PACKAGE_STATUSES = arrayOf(WAREHOUSE, PREPARATION, READY, ISSUED, RETURNED)
        val FILTER_STATUSES = arrayOf(WAREHOUSE, PREPARATION, READY, ISSUED, RETURNED, UNASSIGNED)

        fun getDisplayName(status: String): String {
            return when (status) {
                WAREHOUSE -> "🏢 Warehouse"
                PREPARATION -> "🛠️ Preparation"
                READY -> "✅ Ready"
                ISSUED -> "📤 Issued"
                RETURNED -> "↩️ Returned"
                UNASSIGNED -> "❓ Unassigned"
                else -> status
            }
        }
    }
}
