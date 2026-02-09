package com.example.inventoryapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

/**
 * Formatter for creating label data for Brother PT-P950NW printer
 * Combines barcode bitmap with text labels and formats for ESC/POS printing
 */
object BrotherLabelFormatter {

    private const val DOTS_PER_MM = 8  // 203 DPI ≈ 8 dots/mm
    private const val TEXT_SIZE_SMALL = 24f
    private const val TEXT_SIZE_MEDIUM = 27f  // Reduced from 32f to prevent text cutoff
    private const val TEXT_SIZE_LARGE = 40f
    
    /**
     * Get scale factor based on label length
     * - Small (40mm): 50% scale
     * - Medium (50mm): 75% scale  
     * - Large (70mm): 100% scale
     */
    private fun getScaleFactor(labelLengthMm: Int): Float {
        return when (labelLengthMm) {
            40 -> 0.5f   // Mała - 50%
            50 -> 0.75f  // Średnia - 75%
            70 -> 1.0f   // Duża - 100%
            else -> when {
                labelLengthMm < 45 -> 0.5f
                labelLengthMm < 60 -> 0.75f
                else -> 1.0f
            }
        }
    }

    /**
     * Format label data combining barcode and serial number text
     * For PT-P950NW with 29mm tape:
     * - tapeWidthMm: Physical tape width (29mm)
     * - labelLengthMm: How long the label should be (default 50mm = 5cm)
     * - Max printable height on 29mm tape: ~25mm
     * @param serialNumber Device serial number
     * @param barcodeBitmap Generated Code128 barcode bitmap
     * @param tapeWidthMm Tape width in mm (29mm for standard)
     * @param labelLengthMm Label length in mm (50mm = 5cm default)
     * @return ByteArray of ESC/P commands ready to send to printer
     */
    fun formatLabelData(
        serialNumber: String,
        barcodeBitmap: Bitmap,
        tapeWidthMm: Int = 29,
        labelLengthMm: Int = 50
    ): ByteArray {
        // Create composite bitmap with barcode and text
        // For tape printer: bitmap width = label length, height = tape width
        val compositeBitmap = createCompositeLabelBitmap(
            serialNumber = serialNumber,
            barcodeBitmap = barcodeBitmap,
            tapeWidthMm = tapeWidthMm,
            labelLengthMm = labelLengthMm
        )

        // Convert to ESC/POS raster format
        return bitmapToEscPosRaster(compositeBitmap)
    }

    /**
     * Create a composite bitmap with barcode and serial number text
     * VERTICAL LAYOUT: Barcode on top, text underneath
     * Scales barcode and text size based on label length:
     * - Small (40mm): 50% scale
     * - Medium (50mm): 75% scale
     * - Large (70mm): 100% scale
     * For tape printer orientation:
     * - Bitmap WIDTH = label length (how long the label is)
     * - Bitmap HEIGHT = tape width (max printable area ~25mm for 29mm tape)
     */
    private fun createCompositeLabelBitmap(
        serialNumber: String,
        barcodeBitmap: Bitmap,
        tapeWidthMm: Int,
        labelLengthMm: Int
    ): Bitmap {
        // For 29mm tape, max printable height is ~25mm (with margins)
        val effectivePrintHeightMm = (tapeWidthMm * 0.85).toInt()  // 85% for margins
        
        // Bitmap dimensions: width=label_length, height=tape_width
        val widthPx = labelLengthMm * DOTS_PER_MM   // Label length
        val heightPx = effectivePrintHeightMm * DOTS_PER_MM  // Printable height

        // Create white background bitmap
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // VERTICAL LAYOUT: barcode on top, text below
        // Reduced padding for less white space between cuts
        val padding = 1  // Minimal padding for all sizes
        
        // Text area - enough space to prevent bottom clipping
        val textHeight = when (labelLengthMm) {
            40 -> 28   // Mała - zwiększone dla pełnego tekstu
            50 -> 33   // Średnia
            else -> 40 // Duża
        }
        
        val barcodeHeight = heightPx - textHeight - (padding * 2)
        
        // Center barcode vertically with slight upward shift for better alignment
        val barcodeTopOffset = padding

        // Scale barcode to fit area
        val scaledBarcode = Bitmap.createScaledBitmap(
            barcodeBitmap,
            widthPx - (padding * 2),
            barcodeHeight,
            true
        )

        // Draw barcode (centered horizontally, aligned to top with minimal padding)
        canvas.drawBitmap(
            scaledBarcode,
            padding.toFloat(),
            barcodeTopOffset.toFloat(),
            null
        )

        // Text size - larger relative to barcode for small labels
        val textSize = when (labelLengthMm) {
            40 -> TEXT_SIZE_MEDIUM * 0.7f   // Mała: 70% zamiast 50% - większy tekst
            50 -> TEXT_SIZE_MEDIUM * 0.85f  // Średnia: 85% zamiast 75%
            else -> TEXT_SIZE_MEDIUM        // Duża: 100%
        }
        
        // Draw serial number text BELOW barcode (centered)
        val textPaint = Paint().apply {
            color = Color.BLACK
            this.textSize = textSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Center text horizontally and place at bottom (closer to barcode)
        val textX = widthPx / 2f
        // Draw text with more margin - text baseline vs top issue
        val textY = padding + barcodeHeight + padding + (textSize * 0.95f)

        canvas.drawText(serialNumber, textX, textY, textPaint)

        return bitmap
    }

    /**
     * Convert bitmap to Brother ESC/P mode commands (DEFAULT MODE for PT-P950NW)
     * Simple bitmap printing compatible with factory default settings
     */
    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()

        // Initialize printer - ESC @
        outputStream.write(byteArrayOf(0x1B, 0x40))

        // Select print density (optional) - ESC c 0 n
        // n = 0-4 (0=lightest, 4=darkest), use 3 for good quality
        outputStream.write(byteArrayOf(0x1B, 0x63, 0x30, 0x03))

        val width = bitmap.width
        val height = bitmap.height
        
        // Print bitmap using ESC/P bit image mode
        // ESC * m nL nH [data...]
        // m = mode (33 = 24-pin high density)
        
        // Process in groups of 24 rows (3 bytes per column)
        var y = 0
        while (y < height) {
            val rowsInBand = minOf(24, height - y)
            
            // ESC * 33 - Select 24-pin graphics mode
            outputStream.write(byteArrayOf(0x1B, 0x2A, 33))
            
            // Number of columns (nL, nH) - little endian
            val nL = (width and 0xFF).toByte()
            val nH = ((width shr 8) and 0xFF).toByte()
            outputStream.write(byteArrayOf(nL, nH))
            
            // Send graphics data column by column
            for (x in 0 until width) {
                // Each column is 3 bytes (24 bits)
                val columnBytes = ByteArray(3)
                
                for (bit in 0 until rowsInBand) {
                    val pixelY = y + bit
                    if (pixelY < height) {
                        val pixel = bitmap.getPixel(x, pixelY)
                        val isBlack = isPixelBlack(pixel)
                        
                        if (isBlack) {
                            val byteIndex = bit / 8
                            val bitIndex = 7 - (bit % 8)
                            columnBytes[byteIndex] = (columnBytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                        }
                    }
                }
                
                outputStream.write(columnBytes)
            }
            
            // Line feed after each band - ESC J n (n = dots to feed)
            outputStream.write(byteArrayOf(0x1B, 0x4A, 24))
            
            y += 24
        }
        
        // Form feed to eject label - FF
        outputStream.write(byteArrayOf(0x0C))

        return outputStream.toByteArray()
    }

    /**
     * Convert a single bitmap line to byte array for ESC/POS
     */
    private fun bitmapLineToBytes(bitmap: Bitmap, y: Int): ByteArray {
        val width = bitmap.width
        val bytesPerLine = (width + 7) / 8  // Round up to nearest byte
        val lineBytes = ByteArray(bytesPerLine)

        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, y)
            
            // Convert to monochrome (black = 1, white = 0)
            val isBlack = isPixelBlack(pixel)
            
            if (isBlack) {
                val byteIndex = x / 8
                val bitIndex = 7 - (x % 8)
                lineBytes[byteIndex] = (lineBytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
            }
        }

        return lineBytes
    }

    /**
     * Determine if pixel should be considered black
     * Uses luminance threshold
     */
    private fun isPixelBlack(pixel: Int): Boolean {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        
        // Calculate luminance
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
        
        return luminance < 128  // Threshold at 50% gray
    }

    /**
     * Create test label with simple text (for testing Brother printer connection)
     * Uses ESC/P mode compatible with PT-P950NW factory defaults
     */
    fun createTestLabel(message: String = "Test Print OK"): ByteArray {
        // Create test bitmap: 40mm length x 25mm height (for 29mm tape)
        val width = 320   // 40mm label length (shorter test)
        val height = 200  // 25mm printable height
        val testBitmap = createTestBitmap(message, width, height)

        // Use same ESC/P formatting as main label printing
        return bitmapToEscPosRaster(testBitmap)
    }

    /**
     * Create a simple test bitmap with text
     */
    private fun createTestBitmap(message: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Draw message centered
        canvas.drawText(message, width / 2f, height / 2f + 12f, paint)

        return bitmap
    }

    /**
     * Calculate optimal barcode size based on label dimensions
     * For tape printer: returns (width, height) for barcode bitmap
     * VERTICAL LAYOUT: barcode spans full width, text below
     * Scales barcode size based on label length:
     * - Small (40mm): 50% scale
     * - Medium (50mm): 75% scale
     * - Large (70mm): 100% scale
     * - width: full label length minus margins
     * - height: fits in tape width printable area (minus space for text)
     */
    fun calculateBarcodeSize(tapeWidthMm: Int, labelLengthMm: Int): Pair<Int, Int> {
        // Get scale factor for this label size
        val scale = getScaleFactor(labelLengthMm)
        
        // For vertical layout: barcode on top, text below
        val textSpaceMm = (5 * scale).toInt().coerceAtLeast(3)  // Scaled text space
        val effectivePrintHeightMm = (tapeWidthMm * 0.85).toInt()  // 85% usable height
        
        // Calculate base size and apply scale
        val baseWidthPx = (labelLengthMm * DOTS_PER_MM * 0.98).toInt()
        val baseHeightPx = ((effectivePrintHeightMm - textSpaceMm) * DOTS_PER_MM * 0.95).toInt()
        
        // Apply scale to barcode dimensions (larger labels get bigger barcodes)
        val widthPx = (baseWidthPx * (0.5f + scale * 0.5f)).toInt()  // 50%-100% of base
        val heightPx = (baseHeightPx * (0.5f + scale * 0.5f)).toInt()
        
        return Pair(widthPx, heightPx)
    }
}
