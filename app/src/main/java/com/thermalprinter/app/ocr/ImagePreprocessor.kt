package com.thermalprinter.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * ImagePreprocessor handles low-resolution/compressed screenshots coming from Share Intents:
 * 1. Auto-reads EXIF rotation and un-rotates inverted/sideways images.
 * 2. Upscales low-res/compressed screenshots so OCR text detectors capture smaller fonts.
 * 3. Enhances contrast and lightens background to separate bank UI gradients from text.
 */
object ImagePreprocessor {

    suspend fun loadAndEnhance(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Read EXIF orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {}

        // 2. Decode raw bitmap
        val rawBitmap: Bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("Gagal membaca gambar dari URI")

        // 3. Apply rotation if required
        val rotatedBitmap = applyRotation(rawBitmap, orientation)

        // 4. Auto-upscale if narrow (< 1080px width)
        val scaledBitmap = autoUpscale(rotatedBitmap)

        // 5. Enhance contrast (grayscale + contrast stretch)
        enhanceForOcr(scaledBitmap)
    }

    private fun applyRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun autoUpscale(bitmap: Bitmap): Bitmap {
        val minWidth = 1080
        if (bitmap.width >= minWidth) return bitmap

        val scale = minWidth.toFloat() / bitmap.width.toFloat()
        val targetWidth = minWidth
        val targetHeight = (bitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun enhanceForOcr(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Contrast enhancement color matrix:
        // Increase contrast by 1.35x and slightly brighten by +10 to pull black/dark text out of dark backgrounds
        val contrast = 1.35f
        val brightness = 10f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return output
    }
}
