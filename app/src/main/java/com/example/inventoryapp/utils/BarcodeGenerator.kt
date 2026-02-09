package com.example.inventoryapp.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Utility class for generating various types of barcodes
 * Supports QR codes, Code128, and other barcode formats using ZXing library
 */
object BarcodeGenerator {

    /**
     * Generate QR code bitmap
     * @param content Text to encode
     * @param width Bitmap width in pixels
     * @param height Bitmap height in pixels
     * @return Bitmap containing QR code or null if generation fails
     */
    fun generateQRCode(
        content: String,
        width: Int = 300,
        height: Int = 300
    ): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }

            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            bitMatrixToBitmap(bitMatrix)
        } catch (e: WriterException) {
            android.util.Log.e("BarcodeGenerator", "Error generating QR code", e)
            null
        }
    }

    /**
     * Generate Code128 barcode bitmap
     * Code128 is ideal for alphanumeric serial numbers
     * @param content Text to encode (alphanumeric)
     * @param width Bitmap width in pixels
     * @param height Bitmap height in pixels
     * @return Bitmap containing Code128 barcode or null if generation fails
     */
    fun generateCode128(
        content: String,
        width: Int = 400,
        height: Int = 150
    ): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.MARGIN, 10)
            }

            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.CODE_128,
                width,
                height,
                hints
            )

            bitMatrixToBitmap(bitMatrix)
        } catch (e: WriterException) {
            android.util.Log.e("BarcodeGenerator", "Error generating Code128", e)
            null
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("BarcodeGenerator", "Invalid content for Code128: $content", e)
            null
        }
    }

    /**
     * Generate EAN-13 barcode bitmap
     * Requires exactly 13 numeric digits
     * @param content 13-digit numeric string
     * @param width Bitmap width in pixels
     * @param height Bitmap height in pixels
     * @return Bitmap containing EAN-13 barcode or null if generation fails
     */
    fun generateEAN13(
        content: String,
        width: Int = 350,
        height: Int = 150
    ): Bitmap? {
        if (content.length != 13 || !content.all { it.isDigit() }) {
            android.util.Log.e("BarcodeGenerator", "EAN-13 requires exactly 13 digits")
            return null
        }

        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.MARGIN, 10)
            }

            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.EAN_13,
                width,
                height,
                hints
            )

            bitMatrixToBitmap(bitMatrix)
        } catch (e: WriterException) {
            android.util.Log.e("BarcodeGenerator", "Error generating EAN-13", e)
            null
        }
    }

    /**
     * Generate barcode of specified format
     * @param content Text to encode
     * @param format BarcodeFormat type
     * @param width Bitmap width in pixels
     * @param height Bitmap height in pixels
     * @return Bitmap containing barcode or null if generation fails
     */
    fun generateBarcode(
        content: String,
        format: BarcodeFormat,
        width: Int = 400,
        height: Int = 150
    ): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.MARGIN, 10)
                if (format == BarcodeFormat.QR_CODE) {
                    put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                    put(EncodeHintType.CHARACTER_SET, "UTF-8")
                }
            }

            val bitMatrix = MultiFormatWriter().encode(
                content,
                format,
                width,
                height,
                hints
            )

            bitMatrixToBitmap(bitMatrix)
        } catch (e: Exception) {
            android.util.Log.e("BarcodeGenerator", "Error generating barcode: ${format.name}", e)
            null
        }
    }

    /**
     * Convert ZXing BitMatrix to Android Bitmap
     * @param bitMatrix BitMatrix from ZXing encoder
     * @return Bitmap representation of the barcode
     */
    private fun bitMatrixToBitmap(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }

        return bitmap
    }

    /**
     * Validate content for Code128
     * Code128 supports ASCII 0-127
     */
    fun isValidCode128Content(content: String): Boolean {
        return content.all { it.code in 0..127 }
    }

    /**
     * Validate content for EAN-13
     */
    fun isValidEAN13Content(content: String): Boolean {
        return content.length == 13 && content.all { it.isDigit() }
    }
}
