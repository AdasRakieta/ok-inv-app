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
    private const val TEXT_SIZE_MEDIUM = 32f
    private const val TEXT_SIZE_LARGE = 40f

    /**
     * Format label data combining barcode and serial number text
     * For PT-P950NW with 29mm tape:
     * - tapeWidthMm: Physical tape width (29mm)
     * - labelLengthMm: How long the label should be (default 60mm)
     * - Max printable height on 29mm tape: ~25mm
     * @param serialNumber Device serial number
     * @param barcodeBitmap Generated Code128 barcode bitmap
     * @param tapeWidthMm Tape width in mm (29mm for standard)
     * @param labelLengthMm Label length in mm (60mm default)
     * @return ByteArray of ESC/POS commands ready to send to printer
     */
    fun formatLabelData(
        serialNumber: String,
        barcodeBitmap: Bitmap,
        tapeWidthMm: Int = 29,
        labelLengthMm: Int = 60
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

        // Calculate positions - horizontal layout
        val padding = 8
        val textWidth = 120  // Space for text on right side
        val barcodeWidth = widthPx - textWidth - (padding * 3)

        // Scale barcode to fit left side
        val scaledBarcode = Bitmap.createScaledBitmap(
            barcodeBitmap,
            barcodeWidth,
            heightPx - (padding * 2),
            true
        )

        // Draw barcode on left
        canvas.drawBitmap(
            scaledBarcode,
            padding.toFloat(),
            padding.toFloat(),
            null
        )

        // Draw serial number text on right side
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = TEXT_SIZE_SMALL
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        // Draw text vertically centered on right
        val textX = (padding + barcodeWidth + padding).toFloat()
        val textY = heightPx / 2f + (TEXT_SIZE_SMALL / 3f)

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
        // Create test bitmap: 60mm length x 25mm height (for 29mm tape)
        val width = 480   // 60mm label length
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
     * - width: fits in label length (minus space for text)
     * - height: fits in tape width printable area
     */
    fun calculateBarcodeSize(tapeWidthMm: Int, labelLengthMm: Int): Pair<Int, Int> {
        // For horizontal layout: barcode on left, text on right
        val textSpaceMm = 15  // Reserve 15mm for serial number text
        val effectivePrintHeightMm = (tapeWidthMm * 0.85).toInt()  // 85% usable height
        
        val widthPx = ((labelLengthMm - textSpaceMm) * DOTS_PER_MM * 0.95).toInt()  // 95% of remaining space
        val heightPx = (effectivePrintHeightMm * DOTS_PER_MM * 0.9).toInt()  // 90% of height
        return Pair(widthPx, heightPx)
    }
}
