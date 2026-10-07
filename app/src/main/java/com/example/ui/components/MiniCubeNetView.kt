package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RubikColor
import com.example.scanner.CubeFace

/**
 * Visual 2D Net showing the progress of scanning all 6 Rubik faces.
 * Users can tap any face to select or re-scan it.
 */
@Composable
fun MiniCubeNetView(
    scannedFaces: Map<CubeFace, List<RubikColor>>,
    currentFace: CubeFace,
    onSelectFace: (CubeFace) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Status 6 Sisi Kubus (${scannedFaces.size}/6)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Row 1: UP (aligned with Front)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(34.dp))
                FaceThumbnail(
                    face = CubeFace.UP,
                    colors = scannedFaces[CubeFace.UP],
                    isSelected = currentFace == CubeFace.UP,
                    onClick = { onSelectFace(CubeFace.UP) }
                )
                Spacer(modifier = Modifier.size(68.dp))
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Row 2: LEFT, FRONT, RIGHT, BACK
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FaceThumbnail(
                    face = CubeFace.LEFT,
                    colors = scannedFaces[CubeFace.LEFT],
                    isSelected = currentFace == CubeFace.LEFT,
                    onClick = { onSelectFace(CubeFace.LEFT) }
                )
                FaceThumbnail(
                    face = CubeFace.FRONT,
                    colors = scannedFaces[CubeFace.FRONT],
                    isSelected = currentFace == CubeFace.FRONT,
                    onClick = { onSelectFace(CubeFace.FRONT) }
                )
                FaceThumbnail(
                    face = CubeFace.RIGHT,
                    colors = scannedFaces[CubeFace.RIGHT],
                    isSelected = currentFace == CubeFace.RIGHT,
                    onClick = { onSelectFace(CubeFace.RIGHT) }
                )
                FaceThumbnail(
                    face = CubeFace.BACK,
                    colors = scannedFaces[CubeFace.BACK],
                    isSelected = currentFace == CubeFace.BACK,
                    onClick = { onSelectFace(CubeFace.BACK) }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Row 3: DOWN (aligned with Front)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(34.dp))
                FaceThumbnail(
                    face = CubeFace.DOWN,
                    colors = scannedFaces[CubeFace.DOWN],
                    isSelected = currentFace == CubeFace.DOWN,
                    onClick = { onSelectFace(CubeFace.DOWN) }
                )
                Spacer(modifier = Modifier.size(68.dp))
            }
        }
    }
}

@Composable
private fun FaceThumbnail(
    face: CubeFace,
    colors: List<RubikColor>?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isScanned = colors != null
    val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) borderCol else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (colors != null && colors.size == 9) {
            // Render 3x3 mini grid of detected colors
            Column(
                modifier = Modifier.size(28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 3) {
                            val color = colors[row * 3 + col].color
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        } else {
            // Unscanned placeholder with center color hint and code letter
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(face.centerColor.color.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = face.code,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Small check badge if completed
        if (isScanned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(7.dp)
                )
            }
        }
    }
}
