package com.example.inventoryapp.printer

import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductWithCategory
import com.example.inventoryapp.data.local.entities.PrinterEntity
import java.text.SimpleDateFormat
import java.util.*

class ZplContentGenerator {

    companion object {
        private const val DPI_203 = 203
        private const val DPI_300 = 300

        // Default label dimensions (in dots for 203 DPI) - used as fallback only
        private const val DEFAULT_LABEL_WIDTH = 576  // 3 inches at 203 DPI
        private const val DEFAULT_LABEL_HEIGHT = 324 // 1.6 inches at 203 DPI

        /**
         * Convert millimeters to dots based on printer DPI
         * Formula: dots = (millimeters / 25.4) * DPI
         * @param mm Measurement in millimeters
         * @param dpi Printer dots per inch (203 or 300 typically)
         * @return Measurement in dots (rounded to nearest integer)
         */
        fun mmToDots(mm: Double, dpi: Int): Int {
            return ((mm / 25.4) * dpi).toInt()
        }

        /**
         * Convert millimeters to dots using Int input
         */
        fun mmToDots(mm: Int, dpi: Int): Int {
            return mmToDots(mm.toDouble(), dpi)
        }

        /**
         * Generate ZPL content for inventory item label
         * @param printer Printer configuration with label dimensions and DPI
         */
        fun generateInventoryLabel(
            itemCode: String,
            itemName: String,
            quantity: Int,
            location: String,
            timestamp: Date = Date(),
            printer: PrinterEntity
        ): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(timestamp)

            // Calculate label dimensions in dots
            val labelWidthDots = mmToDots(printer.labelWidthMm, printer.dpi)
            val labelHeightDots = printer.labelHeightMm?.let { mmToDots(it, printer.dpi) } ?: DEFAULT_LABEL_HEIGHT

            return """
                ^XA
                ^PW${labelWidthDots}
                ^LL${labelHeightDots}
                ^FO50,30^A0N,30,30^FDItem Code:^FS
                ^FO50,70^A0N,40,40^FD$itemCode^FS
                ^FO50,130^A0N,25,25^FD$itemName^FS
                ^FO50,170^A0N,20,20^FDQty: $quantity^FS
                ^FO50,200^A0N,20,20^FDLocation: $location^FS
                ^FO50,230^A0N,15,15^FD$formattedDate^FS
                ^FO400,30^BQN,2,4^FDMM,A$itemCode^FS
                ^XZ
            """.trimIndent()
        }

        /**
         * Legacy version for backward compatibility - uses default 50mm width, 203 DPI
         * @deprecated Use version with PrinterEntity parameter instead
         */
        @Deprecated("Use version with PrinterEntity parameter", ReplaceWith("generateInventoryLabel(itemCode, itemName, quantity, location, timestamp, printer)"))
        fun generateInventoryLabel(
            itemCode: String,
            itemName: String,
            quantity: Int,
            location: String,
            timestamp: Date = Date(),
            dpi: Int = DPI_203
        ): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(timestamp)

            return """
                ^XA
                ^PW${DEFAULT_LABEL_WIDTH}
                ^LL${DEFAULT_LABEL_HEIGHT}
                ^FO50,30^A0N,30,30^FDItem Code:^FS
                ^FO50,70^A0N,40,40^FD$itemCode^FS
                ^FO50,130^A0N,25,25^FD$itemName^FS
                ^FO50,170^A0N,20,20^FDQty: $quantity^FS
                ^FO50,200^A0N,20,20^FDLocation: $location^FS
                ^FO50,230^A0N,15,15^FD$formattedDate^FS
                ^FO400,30^BQN,2,4^FDMM,A$itemCode^FS
                ^XZ
            """.trimIndent()
        }

        /**
         * Generate ZPL content for inventory summary label
         */
        fun generateInventorySummaryLabel(
            totalItems: Int,
            totalQuantity: Int,
            location: String,
            timestamp: Date = Date(),
            dpi: Int = DPI_203
        ): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(timestamp)

            return """
                ^XA
                ^PW${DEFAULT_LABEL_WIDTH}
                ^LL${DEFAULT_LABEL_HEIGHT}
                ^FO50,30^A0N,35,35^FDInventory Summary^FS
                ^FO50,80^A0N,25,25^FDTotal Items: $totalItems^FS
                ^FO50,120^A0N,25,25^FDTotal Quantity: $totalQuantity^FS
                ^FO50,160^A0N,20,20^FDLocation: $location^FS
                ^FO50,190^A0N,15,15^FD$formattedDate^FS
                ^XZ
            """.trimIndent()
        }

        /**
         * Generate ZPL content for location label
         */
        fun generateLocationLabel(
            locationCode: String,
            locationName: String,
            description: String? = null,
            dpi: Int = DPI_203
        ): String {
            val descText = description?.let { "\n^FO50,160^A0N,20,20^FD$it^FS" } ?: ""

            return """
                ^XA
                ^PW${DEFAULT_LABEL_WIDTH}
                ^LL${DEFAULT_LABEL_HEIGHT}
                ^FO50,30^A0N,35,35^FDLocation^FS
                ^FO50,80^A0N,30,30^FD$locationCode^FS
                ^FO50,120^A0N,25,25^FD$locationName^FS$descText
                ^FO400,30^BQN,2,4^FDMM,A$locationCode^FS
                ^XZ
            """.trimIndent()
        }

        /**
         * Generate ZPL content for custom text label
         */
        fun generateTextLabel(
            title: String,
            content: String,
            subtitle: String? = null,
            dpi: Int = DPI_203
        ): String {
            val subtitleText = subtitle?.let { "\n^FO50,120^A0N,20,20^FD$it^FS" } ?: ""

            return """
                ^XA
                ^PW${DEFAULT_LABEL_WIDTH}
                ^LL${DEFAULT_LABEL_HEIGHT}
                ^FO50,30^A0N,35,35^FD$title^FS
                ^FO50,80^A0N,25,25^FD$content^FS$subtitleText
                ^XZ
            """.trimIndent()
        }

        /**
         * Generate test ZPL content to verify printer functionality
         */
        fun generateTestLabel(dpi: Int = DPI_203): String {
            return """
                ^XA
                ^PW${DEFAULT_LABEL_WIDTH}
                ^LL${DEFAULT_LABEL_HEIGHT}
                ^FO50,30^A0N,35,35^FDPrinter Test^FS
                ^FO50,80^A0N,25,25^FDHello Zebra!^FS
                ^FO50,120^A0N,20,20^FDTest successful^FS
                ^FO50,160^A0N,15,15^FD${Date()}^FS
                ^XZ
            """.trimIndent()
        }

        /**
         * Generate ZPL content with QR code for data export/import
         * QR code size is optimized based on printer configuration
         * @param qrData Data to encode in QR code (JSON string)
         * @param title Optional title to display above QR code
         * @param printer Printer configuration with label dimensions and DPI
         */
        fun generateQRCodeLabel(
            qrData: String,
            title: String = "Inventory Data",
            printer: PrinterEntity
        ): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())

            // Calculate label dimensions in dots
            val labelWidthDots = mmToDots(printer.labelWidthMm, printer.dpi)

            // QR code sizing based on available width
            // Magnification 3 produces ~4cm QR at 203 DPI
            val qrMagnification = 3
            val qrActualSize = 320   // Approximate size in dots

            // Layout positioning
            val leftMargin = 30
            val qrX = leftMargin

            var currentY = 50
            
            val zpl = StringBuilder()
            zpl.append("^XA\n")
            zpl.append("^PW$labelWidthDots\n")
            
            // Title
            zpl.append("^FO$leftMargin,$currentY^A0N,25,25^FD$title^FS\n")
            currentY += 50
            
            // QR Code
            zpl.append("^FO$qrX,$currentY^BQN,2,$qrMagnification^FDMA,$qrData^FS\n")
            currentY += qrActualSize + 200
            
            // Date label
            zpl.append("^FO$leftMargin,$currentY^A0N,20,20^FDGenerated:^FS\n")
            currentY += 50
            zpl.append("^FO$leftMargin,$currentY^A0N,20,20^FD$formattedDate^FS\n")
            currentY += 50
            
            // Calculate total height
            val totalHeight = printer.labelHeightMm?.let { mmToDots(it, printer.dpi) } ?: currentY
            zpl.append("^LL$totalHeight\n")
            
            zpl.append("^XZ")
            
            return zpl.toString()
        }

        /**
         * Legacy version for backward compatibility
         * @deprecated Use version with PrinterEntity parameter instead
         */
        @Deprecated("Use version with PrinterEntity parameter", ReplaceWith("generateQRCodeLabel(qrData, title, printer)"))
        fun generateQRCodeLabel(
            qrData: String,
            title: String = "Inventory Data",
            dpi: Int = DPI_203
        ): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())

            // Label dimensions for 48mm width paper
            val labelWidth = 384  // 48mm at 203 DPI

            // FIXED QR CODE SIZE: 4cm x 4cm
            // At 203 DPI: 4cm = 1.575 inches = 1.575 * 203 = 320 dots
            // We'll use module size and magnification to achieve this
            // ZPL ^BQ command: ^BQo,m,e where m = magnification (1-10)
            // For 4cm QR at 203 DPI, we need magnification = 8
            val qrMagnification = 3  // Fixed magnification for 4cm QR code
            val qrActualSize = 320   // 4cm at 203 DPI = 320 dots

            // Left-align the QR code with margin
            val qrX = 30  // Left margin

            // Y positions for layout
            val titleY = 50
            val qrY = 100          // After title
            val dateLabelY = qrY + qrActualSize + 200  // After QR + spacing
            val dateValueY = dateLabelY + 50

            // Calculate total height dynamically
            val totalHeight = dateValueY + 400  // Date line + bottom margin

            return """
                ^XA
                ^PW$labelWidth
                ^LL$totalHeight
                ^FO30,$titleY^A0N,25,25^FD$title^FS
                ^FO$qrX,$qrY^BQN,2,$qrMagnification^FDMA,$qrData^FS
                ^FO30,$dateLabelY^A0N,20,20^FDGenerated:^FS
                ^FO30,$dateValueY^A0N,20,20^FD$formattedDate^FS
                ^XZ
            """.trimIndent()
        }

        /**
         * Get font sizes and line heights based on printer font size setting
         * @param fontSize Font size setting ("small", "medium", "large")
         * @return Map containing font sizes and line heights
         */
        private fun getFontSizes(fontSize: String): Map<String, Any> {
            return when (fontSize.lowercase()) {
                "medium" -> mapOf(
                    "headerFont" to "40,40",    // Box name (larger)
                    "normalFont" to "32,30",    // Regular text (larger)
                    "smallFont" to "28,25",     // Product details (larger)
                    "tinyFont" to "25,20",      // Date/timestamps (larger)
                    "lineHeight" to 40,         // Base line height
                    "headerLineHeight" to 45,    // Header line height
                    "smallLineHeight" to 30      // Small text line height
                )
                "large" -> mapOf(
                    "headerFont" to "45,45",    // Box name (largest)
                    "normalFont" to "37,35",    // Regular text (largest)
                    "smallFont" to "33,30",     // Product details (largest)
                    "tinyFont" to "30,25",      // Date/timestamps (largest)
                    "lineHeight" to 45,         // Base line height
                    "headerLineHeight" to 50,    // Header line height
                    "smallLineHeight" to 35      // Small text line height
                )
                else -> mapOf( // "small" - default
                    "headerFont" to "35,35",    // Box name
                    "normalFont" to "27,25",    // Regular text
                    "smallFont" to "23,20",     // Product details
                    "tinyFont" to "20,15",      // Date/timestamps
                    "lineHeight" to 35,         // Base line height
                    "headerLineHeight" to 40,    // Header line height
                    "smallLineHeight" to 25      // Small text line height
                )
            }
        }

        /**
         * Generate ZPL content for box label with smart product list layout
         * Automatically uses horizontal layout (Name: SN, Name: SN,) if width allows,
         * otherwise falls back to vertical stacked layout.
         * Products are grouped by category with separate numbering per category.
         * For "Other" category, quantity is shown instead of serial number.
         * 
         * @param box Box entity with name, description, location
         * @param products List of products with categories in the box
         * @param printer Printer configuration with label dimensions
         * @return ZPL string ready to send to printer
         */
        fun generateBoxLabel(
            box: BoxEntity,
            products: List<ProductWithCategory>,
            printer: PrinterEntity
        ): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(box.createdAt)

            // Calculate label dimensions in dots
            val labelWidthDots = mmToDots(printer.labelWidthMm, printer.dpi)
            val labelHeightDots = printer.labelHeightMm?.let { mmToDots(it, printer.dpi) }

            // Layout constants (all in dots)
            val leftMargin = 10
            val rightMargin = 30
            val availableWidth = labelWidthDots - leftMargin - rightMargin

            // Get font sizes and line heights based on printer setting
            val fontConfig = getFontSizes(printer.fontSize)
            val headerFontSize = fontConfig["headerFont"] as String
            val normalFontSize = fontConfig["normalFont"] as String
            val smallFontSize = fontConfig["smallFont"] as String
            val tinyFontSize = fontConfig["tinyFont"] as String
            val lineHeight = fontConfig["lineHeight"] as Int
            val headerLineHeight = fontConfig["headerLineHeight"] as Int
            val smallLineHeight = fontConfig["smallLineHeight"] as Int
            
            // Calculate character width based on font size
            // ZPL font format: "width,height" - we use the width value
            val smallFontWidth = smallFontSize.split(",")[0].toInt()
            // Character width estimation: font width in dots + small spacing (~1.2x multiplier for safety)
            val charWidthDots = (smallFontWidth * 1.2).toInt()

            // Map Polish category names to English for labels
            fun getCategoryDisplayName(categoryName: String?): String {
                return when (categoryName?.lowercase()?.trim()) {
                    "skaner" -> "Scanner"
                    "drukarka" -> "Printer"
                    "stacja dokująca skanera" -> "Scanner Dock"
                    "stacja dokująca drukarki" -> "Printer Dock"
                    "scanner docking station" -> "Scanner Dock"
                    "printer docking station" -> "Printer Dock"
                    "other", "inne", "uncategorized", "" -> "Other"
                    null -> "Other"
                    else -> categoryName.trim()
                }
            }

            // Group products by category
            val productsByCategory = products.groupBy { getCategoryDisplayName(it.category?.name) }
            
            // Sort categories: put "Other" at the end, others alphabetically
            val sortedCategories = productsByCategory.keys.sortedWith(
                compareBy<String> { it.equals("Other", ignoreCase = true) }
                    .thenBy { it }
            )

            // ===== FIRST PASS: Calculate total height needed =====
            var calculatedHeight = 60  // Initial top margin
            
            // Header: Box Name
            calculatedHeight += headerLineHeight

            // Description (if present)
            if (!box.description.isNullOrBlank()) {
                calculatedHeight += lineHeight
            }

            // Location (if present)
            if (!box.warehouseLocation.isNullOrBlank()) {
                calculatedHeight += smallLineHeight
            }

            // Created date
            calculatedHeight += smallLineHeight + 10

            // Separator line
            calculatedHeight += 20

            // Products header
            calculatedHeight += lineHeight

            // Calculate height for all products grouped by category
            sortedCategories.forEach { categoryName ->
                val categoryProducts = productsByCategory[categoryName] ?: emptyList()
                
                // Category header
                calculatedHeight += lineHeight
                
                // Check if this is "Other" category
                val isOtherCategory = categoryName.equals("Other", ignoreCase = true)
                
                // Calculate height for each product in this category
                categoryProducts.forEach { productWithCategory ->
                    val name = productWithCategory.product.name
                    
                    if (isOtherCategory) {
                        val quantity = productWithCategory.product.quantity
                        val horizontalText = "${categoryProducts.indexOf(productWithCategory) + 1}. $name x$quantity, "
                        val estimatedWidth = horizontalText.length * charWidthDots

                        if (estimatedWidth <= availableWidth) {
                            calculatedHeight += smallLineHeight
                        } else {
                            calculatedHeight += smallLineHeight * 2
                        }
                    } else {
                        val sn = productWithCategory.product.serialNumber ?: "N/A"
                        val horizontalText = "${categoryProducts.indexOf(productWithCategory) + 1}. $name: $sn, "
                        val estimatedWidth = horizontalText.length * charWidthDots

                        if (estimatedWidth <= availableWidth) {
                            calculatedHeight += smallLineHeight
                        } else {
                            calculatedHeight += smallLineHeight * 2
                        }
                    }
                }
                
                // Spacing after category
                calculatedHeight += smallLineHeight
            }

            // Bottom margin
            calculatedHeight += 30

            // Determine final label height
            val finalHeight = labelHeightDots ?: calculatedHeight

            // ===== SECOND PASS: Generate ZPL with correct height =====
            val zpl = StringBuilder()
            
            // Start ZPL
            zpl.append("^XA\n")
            zpl.append("^PW$labelWidthDots\n")
            zpl.append("^LL$finalHeight\n")  // Set label length BEFORE content
            
            var currentY = 60  // Increased top margin for easier label tearing
            
            // Header: Box Name
            zpl.append("^FO$leftMargin,$currentY^A0N,$headerFontSize^FDBox: ${box.name}^FS\n")
            currentY += headerLineHeight

            // Description (if present)
            if (!box.description.isNullOrBlank()) {
                zpl.append("^FO$leftMargin,$currentY^A0N,$normalFontSize^FD${box.description}^FS\n")
                currentY += lineHeight
            }

            // Location (if present)
            if (!box.warehouseLocation.isNullOrBlank()) {
                zpl.append("^FO$leftMargin,$currentY^A0N,$smallFontSize^FDLocation: ${box.warehouseLocation}^FS\n")
                currentY += smallLineHeight
            }

            // Created date
            zpl.append("^FO$leftMargin,$currentY^A0N,$tinyFontSize^FDCreated: $formattedDate^FS\n")
            currentY += smallLineHeight + 10

            // Separator line
            zpl.append("^FO$leftMargin,$currentY^GB${availableWidth},2,2^FS\n")
            currentY += 20

            // Products header
            zpl.append("^FO$leftMargin,$currentY^A0N,$normalFontSize^FDProducts (${products.size}):^FS\n")
            currentY += lineHeight

            // Render products grouped by category
            sortedCategories.forEach { categoryName ->
                val categoryProducts = productsByCategory[categoryName] ?: emptyList()
                
                // Category header
                zpl.append("^FO$leftMargin,$currentY^A0N,$normalFontSize^FD$categoryName:^FS\n")
                currentY += lineHeight
                
                // Check if this is "Other" category (doesn't require serial number)
                val isOtherCategory = categoryName.equals("Other", ignoreCase = true)
                
                // Render each product in this category with its own numbering
                categoryProducts.forEachIndexed { index, productWithCategory ->
                    val productNumber = index + 1
                    val name = productWithCategory.product.name
                    
                    if (isOtherCategory) {
                        // For "Other" category: show quantity instead of serial number
                        val quantity = productWithCategory.product.quantity
                        val horizontalText = "$productNumber. $name x$quantity, "
                        val estimatedWidth = horizontalText.length * charWidthDots

                        if (estimatedWidth <= availableWidth) {
                            // HORIZONTAL LAYOUT: Fits in one line
                            zpl.append("^FO$leftMargin,$currentY^A0N,$smallFontSize^FD$horizontalText^FS\n")
                            currentY += smallLineHeight
                        } else {
                            // VERTICAL LAYOUT: Stack name and quantity on separate lines
                            zpl.append("^FO$leftMargin,$currentY^A0N,$smallFontSize^FD$productNumber. $name:^FS\n")
                            currentY += smallLineHeight
                            zpl.append("^FO${leftMargin + 20},$currentY^A0N,$smallFontSize^FDx$quantity^FS\n")
                            currentY += smallLineHeight
                        }
                    } else {
                        // For other categories: show serial number
                        val sn = productWithCategory.product.serialNumber ?: "N/A"
                        val horizontalText = "$productNumber. $name: $sn, "
                        val estimatedWidth = horizontalText.length * charWidthDots

                        if (estimatedWidth <= availableWidth) {
                            // HORIZONTAL LAYOUT: Fits in one line
                            zpl.append("^FO$leftMargin,$currentY^A0N,$smallFontSize^FD$horizontalText^FS\n")
                            currentY += smallLineHeight
                        } else {
                            // VERTICAL LAYOUT: Stack name and SN on separate lines
                            zpl.append("^FO$leftMargin,$currentY^A0N,$smallFontSize^FD$productNumber. $name:^FS\n")
                            currentY += smallLineHeight
                            zpl.append("^FO${leftMargin + 20},$currentY^A0N,$smallFontSize^FD$sn^FS\n")
                            currentY += smallLineHeight
                        }
                    }
                }
                
                // Add spacing after category
                currentY += smallLineHeight
            }

            // End ZPL
            zpl.append("^XZ")

            return zpl.toString()
        }

        /**
         * Calculate optimal QR code magnification to fit within available width
         * @param dataLength Length of data to encode
         * @param availableWidth Available width in dots
         * @return Optimal magnification level (1-10)
         */
        private fun calculateOptimalQRMagnification(dataLength: Int, availableWidth: Int): Int {
            // Base magnification based on data size
            // QR codes can hold roughly 100-200 chars per magnification level increase
            val baseMagnification = when {
                dataLength <= 100 -> 5    // Small data - larger QR for better readability
                dataLength <= 500 -> 4    // Medium data
                dataLength <= 1000 -> 3   // Large data
                dataLength <= 2000 -> 2   // Very large data
                else -> 1                 // Extremely large data
            }

            // Calculate approximate QR size (magnification * ~50 dots)
            val estimatedQRSize = baseMagnification * 50

            // If QR fits, use base magnification
            if (estimatedQRSize <= availableWidth) {
                return baseMagnification
            }

            // Otherwise, scale down to fit available width
            val maxMagnification = (availableWidth / 50).coerceAtLeast(1)
            return maxMagnification.coerceAtMost(baseMagnification)
        }
    }
}