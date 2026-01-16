package com.example.inventoryapp.data.local.entity

/**
 * Represents a single row in the unified CSV format
 * All entities (products, packages, boxes, contractors) use the same CSV structure
 */
data class CsvRow(
    val type: String,                    // Product, Package, Box, Contractor
    val serialNumber: String?,           // Product only
    val name: String,                    // Required for all
    val description: String?,            // Optional
    val category: String?,               // Product only
    val quantity: Int?,                  // Product only
    val packageName: String?,            // Product: package assignment
    val boxName: String?,                // Product: box assignment
    val contractorName: String?,         // Package: contractor assignment
    val location: String?,               // Box only
    val status: String?,                 // Package only
    val createdDate: String?,            // Optional
    val shippedDate: String?,            // Package only
    val deliveredDate: String?           // Package only
) {
    companion object {
        const val TYPE_PRODUCT = "Product"
        const val TYPE_PACKAGE = "Package"
        const val TYPE_BOX = "Box"
        const val TYPE_CONTRACTOR = "Contractor"
        
        /**
         * CSV column headers in exact order
         */
        val CSV_HEADERS = listOf(
            "Type",
            "Serial Number",
            "Name",
            "Description",
            "Category",
            "Quantity",
            "Package Name",
            "Box Name",
            "Contractor Name",
            "Location",
            "Status",
            "Created Date",
            "Shipped Date",
            "Delivered Date"
        )
        
        /**
         * Parse CSV row from fields
         */
        fun fromCsvFields(fields: List<String>): CsvRow? {
            return try {
                if (fields.size < CSV_HEADERS.size) {
                    // Pad with empty strings
                    val paddedFields = fields + List(CSV_HEADERS.size - fields.size) { "" }
                    parseFields(paddedFields)
                } else {
                    parseFields(fields)
                }
            } catch (e: Exception) {
                null
            }
        }
        
        private fun parseFields(fields: List<String>): CsvRow {
            return CsvRow(
                type = fields[0],
                serialNumber = fields.getOrNull(1)?.takeIf { it.isNotEmpty() },
                name = fields.getOrNull(2) ?: "",
                description = fields.getOrNull(3)?.takeIf { it.isNotEmpty() },
                category = fields.getOrNull(4)?.takeIf { it.isNotEmpty() },
                quantity = fields.getOrNull(5)?.toIntOrNull(),
                packageName = fields.getOrNull(6)?.takeIf { it.isNotEmpty() },
                boxName = fields.getOrNull(7)?.takeIf { it.isNotEmpty() },
                contractorName = fields.getOrNull(8)?.takeIf { it.isNotEmpty() },
                location = fields.getOrNull(9)?.takeIf { it.isNotEmpty() },
                status = fields.getOrNull(10)?.takeIf { it.isNotEmpty() },
                createdDate = fields.getOrNull(11)?.takeIf { it.isNotEmpty() },
                shippedDate = fields.getOrNull(12)?.takeIf { it.isNotEmpty() },
                deliveredDate = fields.getOrNull(13)?.takeIf { it.isNotEmpty() }
            )
        }
        
        /**
         * Convert to CSV line
         */
        fun toCsvLine(row: CsvRow): String {
            val fields = listOf(
                row.type,
                row.serialNumber ?: "",
                row.name,
                row.description ?: "",
                row.category ?: "",
                row.quantity?.toString() ?: "",
                row.packageName ?: "",
                row.boxName ?: "",
                row.contractorName ?: "",
                row.location ?: "",
                row.status ?: "",
                row.createdDate ?: "",
                row.shippedDate ?: "",
                row.deliveredDate ?: ""
            )
            
            return fields.joinToString(",") { field ->
                if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                    // Escape quotes and wrap in quotes
                    "\"${field.replace("\"", "\"\"")}\""
                } else {
                    field
                }
            }
        }
    }
    
    fun isValid(): Boolean {
        // Type and Name are required
        if (type.isEmpty() || name.isEmpty()) return false
        
        // Validate type
        if (type !in listOf(TYPE_PRODUCT, TYPE_PACKAGE, TYPE_BOX, TYPE_CONTRACTOR)) {
            return false
        }
        
        return true
    }
}
