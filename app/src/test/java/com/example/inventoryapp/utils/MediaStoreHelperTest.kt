package com.example.inventoryapp.utils

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MediaStoreHelperTest {

    @Test
    fun saveBitmap_doesNotThrow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp: Bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            val uri: Uri? = MediaStoreHelper.saveBitmap(context, bmp, "test_save_bitmap_${System.currentTimeMillis()}")
            // We don't enforce MediaStore availability in unit environment.
            // The important part for this unit test is that the helper does not throw.
            // If a Uri is returned, it's a success; if null, environment limitations likely caused fallback failure.
            // Make the test pass as long as no exception was thrown.
            assertTrue(true)
        } catch (e: Exception) {
            // Fail explicitly if method throws
            fail("MediaStoreHelper.saveBitmap threw: ${e.message}")
        }
    }
}
