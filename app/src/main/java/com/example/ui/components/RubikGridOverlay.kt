package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.example.engine.RubikColor
import com.example.scanner.CubeFace
import kotlin.math.min

/**
 * High-tech 3x3 Rubik's Cube scanning frame watermark overlay.
 * Draws an interactive 9-block grid with corner guides, central alignment target,
 * and animated scanline glow.
 */
@Composable
fun RubikGridOverlay(
    currentFace: CubeFace,
    previewColors: List<RubikColor>?,
    modifier: Modifier = Modifier,
    isScanningActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanline"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate a responsive square bounding box for the 3x3 Rubik face
        val boxSize = min(canvasWidth * 0.78f, canvasHeight * 0.48f)
        val boxLeft = (canvasWidth - boxSize) / 2f
        val boxTop = (canvasHeight - boxSize) / 2f - (canvasHeight * 0.15f)
        val boxRight = boxLeft + boxSize
        val boxBottom = boxTop + boxSize

        // 1. Draw darkened backdrop outside the frame
        val fullPath = Path().apply {
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
        }
        val cutoutPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(boxLeft, boxTop, boxRight, boxBottom),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            )
        }

        // Clip out the target box and darken the surround
        drawPath(
            path = fullPath,
            color = Color.Black.copy(alpha = 0.65f)
        )

        // Clear the inside of the square cutout using BlendMode.Clear
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // 2. Draw Outer Glowing Frame Border
        val primaryAccent = currentFace.centerColor.color
        drawRoundRect(
            color = primaryAccent.copy(alpha = glowAlpha),
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // 3. Draw Corner Brackets (HUD style)
        val bracketLen = boxSize * 0.12f
        val bracketStroke = 4.5.dp.toPx()
        val cornerColor = Color(0xFF38BDF8) // High-tech Cyan

        // Top-Left
        drawLine(cornerColor, Offset(boxLeft - 4, boxTop + bracketLen), Offset(boxLeft - 4, boxTop - 4), bracketStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(boxLeft - 4, boxTop - 4), Offset(boxLeft + bracketLen, boxTop - 4), bracketStroke, StrokeCap.Round)
        // Top-Right
        drawLine(cornerColor, Offset(boxRight - bracketLen, boxTop - 4), Offset(boxRight + 4, boxTop - 4), bracketStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(boxRight + 4, boxTop - 4), Offset(boxRight + 4, boxTop + bracketLen), bracketStroke, StrokeCap.Round)
        // Bottom-Left
        drawLine(cornerColor, Offset(boxLeft - 4, boxBottom - bracketLen), Offset(boxLeft - 4, boxBottom + 4), bracketStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(boxLeft - 4, boxBottom + 4), Offset(boxLeft + bracketLen, boxBottom + 4), bracketStroke, StrokeCap.Round)
        // Bottom-Right
        drawLine(cornerColor, Offset(boxRight - bracketLen, boxBottom + 4), Offset(boxRight + 4, boxBottom + 4), bracketStroke, StrokeCap.Round)
        drawLine(cornerColor, Offset(boxRight + 4, boxBottom + 4), Offset(boxRight + 4, boxBottom - bracketLen), bracketStroke, StrokeCap.Round)

        // 4. Draw 3x3 Grid of 9 Blocks (Stickers)
        val cellSize = boxSize / 3f
        val padding = 6.dp.toPx()
        val stickerRadius = 12.dp.toPx()

        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val index = row * 3 + col
                val cellLeft = boxLeft + col * cellSize + padding
                val cellTop = boxTop + row * cellSize + padding
                val stickerWidth = cellSize - padding * 2
                val stickerHeight = cellSize - padding * 2

                val isCenter = row == 1 && col == 1

                // Draw cell boundary
                val cellColor = if (isCenter) primaryAccent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.45f)
                drawRoundRect(
                    color = cellColor,
                    topLeft = Offset(cellLeft, cellTop),
                    size = Size(stickerWidth, stickerHeight),
                    cornerRadius = CornerRadius(stickerRadius, stickerRadius),
                    style = Stroke(width = if (isCenter) 2.5.dp.toPx() else 1.5.dp.toPx())
                )

                // If preview colors provided, show subtle tinted dot at center
                val stickerColor = previewColors?.getOrNull(index)?.color
                if (stickerColor != null) {
                    drawCircle(
                        color = stickerColor.copy(alpha = 0.75f),
                        radius = 8.dp.toPx(),
                        center = Offset(cellLeft + stickerWidth / 2f, cellTop + stickerHeight / 2f)
                    )
                } else if (isCenter) {
                    // Center guide dot
                    drawCircle(
                        color = primaryAccent,
                        radius = 7.dp.toPx(),
                        center = Offset(cellLeft + stickerWidth / 2f, cellTop + stickerHeight / 2f)
                    )
                }
            }
        }

        // 5. Draw Animated Scanline (Laser effect)
        if (isScanningActive) {
            val scanY = boxTop + (boxSize * scanLineProgress)
            val laserBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF38BDF8).copy(alpha = 0.85f),
                    Color.White,
                    Color(0xFF38BDF8).copy(alpha = 0.85f),
                    Color.Transparent
                ),
                startX = boxLeft,
                endX = boxRight
            )

            drawLine(
                brush = laserBrush,
                start = Offset(boxLeft + 8.dp.toPx(), scanY),
                end = Offset(boxRight - 8.dp.toPx(), scanY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
