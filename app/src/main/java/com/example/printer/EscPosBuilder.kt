package com.example.printer

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosBuilder(private val paperSize: String = "58mm") {

    val maxColumns: Int = when (paperSize) {
        "80mm" -> 48
        "102mm" -> 64
        else -> 32 // 58mm default
    }

    private val stream = ByteArrayOutputStream()
    private val charset = Charset.forName("CP437")

    fun init(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x40)) // ESC @
        return this
    }

    fun alignLeft(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x61, 0x00)) // ESC a 0
        return this
    }

    fun alignCenter(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1
        return this
    }

    fun alignRight(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x61, 0x02)) // ESC a 2
        return this
    }

    fun bold(enable: Boolean): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x45, if (enable) 0x01 else 0x00)) // ESC E n
        return this
    }

    fun doubleSize(enable: Boolean): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x21, if (enable) 0x30 else 0x00)) // ESC ! n
        return this
    }

    fun text(text: String): EscPosBuilder {
        stream.write(text.toByteArray(charset))
        return this
    }

    fun textLine(line: String): EscPosBuilder {
        text(line)
        stream.write(byteArrayOf(0x0A)) // LF
        return this
    }

    fun lineFeed(lines: Int = 1): EscPosBuilder {
        repeat(lines) {
            stream.write(byteArrayOf(0x0A))
        }
        return this
    }

    fun divider(char: Char = '-'): EscPosBuilder {
        val line = char.toString().repeat(maxColumns)
        textLine(line)
        return this
    }

    fun doubleRow(left: String, right: String): EscPosBuilder {
        val spacesNeeded = maxColumns - left.length - right.length
        val row = if (spacesNeeded > 0) {
            left + " ".repeat(spacesNeeded) + right
        } else {
            // If text too long, truncate left
            val maxLeft = maxColumns - right.length - 1
            val safeLeft = if (maxLeft > 0) left.take(maxLeft) else left
            val sp = (maxColumns - safeLeft.length - right.length).coerceAtLeast(1)
            safeLeft + " ".repeat(sp) + right
        }
        textLine(row)
        return this
    }

    fun itemRow(name: String, qtyPrice: String, total: String): EscPosBuilder {
        textLine(name)
        doubleRow("  $qtyPrice", total)
        return this
    }

    fun qrCode(qrData: String, sizeMultiplier: Int = 6): EscPosBuilder {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 200, 200)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            rasterBitmap(bitmap)
        } catch (_: Exception) {
            textLine("[QR: $qrData]")
        }
        return this
    }

    fun rasterBitmap(bitmap: Bitmap): EscPosBuilder {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        // GS v 0 0 xL xH yL yH d1...dk
        val xL = (widthBytes and 0xFF).toByte()
        val xH = ((widthBytes shr 8) and 0xFF).toByte()
        val yL = (height and 0xFF).toByte()
        val yH = ((height shr 8) and 0xFF).toByte()

        stream.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

        for (y in 0 until height) {
            for (xByte in 0 until widthBytes) {
                var byteVal = 0
                for (bit in 0..7) {
                    val x = xByte * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        if (luminance < 128) {
                            byteVal = byteVal or (1 shl (7 - bit))
                        }
                    }
                }
                stream.write(byteVal)
            }
        }
        return this
    }

    fun cutPaper(): EscPosBuilder {
        lineFeed(4)
        stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0 (Cut paper)
        return this
    }

    fun build(): ByteArray = stream.toByteArray()
}
