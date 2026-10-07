package com.example.scanner

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import com.example.engine.RubikColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Intelligent color detection and classification for Rubik's cube face scanning.
 * Uses HSV and normalized RGB Euclidean distance to accurately map sampled pixels
 * to the standard Rubik colors (Yellow, White, Green, Blue, Red, Orange).
 */
object RubikColorDetector {

    /**
     * Samples the 9 stickers in a 3x3 grid from a source bitmap according to the given relative bounds.
     * Sampling averages a radius of pixels to minimize noise, glare, and shadows.
     */
    fun detect3x3GridFromBitmap(
        bitmap: Bitmap
    ): List<RubikColor> {
        val width = bitmap.width
        val height = bitmap.height

        val boxDimension = min(width, height) * 0.72f
        val left = ((width - boxDimension) / 2f).toInt().coerceIn(0, width - 1)
        val top = ((height - boxDimension) / 2f).toInt().coerceIn(0, height - 1)
        val right = (left + boxDimension).toInt().coerceIn(left + 1, width)
        val bottom = (top + boxDimension).toInt().coerceIn(top + 1, height)

        val cellWidth = (right - left) / 3
        val cellHeight = (bottom - top) / 3

        val sampleRadius = (min(cellWidth, cellHeight) / 5).coerceIn(4, 32)

        val detected = mutableListOf<RubikColor>()

        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val centerX = left + col * cellWidth + cellWidth / 2
                val centerY = top + row * cellHeight + cellHeight / 2

                val color = sampleAverageColor(bitmap, centerX, centerY, sampleRadius)
                detected.add(classifyColor(color))
            }
        }

        return detected
    }

    /**
     * Calculates the average RGB color within a small circular region around (cx, cy).
     */
    fun sampleAverageColor(bitmap: Bitmap, cx: Int, cy: Int, radius: Int): Int {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        val rClamped = radius.coerceAtLeast(2)
        val startX = (cx - rClamped).coerceIn(0, bitmap.width - 1)
        val endX = (cx + rClamped).coerceIn(0, bitmap.width - 1)
        val startY = (cy - rClamped).coerceIn(0, bitmap.height - 1)
        val endY = (cy + rClamped).coerceIn(0, bitmap.height - 1)

        for (y in startY..endY) {
            for (x in startX..endX) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy <= rClamped * rClamped) {
                    val pixel = bitmap.getPixel(x, y)
                    rSum += (pixel shr 16) and 0xFF
                    gSum += (pixel shr 8) and 0xFF
                    bSum += pixel and 0xFF
                    count++
                }
            }
        }

        if (count == 0) return bitmap.getPixel(cx.coerceIn(0, bitmap.width - 1), cy.coerceIn(0, bitmap.height - 1))

        val r = (rSum / count).toInt().coerceIn(0, 255)
        val g = (gSum / count).toInt().coerceIn(0, 255)
        val b = (bSum / count).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Pure Kotlin RGB to HSV conversion (Hue: 0..360, Saturation: 0..1, Value: 0..1).
     */
    fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val v = max
        val s = if (max == 0f) 0f else delta / max
        val h = when {
            delta == 0f -> 0f
            max == rf -> ((gf - bf) / delta) % 6f * 60f
            max == gf -> ((bf - rf) / delta + 2f) * 60f
            else -> ((rf - gf) / delta + 4f) * 60f
        }.let { if (it < 0f) it + 360f else it }

        return floatArrayOf(h, s, v)
    }

    /**
     * Classifies a 32-bit ARGB color into one of the 6 Rubik colors.
     * Uses HSV color space thresholds followed by distance matching.
     */
    fun classifyColor(pixel: Int): RubikColor {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        val hsv = rgbToHsv(r, g, b)
        val hue = hsv[0]        // 0..360
        val saturation = hsv[1] // 0..1
        val value = hsv[2]      // 0..1

        // 1. Check for White: Low saturation or very high brightness with near equal RGB
        if (saturation < 0.22f && value > 0.40f) {
            return RubikColor.WHITE
        }

        // Check if RGB components are very balanced (low color difference)
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        if ((maxC - minC) < 28 && value > 0.45f) {
            return RubikColor.WHITE
        }

        // 2. Classify by Hue
        return when {
            // Orange vs Red boundary (Hue 0..20 is Red, 20..45 is Orange, 340..360 is Red)
            hue >= 345f || hue < 15f -> RubikColor.RED
            hue in 15f..46f -> {
                if (hue > 36f && saturation > 0.35f && g > 150) RubikColor.YELLOW
                else RubikColor.ORANGE
            }
            hue in 47f..75f -> RubikColor.YELLOW
            hue in 76f..170f -> RubikColor.GREEN
            hue in 171f..265f -> RubikColor.BLUE
            hue in 266f..344f -> RubikColor.RED
            else -> getNearestByRgbDistance(r, g, b)
        }
    }

    /**
     * Fallback Euclidean RGB distance matcher.
     */
    private fun getNearestByRgbDistance(r: Int, g: Int, b: Int): RubikColor {
        val targets = listOf(
            RubikColor.YELLOW to Triple(250, 204, 21),
            RubikColor.WHITE to Triple(245, 245, 245),
            RubikColor.GREEN to Triple(34, 197, 94),
            RubikColor.BLUE to Triple(59, 130, 246),
            RubikColor.RED to Triple(239, 68, 68),
            RubikColor.ORANGE to Triple(249, 115, 22)
        )

        return targets.minByOrNull { (_, rgb) ->
            val dr = (r - rgb.first).toDouble()
            val dg = (g - rgb.second).toDouble()
            val db = (b - rgb.third).toDouble()
            sqrt(dr * dr + dg * dg + db * db)
        }?.first ?: RubikColor.WHITE
    }
}
