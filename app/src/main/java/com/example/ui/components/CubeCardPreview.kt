package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.AlgorithmItem
import com.example.engine.CubeState2x2
import com.example.engine.CubeState3x3
import com.example.engine.Cubie
import com.example.engine.RubikColor
import com.example.engine.Vector3D

enum class PreviewMode {
    ISOMETRIC_3D,
    TOP_DOWN_DIAGRAM
}

@Composable
fun CubeCardPreview(
    algorithm: AlgorithmItem,
    modifier: Modifier = Modifier,
    initialMode: PreviewMode? = null
) {
    val is3x3 = algorithm.id.startsWith("3x3")
    val isOLLorPLL = algorithm.group.contains("OLL", ignoreCase = true) ||
            algorithm.group.contains("PLL", ignoreCase = true) ||
            algorithm.name.contains("OLL", ignoreCase = true) ||
            algorithm.name.contains("PLL", ignoreCase = true)

    val defaultMode = initialMode ?: if (isOLLorPLL) PreviewMode.TOP_DOWN_DIAGRAM else PreviewMode.ISOMETRIC_3D
    var mode by remember(algorithm.id) { mutableStateOf(defaultMode) }

    // Generate the setup cubies for this exact algorithm case
    val cubies = remember(algorithm.id) {
        if (is3x3) {
            val cube = CubeState3x3.createSolved()
            val setup = if (algorithm.setupMoves.isNotBlank()) algorithm.setupMoves else CubeState3x3.invertAlgorithm(algorithm.notation)
            cube.applyAlgorithm(setup)
            cube.cubies
        } else {
            val cube = CubeState2x2.createSolved()
            val setup = if (algorithm.setupMoves.isNotBlank()) algorithm.setupMoves else CubeState2x2.invertAlgorithm(algorithm.notation)
            cube.applyAlgorithm(setup)
            cube.cubies
        }
    }

    Box(
        modifier = modifier
            .size(86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable {
                // Tap thumbnail to toggle between 3D Isometric and Top-Down Diagram
                mode = if (mode == PreviewMode.ISOMETRIC_3D) PreviewMode.TOP_DOWN_DIAGRAM else PreviewMode.ISOMETRIC_3D
            },
        contentAlignment = Alignment.Center
    ) {
        if (mode == PreviewMode.ISOMETRIC_3D) {
            // Isometric 3D Canvas
            Rubik3DCanvas(
                cubies = cubies,
                allowDragRotation = false,
                initialRotX = if (isOLLorPLL) 0.68f else 0.42f,
                initialRotY = -0.58f,
                scaleFactor = if (is3x3) 0.65f else 0.95f,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Top-Down Diagram (matching CFOP Trainer OLL/PLL screenshots)
            TopDownDiagram(
                cubies = cubies,
                is3x3 = is3x3,
                algorithm = algorithm,
                modifier = Modifier.fillMaxSize().padding(6.dp)
            )
        }

        // Small indicator icon showing it can be toggled
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Ganti Mode Preview",
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(3.dp)
                .size(12.dp)
        )
    }
}

@Composable
fun TopDownDiagram(
    cubies: List<Cubie>,
    is3x3: Boolean,
    algorithm: AlgorithmItem,
    modifier: Modifier = Modifier
) {
    val isOLL = algorithm.group.contains("OLL", ignoreCase = true) || algorithm.name.contains("OLL", ignoreCase = true)
    val isPLL = algorithm.group.contains("PLL", ignoreCase = true) || algorithm.name.contains("PLL", ignoreCase = true)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer margin for rim stickers
        val rimThickness = w * 0.11f
        val gap = 2f

        val gridLeft = rimThickness + 3f
        val gridTop = rimThickness + 3f
        val gridWidth = w - 2 * gridLeft
        val gridHeight = h - 2 * gridTop

        val gridSize = if (is3x3) 3 else 2
        val cellSize = gridWidth / gridSize

        // 1. Draw Center Yellow / Face Grid
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val cx = if (is3x3) (col - 1).toFloat() else if (col == 0) -0.5f else 0.5f
                val cz = if (is3x3) (row - 1).toFloat() else if (row == 0) -0.5f else 0.5f

                // Find cubie at this position
                val cubie = cubies.find {
                    kotlin.math.abs(it.position.x - cx) < 0.3f &&
                            it.position.y > 0.3f &&
                            kotlin.math.abs(it.position.z - cz) < 0.3f
                }

                val sticker = cubie?.stickers?.find { it.currentNormal.y > 0.7f }
                val color = if (sticker != null) {
                    if (isOLL) {
                        if (sticker.color == RubikColor.YELLOW) Color(0xFFFACC15) else Color(0xFF475569)
                    } else {
                        sticker.color.color
                    }
                } else {
                    if (isOLL) Color(0xFF475569) else Color(0xFFFACC15)
                }

                val cellRectX = gridLeft + col * cellSize
                val cellRectY = gridTop + row * cellSize

                drawRoundRect(
                    color = color,
                    topLeft = Offset(cellRectX + gap, cellRectY + gap),
                    size = Size(cellSize - 2 * gap, cellSize - 2 * gap),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(cellRectX + gap, cellRectY + gap),
                    size = Size(cellSize - 2 * gap, cellSize - 2 * gap),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // 2. Draw Outer Rim Stickers (Top, Bottom, Left, Right)
        for (i in 0 until gridSize) {
            val coord = if (is3x3) (i - 1).toFloat() else if (i == 0) -0.5f else 0.5f

            // Top rim (Back face, normal = (0, 0, -1), z = -max)
            val topCubie = cubies.find {
                kotlin.math.abs(it.position.x - coord) < 0.3f &&
                        it.position.y > 0.3f &&
                        it.position.z < -0.3f
            }
            val topSticker = topCubie?.stickers?.find { it.currentNormal.z < -0.7f }
            val topColor = getRimColor(topSticker?.color, isOLL)
            if (topColor != null) {
                drawRoundRect(
                    color = topColor,
                    topLeft = Offset(gridLeft + i * cellSize + gap, 0f),
                    size = Size(cellSize - 2 * gap, rimThickness),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }

            // Bottom rim (Front face, normal = (0, 0, 1), z = +max)
            val bottomCubie = cubies.find {
                kotlin.math.abs(it.position.x - coord) < 0.3f &&
                        it.position.y > 0.3f &&
                        it.position.z > 0.3f
            }
            val bottomSticker = bottomCubie?.stickers?.find { it.currentNormal.z > 0.7f }
            val bottomColor = getRimColor(bottomSticker?.color, isOLL)
            if (bottomColor != null) {
                drawRoundRect(
                    color = bottomColor,
                    topLeft = Offset(gridLeft + i * cellSize + gap, h - rimThickness),
                    size = Size(cellSize - 2 * gap, rimThickness),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }

            // Left rim (Left face, normal = (-1, 0, 0), x = -max)
            val leftCubie = cubies.find {
                it.position.x < -0.3f &&
                        it.position.y > 0.3f &&
                        kotlin.math.abs(it.position.z - coord) < 0.3f
            }
            val leftSticker = leftCubie?.stickers?.find { it.currentNormal.x < -0.7f }
            val leftColor = getRimColor(leftSticker?.color, isOLL)
            if (leftColor != null) {
                drawRoundRect(
                    color = leftColor,
                    topLeft = Offset(0f, gridTop + i * cellSize + gap),
                    size = Size(rimThickness, cellSize - 2 * gap),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }

            // Right rim (Right face, normal = (1, 0, 0), x = +max)
            val rightCubie = cubies.find {
                it.position.x > 0.3f &&
                        it.position.y > 0.3f &&
                        kotlin.math.abs(it.position.z - coord) < 0.3f
            }
            val rightSticker = rightCubie?.stickers?.find { it.currentNormal.x > 0.7f }
            val rightColor = getRimColor(rightSticker?.color, isOLL)
            if (rightColor != null) {
                drawRoundRect(
                    color = rightColor,
                    topLeft = Offset(w - rimThickness, gridTop + i * cellSize + gap),
                    size = Size(rimThickness, cellSize - 2 * gap),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }
        }

        // 3. Draw Permutation Arrows for PLL (matching Screenshot 1!)
        if (isPLL && is3x3) {
            drawPllArrows(algorithm.name, gridLeft, gridTop, cellSize)
        }
    }
}

private fun getRimColor(rubikColor: RubikColor?, isOLL: Boolean): Color? {
    if (rubikColor == null) return null
    return if (isOLL) {
        if (rubikColor == RubikColor.YELLOW) Color(0xFFFACC15) else Color(0xFF334155)
    } else {
        rubikColor.color
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPllArrows(
    algoName: String,
    gridLeft: Float,
    gridTop: Float,
    cellSize: Float
) {
    val arrowColor = Color(0xFF94A3B8)
    val stroke = Stroke(width = 2.5f, cap = StrokeCap.Round)

    fun centerOf(col: Int, row: Int) = Offset(gridLeft + (col + 0.5f) * cellSize, gridTop + (row + 0.5f) * cellSize)

    when {
        algoName.contains("Z-Perm", ignoreCase = true) || algoName.contains(" Z ", ignoreCase = true) -> {
            // Diagonal swap edges (F <-> R and B <-> L)
            drawArrow(centerOf(1, 2), centerOf(2, 1), arrowColor, stroke)
            drawArrow(centerOf(1, 0), centerOf(0, 1), arrowColor, stroke)
        }
        algoName.contains("H-Perm", ignoreCase = true) -> {
            // Opposite edges swap (F <-> B and L <-> R)
            drawArrow(centerOf(1, 2), centerOf(1, 0), arrowColor, stroke)
            drawArrow(centerOf(0, 1), centerOf(2, 1), arrowColor, stroke)
        }
        algoName.contains("T-Perm", ignoreCase = true) -> {
            // Adjacent corners (top-right & bottom-right) and edges (right & bottom)
            drawArrow(centerOf(2, 0), centerOf(2, 2), arrowColor, stroke)
            drawArrow(centerOf(2, 1), centerOf(1, 2), arrowColor, stroke)
        }
        algoName.contains("Y-Perm", ignoreCase = true) -> {
            // Diagonal corners (top-left & bottom-right) and edges (top & left)
            drawArrow(centerOf(0, 0), centerOf(2, 2), arrowColor, stroke)
            drawArrow(centerOf(1, 0), centerOf(0, 1), arrowColor, stroke)
        }
        algoName.contains("F-Perm", ignoreCase = true) -> {
            // Adjacent corners (left) and opposite edges (top & bottom)
            drawArrow(centerOf(0, 0), centerOf(0, 2), arrowColor, stroke)
            drawArrow(centerOf(1, 0), centerOf(1, 2), arrowColor, stroke)
        }
        algoName.contains("Ja-Perm", ignoreCase = true) -> {
            // Left corners and left edges
            drawArrow(centerOf(0, 0), centerOf(0, 2), arrowColor, stroke)
            drawArrow(centerOf(0, 1), centerOf(1, 2), arrowColor, stroke)
        }
        algoName.contains("Jb-Perm", ignoreCase = true) -> {
            // Right corners and right edges
            drawArrow(centerOf(2, 0), centerOf(2, 2), arrowColor, stroke)
            drawArrow(centerOf(2, 1), centerOf(1, 2), arrowColor, stroke)
        }
        algoName.contains("Ra-Perm", ignoreCase = true) || algoName.contains("Rb-Perm", ignoreCase = true) -> {
            // Adjacent corners and adjacent edges
            drawArrow(centerOf(0, 0), centerOf(2, 0), arrowColor, stroke)
            drawArrow(centerOf(1, 2), centerOf(2, 1), arrowColor, stroke)
        }
        algoName.contains("V-Perm", ignoreCase = true) -> {
            // Diagonal corners (top-left & bottom-right) and edges (top & right)
            drawArrow(centerOf(0, 0), centerOf(2, 2), arrowColor, stroke)
            drawArrow(centerOf(1, 0), centerOf(2, 1), arrowColor, stroke)
        }
        algoName.contains("Na-Perm", ignoreCase = true) || algoName.contains("Nb-Perm", ignoreCase = true) -> {
            // Diagonal corners and opposite edges
            drawArrow(centerOf(0, 0), centerOf(2, 2), arrowColor, stroke)
            drawArrow(centerOf(2, 0), centerOf(0, 2), arrowColor, stroke)
        }
        algoName.contains("Ga-Perm", ignoreCase = true) || algoName.contains("Gb-Perm", ignoreCase = true) ||
        algoName.contains("Gc-Perm", ignoreCase = true) || algoName.contains("Gd-Perm", ignoreCase = true) -> {
            // 3-corner cycle + 3-edge cycle
            drawArrow(centerOf(0, 0), centerOf(2, 0), arrowColor, stroke)
            drawArrow(centerOf(2, 0), centerOf(2, 2), arrowColor, stroke)
            drawArrow(centerOf(1, 2), centerOf(0, 1), arrowColor, stroke)
        }
        algoName.contains("Ua-Perm", ignoreCase = true) || algoName.contains("Ub-Perm", ignoreCase = true) -> {
            // 3-edge cycle
            drawArrow(centerOf(1, 2), centerOf(2, 1), arrowColor, stroke)
            drawArrow(centerOf(2, 1), centerOf(0, 1), arrowColor, stroke)
            drawArrow(centerOf(0, 1), centerOf(1, 2), arrowColor, stroke)
        }
        algoName.contains("Aa-Perm", ignoreCase = true) || algoName.contains("Ab-Perm", ignoreCase = true) -> {
            // 3-corner cycle
            drawArrow(centerOf(0, 0), centerOf(2, 0), arrowColor, stroke)
            drawArrow(centerOf(2, 0), centerOf(2, 2), arrowColor, stroke)
            drawArrow(centerOf(2, 2), centerOf(0, 0), arrowColor, stroke)
        }
        algoName.contains("E-Perm", ignoreCase = true) -> {
            // Parallel opposite corners swap
            drawArrow(centerOf(0, 0), centerOf(0, 2), arrowColor, stroke)
            drawArrow(centerOf(2, 0), centerOf(2, 2), arrowColor, stroke)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    stroke: Stroke
) {
    drawLine(color = color, start = start, end = end, strokeWidth = stroke.width)
    // Draw small double-ended or directional circle
    drawCircle(color = color, radius = 2.8f, center = start)
    drawCircle(color = color, radius = 2.8f, center = end)
}
