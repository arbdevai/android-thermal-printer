package com.thermalprinter.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrManager(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(imageUri: Uri): Result<String> {
        return try {
            // Apply preprocessing (EXIF rotation + auto-upscale + contrast enhance)
            val enhancedBitmap = ImagePreprocessor.loadAndEnhance(context, imageUri)
            val image = InputImage.fromBitmap(enhancedBitmap, 0)
            val visionText = recognizer.process(image).await()
            Result.success(visionText.text)
        } catch (e: Exception) {
            // Fallback directly to original URI if preprocessing fails
            try {
                val fallbackImage = InputImage.fromFilePath(context, imageUri)
                val fallbackText = recognizer.process(fallbackImage).await()
                Result.success(fallbackText.text)
            } catch (fallbackEx: Exception) {
                Result.failure(fallbackEx)
            }
        }
    }

    suspend fun recognizeText(bitmap: Bitmap): Result<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            Result.success(visionText.text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        recognizer.close()
    }
}
