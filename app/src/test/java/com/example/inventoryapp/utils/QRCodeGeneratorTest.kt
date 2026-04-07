package com.example.inventoryapp.utils

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class QRCodeGeneratorTest {

    @Test
    fun generateFromString_returnsBitmap() {
        val payload = "invapp://location/test-uid"
        val bmp: Bitmap? = QRCodeGenerator.generateFromString(payload, 256, 256)
        assertNotNull("Expected QR code bitmap not null", bmp)
        assertTrue("Bitmap should have positive width", bmp!!.width > 0)
        assertTrue("Bitmap should have positive height", bmp.height > 0)
    }
}
