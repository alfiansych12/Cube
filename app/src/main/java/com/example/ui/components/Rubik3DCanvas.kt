package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.engine.Cubie
import com.example.engine.RubikColor
import com.example.engine.TurnAxis
import com.example.engine.TurnSpec
import com.example.engine.Vector3D
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

data class RenderPolygon(
    val points: List<Offset>,
    val depth: Float,
    val color: Color,
    val isSticker: Boolean
)

@Composable
fun Rubik3DCanvas(
    cubies: List<Cubie>,
    modifier: Modifier = Modifier,
    activeTurn: TurnSpec? = null,
    turnProgress: Float = 0f, // 0f to 1f
    initialRotX: Float = 0.42f, // ~24 deg
    initialRotY: Float = -0.58f, // ~-33 deg
    allowDragRotation: Boolean = true,
    scaleFactor: Float = 1.0f
) {
    var rotX by remember { mutableFloatStateOf(initialRotX) }
    var rotY by remember { mutableFloatStateOf(initialRotY) }

    Box(
        modifier = modifier.then(
            if (allowDragRotation) {
                Modifier.pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging adjusts orbital rotation
                        rotY += dragAmount.x * 0.012f
                        rotX = (rotX + dragAmount.y * 0.012f).coerceIn(-1.4f, 1.4f)
                    }
                }
            } else Modifier
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // Dynamic scale based on canvas size
            val baseScale = min(canvasWidth, canvasHeight) * 0.28f * scaleFactor
            val cameraDist = 6.0f
            val lightDir = Vector3D(0.4f, 0.8f, 0.6f).normalized()

            val polygons = mutableListOf<RenderPolygon>()

            // Pre-calculate partial rotation for animating cubies
            val currentTurnAngle = (activeTurn?.angleRadians ?: 0f) * turnProgress

            for (cubie in cubies) {
                val isTurned = activeTurn != null && activeTurn.sliceFilter(cubie.position)

                var cPos = cubie.position
                if (isTurned && activeTurn != null) {
                    cPos = when (activeTurn.axis) {
                        TurnAxis.X -> cPos.rotateX(currentTurnAngle)
                        TurnAxis.Y -> cPos.rotateY(currentTurnAngle)
                        TurnAxis.Z -> cPos.rotateZ(currentTurnAngle)
                    }
                }

                // Render plastic base for cubie
                // Render visible stickers
                for (sticker in cubie.stickers) {
                    var sNormal = sticker.currentNormal
                    if (isTurned && activeTurn != null) {
                        sNormal = when (activeTurn.axis) {
                            TurnAxis.X -> sNormal.rotateX(currentTurnAngle)
                            TurnAxis.Y -> sNormal.rotateY(currentTurnAngle)
                            TurnAxis.Z -> sNormal.rotateZ(currentTurnAngle)
                        }
                    }

                    // Check if face is facing camera (smooth culling without edge popping)
                    val worldNormal = sNormal.rotateY(rotY).rotateX(rotX)
                    if (worldNormal.z <= -0.02f) {
                        // Backface culled
                        continue
                    }

                    // Compute tangent vectors for this sticker face
                    val (tanU, tanV) = getFaceTangents(sNormal)
                    val sCenter = cPos + sNormal * 0.5f

                    // 1. Draw black cubie face background (size 0.49f)
                    val bgCornerOffsets = listOf(
                        sCenter + tanU * -0.485f + tanV * -0.485f,
                        sCenter + tanU * 0.485f + tanV * -0.485f,
                        sCenter + tanU * 0.485f + tanV * 0.485f,
                        sCenter + tanU * -0.485f + tanV * 0.485f
                    )
                    val bgProj = projectQuad(bgCornerOffsets, rotX, rotY, centerX, centerY, baseScale, cameraDist)
                    if (bgProj != null) {
                        val avgDepth = bgProj.second
                        polygons.add(
                            RenderPolygon(
                                points = bgProj.first,
                                depth = avgDepth - 0.005f,
                                color = Color(0xFF1E2229),
                                isSticker = false
                            )
                        )
                    }

                    // 2. Draw colored sticker (size 0.42f)
                    val stickerCorners = listOf(
                        sCenter + tanU * -0.415f + tanV * -0.415f,
                        sCenter + tanU * 0.415f + tanV * -0.415f,
                        sCenter + tanU * 0.415f + tanV * 0.415f,
                        sCenter + tanU * -0.415f + tanV * 0.415f
                    )
                    val stProj = projectQuad(stickerCorners, rotX, rotY, centerX, centerY, baseScale, cameraDist)
                    if (stProj != null) {
                        // Lighting calculation
                        val diffuse = max(0f, sNormal.dot(lightDir))
                        val brightness = 0.72f + 0.28f * diffuse
                        val baseColor = sticker.color.color
                        val litColor = Color(
                            red = min(1f, baseColor.red * brightness),
                            green = min(1f, baseColor.green * brightness),
                            blue = min(1f, baseColor.blue * brightness),
                            alpha = 1f
                        )

                        polygons.add(
                            RenderPolygon(
                                points = stProj.first,
                                depth = stProj.second,
                                color = litColor,
                                isSticker = true
                            )
                        )
                    }
                }
            }

            // Painter's algorithm: sort by depth ascending (smallest Z is farthest in our camera frame)
            polygons.sortBy { it.depth }

            // Draw all sorted polygons
            for (poly in polygons) {
                if (poly.points.size < 4) continue
                val path = Path().apply {
                    moveTo(poly.points[0].x, poly.points[0].y)
                    for (i in 1 until poly.points.size) {
                        lineTo(poly.points[i].x, poly.points[i].y)
                    }
                    close()
                }

                drawPath(path = path, color = poly.color, style = Fill)

                if (poly.isSticker) {
                    // Subtle glossy highlight line on top edge of sticker
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.25f),
                        style = Stroke(width = 1.5f)
                    )
                } else {
                    // Dark bevel stroke
                    drawPath(
                        path = path,
                        color = Color(0xFF0F1115),
                        style = Stroke(width = 2.0f)
                    )
                }
            }
        }
    }
}

private fun projectQuad(
    corners: List<Vector3D>,
    rotX: Float,
    rotY: Float,
    centerX: Float,
    centerY: Float,
    scale: Float,
    cameraDist: Float
): Pair<List<Offset>, Float>? {
    val screenPoints = mutableListOf<Offset>()
    var sumDepth = 0f

    for (corner in corners) {
        val rotated = corner.rotateY(rotY).rotateX(rotX)
        val z = rotated.z
        val denom = cameraDist - z
        if (denom <= 0.2f) return null

        val factor = cameraDist / denom
        val sx = centerX + rotated.x * scale * factor
        val sy = centerY - rotated.y * scale * factor // invert Y

        screenPoints.add(Offset(sx, sy))
        sumDepth += z
    }

    return Pair(screenPoints, sumDepth / corners.size)
}

private fun getFaceTangents(normal: Vector3D): Pair<Vector3D, Vector3D> {
    val nx = kotlin.math.abs(normal.x)
    val ny = kotlin.math.abs(normal.y)
    val nz = kotlin.math.abs(normal.z)

    return when {
        ny > 0.8f -> Pair(Vector3D(1f, 0f, 0f), Vector3D(0f, 0f, 1f))
        nx > 0.8f -> Pair(Vector3D(0f, 1f, 0f), Vector3D(0f, 0f, 1f))
        else -> Pair(Vector3D(1f, 0f, 0f), Vector3D(0f, 1f, 0f))
    }
}
