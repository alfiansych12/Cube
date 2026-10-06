package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.Rubik3DCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreePlayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val freePlayState by viewModel.freePlayEngine.state.collectAsState()
    var customNotation by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "3D Rubik Engine Lab",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.freePlayEngine.resetToSolved() }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Solved",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Cube Size Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = freePlayState.cubeSize == 2,
                    onClick = { viewModel.setFreePlayCubeSize(2) },
                    label = { Text("2x2") }
                )
                FilterChip(
                    selected = freePlayState.cubeSize == 3,
                    onClick = { viewModel.setFreePlayCubeSize(3) },
                    label = { Text("3x3") }
                )
                FilterChip(
                    selected = freePlayState.cubeSize == 4,
                    onClick = { viewModel.setFreePlayCubeSize(4) },
                    label = { Text("4x4") }
                )
                FilterChip(
                    selected = freePlayState.cubeSize == 5,
                    onClick = { viewModel.setFreePlayCubeSize(5) },
                    label = { Text("5x5") }
                )
            }

            // 3D Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFF0F172A))
            ) {
                Rubik3DCanvas(
                    cubies = freePlayState.cubies,
                    activeTurn = freePlayState.activeTurn,
                    turnProgress = freePlayState.turnProgress,
                    allowDragRotation = true,
                    scaleFactor = when (freePlayState.cubeSize) {
                        5 -> 0.44f
                        4 -> 0.55f
                        3 -> 0.72f
                        else -> 1.05f
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Drag instruction
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Geser untuk rotasi 360° (${freePlayState.cubeSize}x${freePlayState.cubeSize})",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }

                // Solved status
                if (freePlayState.isSolved) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.9f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SOLVED",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Quick Buttons: Scramble & Reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.freePlayEngine.scrambleCube() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Acak (Scramble)")
                }

                FilledTonalButton(
                    onClick = { viewModel.freePlayEngine.resetToSolved() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pecahkan (Solve)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Face Turn Controller
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kontrol Gerakan Manual (${freePlayState.cubeSize}x${freePlayState.cubeSize})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row R
                    TurnRow(
                        moves = listOf("R", "R'", "R2"),
                        onMove = { viewModel.freePlayEngine.performFreeMove(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row L
                    TurnRow(
                        moves = listOf("L", "L'", "L2"),
                        onMove = { viewModel.freePlayEngine.performFreeMove(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row U
                    TurnRow(
                        moves = listOf("U", "U'", "U2"),
                        onMove = { viewModel.freePlayEngine.performFreeMove(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row D
                    TurnRow(
                        moves = listOf("D", "D'", "D2"),
                        onMove = { viewModel.freePlayEngine.performFreeMove(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row F
                    TurnRow(
                        moves = listOf("F", "F'", "F2"),
                        onMove = { viewModel.freePlayEngine.performFreeMove(it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Row B
                    TurnRow(
                        moves = listOf("B", "B'", "B2"),
                        onMove = { viewModel.freePlayEngine.performFreeMove(it) }
                    )

                    if (freePlayState.cubeSize == 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row M, S, Rw
                        TurnRow(
                            moves = listOf("M", "M'", "Rw"),
                            onMove = { viewModel.freePlayEngine.performFreeMove(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Algorithm Runner Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Uji Coba Rumus Bebas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Masukkan notasi rumus (contoh: R U R' U') untuk disimulasikan oleh mesin 3D:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customNotation,
                        onValueChange = { customNotation = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("misal: R U R' U'") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (customNotation.isNotBlank()) {
                                viewModel.freePlayEngine.scrambleCube(customNotation)
                            }
                        },
                        enabled = customNotation.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Jalankan pada Kubus 3D")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TurnRow(
    moves: List<String>,
    onMove: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        moves.forEach { move ->
            FilledTonalButton(
                onClick = { onMove(move) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = move,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
