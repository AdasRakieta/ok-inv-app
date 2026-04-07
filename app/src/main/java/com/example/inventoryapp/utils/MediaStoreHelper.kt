package com.example.inventoryapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

object MediaStoreHelper {

    /**
     * Save bitmap to MediaStore (preferred) and return Uri. If MediaStore is not available
     * or operation fails, fallback to writing file via FileHelper and return file Uri.
     */
    fun saveBitmap(context: Context, bitmap: Bitmap, displayName: String): Uri? {
        try {
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(collection, values)
            if (uri != null) {
                resolver.openOutputStream(uri).use { out ->
                    out?.let { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                return uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: write to external file using FileHelper
        return try {
            val exportsDir: File = FileHelper.getExportsDirectory()
            val file = File(exportsDir, "$displayName.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
