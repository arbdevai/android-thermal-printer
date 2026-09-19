package com.thermalprinter.app.printer

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosDriver(private val lineChars: Int = 32) {
    private val buffer = ByteArrayOutputStream()
    private val charset = Charset.forName("CP437")

    fun init(): EscPosDriver {
        buffer.write(byteArrayOf(0x1B, 0x40)) // ESC @
        return this
    }

    fun alignLeft(): EscPosDriver {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x00)) // ESC a 0
        return this
    }

    fun alignCenter(): EscPosDriver {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1
        return this
    }

    fun alignRight(): EscPosDriver {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x02)) // ESC a 2
        return this
    }

    fun bold(enable: Boolean): EscPosDriver {
        buffer.write(byteArrayOf(0x1B, 0x45, if (enable) 0x01 else 0x00)) // ESC E n
        return this
    }

    fun doubleHeight(enable: Boolean): EscPosDriver {
        buffer.write(byteArrayOf(0x1D, 0x21, if (enable) 0x01 else 0x00)) // GS ! n
        return this
    }

    fun doubleWidth(enable: Boolean): EscPosDriver {
        buffer.write(byteArrayOf(0x1D, 0x21, if (enable) 0x10 else 0x00)) // GS ! n
        return this
    }

    fun doubleSize(enable: Boolean): EscPosDriver {
        buffer.write(byteArrayOf(0x1D, 0x21, if (enable) 0x11 else 0x00)) // GS ! n
        return this
    }

    fun text(text: String): EscPosDriver {
        buffer.write(text.toByteArray(charset))
        return this
    }

    fun line(text: String = ""): EscPosDriver {
        if (text.isNotEmpty()) {
            buffer.write(text.toByteArray(charset))
        }
        buffer.write(0x0A) // LF
        return this
    }

    fun divider(char: Char = '-'): EscPosDriver {
        val line = char.toString().repeat(lineChars)
        return line(line)
    }

    fun doubleDivider(): EscPosDriver {
        return divider('=')
    }

    fun twoColumn(left: String, right: String): EscPosDriver {
        val available = lineChars - left.length - right.length
        val spaces = if (available > 0) " ".repeat(available) else " "
        return line("$left$spaces$right")
    }

    fun feed(lines: Int = 3): EscPosDriver {
        buffer.write(byteArrayOf(0x1B, 0x64, lines.toByte())) // ESC d n
        return this
    }

    fun cut(): EscPosDriver {
        buffer.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0
        return this
    }

    /**
     * Converts a bitmap to ESC/POS raster bit image (GS v 0 m xL xH yL yH d1...dk)
     * using Floyd-Steinberg dithering for crisp monochrome thermal printing.
     */
    fun image(bitmap: Bitmap, maxWidth: Int = 384): EscPosDriver {
        val scaled = if (bitmap.width > maxWidth) {
            val scale = maxWidth.toFloat() / bitmap.width
            val targetHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)
        } else {
            bitmap
        }

        val width = scaled.width
        val height = scaled.height
        val widthBytes = (width + 7) / 8

        // Floyd-Steinberg dithering to 1-bit monochrome
        val gray = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)
                gray[y][x] = if (a < 128) 255 else (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            }
        }

        val dots = Array(height) { BooleanArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldVal = gray[y][x]
                val newVal = if (oldVal < 128) 0 else 255
                dots[y][x] = (newVal == 0) // true = black dot
                val error = oldVal - newVal

                if (x + 1 < width) gray[y][x + 1] = (gray[y][x + 1] + error * 7 / 16).coerceIn(0, 255)
                if (y + 1 < height) {
                    if (x > 0) gray[y + 1][x - 1] = (gray[y + 1][x - 1] + error * 3 / 16).coerceIn(0, 255)
                    gray[y + 1][x] = (gray[y + 1][x] + error * 5 / 16).coerceIn(0, 255)
                    if (x + 1 < width) gray[y + 1][x + 1] = (gray[y + 1][x + 1] + error * 1 / 16).coerceIn(0, 255)
                }
            }
        }

        // GS v 0 0 xL xH yL yH
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (height and 0xFF).toByte()
        val yH = ((height shr 8) and 0xFF).toByte()

        buffer.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

        for (y in 0 until height) {
            for (byteX in 0 until widthBytes) {
                var byteVal = 0
                for (b in 0 until 8) {
                    val px = byteX * 8 + b
                    if (px < width && dots[y][px]) {
                        byteVal = byteVal or (1 shl (7 - b))
                    }
                }
                buffer.write(byteVal)
            }
        }

        return this
    }

    fun build(): ByteArray {
        return buffer.toByteArray()
    }
}
