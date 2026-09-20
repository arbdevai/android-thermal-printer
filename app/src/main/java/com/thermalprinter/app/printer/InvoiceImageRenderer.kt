package com.thermalprinter.app.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.thermalprinter.app.domain.model.CustomInvoice
import com.thermalprinter.app.domain.model.StoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Off-thread PNG image renderer for invoices using Android Canvas.
 * Generates an image representing the 58mm thermal paper output from canonical document lines.
 */
object InvoiceImageRenderer {

    private const val BITMAP_WIDTH = 480
    private const val MAX_HEIGHT_PIXELS = 12000
    private const val MAX_TOTAL_PIXELS = BITMAP_WIDTH * MAX_HEIGHT_PIXELS

    private const val FONT_SIZE = 20f
    private const val LINE_HEIGHT = 28f
    private const val PADDING_HORIZONTAL = 24f
    private const val PADDING_VERTICAL = 32f

    /**
     * Renders [invoice] and [settings] as a PNG file in the app's cache invoices subdirectory.
     * Executes on [Dispatchers.IO] to avoid any UI thread bitmap overhead.
     *
     * @throws IllegalStateException if the invoice dimensions exceed safety limits.
     */
    suspend fun writePng(
        context: Context,
        invoice: CustomInvoice,
        settings: StoreSettings
    ): File = withContext(Dispatchers.IO) {
        val lines = InvoiceDocument.lines(invoice, settings)

        // Decode logo if enabled in settings
        val logoBitmap: Bitmap? = if (settings.showLogo && settings.logoPath.isNotBlank()) {
            decodeLogo(settings.logoPath, maxWidth = 240)
        } else {
            null
        }

        val logoHeight = if (logoBitmap != null) logoBitmap.height + 16f else 0f
        val textTotalHeight = lines.size * LINE_HEIGHT
        val totalHeight = (PADDING_VERTICAL * 2 + logoHeight + textTotalHeight).toInt()

        if (totalHeight > MAX_HEIGHT_PIXELS || (BITMAP_WIDTH * totalHeight) > MAX_TOTAL_PIXELS) {
            logoBitmap?.recycle()
            throw IllegalStateException(
                "Invoice is too large to render as image (height: $totalHeight px exceeds limit: $MAX_HEIGHT_PIXELS px)"
            )
        }

        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Solid thermal paper background
        canvas.drawColor(Color.WHITE)

        var currentY = PADDING_VERTICAL

        // Draw optional centered logo
        if (logoBitmap != null) {
            val logoLeft = (BITMAP_WIDTH - logoBitmap.width) / 2f
            canvas.drawBitmap(logoBitmap, logoLeft, currentY, null)
            currentY += logoBitmap.height + 16f
            logoBitmap.recycle()
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = FONT_SIZE
            typeface = Typeface.MONOSPACE
        }

        // Measure monospaced character width to center 32-column text block on canvas
        val charWidth = textPaint.measureText("A")
        val contentWidth = charWidth * InvoiceDocument.LINE_WIDTH
        val leftMargin = ((BITMAP_WIDTH - contentWidth) / 2f).coerceAtLeast(PADDING_HORIZONTAL)

        val fontMetrics = textPaint.fontMetrics
        val textBaselineOffset = -fontMetrics.ascent

        for (line in lines) {
            canvas.drawText(line, leftMargin, currentY + textBaselineOffset, textPaint)
            currentY += LINE_HEIGHT
        }

        val invoiceDir = File(context.cacheDir, "invoices").apply {
            if (!exists()) mkdirs()
        }

        val sanitizedNumber = invoice.invoiceNumber
            .ifBlank { "INV-${invoice.createdAt.toString().takeLast(6)}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val uniqueFile = File(
            invoiceDir,
            "invoice_${sanitizedNumber}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
        )

        FileOutputStream(uniqueFile).use { outStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
        }

        bitmap.recycle()

        uniqueFile
    }

    private fun decodeLogo(path: String, maxWidth: Int): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            var sampleSize = 1
            while (boundsOptions.outWidth / (sampleSize * 2) >= maxWidth) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val original = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            if (original.width > maxWidth) {
                val scale = maxWidth.toFloat() / original.width
                val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(original, maxWidth, targetHeight, true)
                if (scaled != original) original.recycle()
                scaled
            } else {
                original
            }
        } catch (_: Exception) {
            null
        }
    }
}
