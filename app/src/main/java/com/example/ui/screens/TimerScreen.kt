package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SolveRecord
import com.example.ui.MainViewModel
import com.example.ui.TimerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val timerState by viewModel.timerState.collectAsState()
    val elapsedTimeMillis by viewModel.elapsedTimeMillis.collectAsState()
    val inspectionSeconds by viewModel.inspectionSecondsLeft.collectAsState()
    val currentScramble by viewModel.currentScramble.collectAsState()
    val isInspectionEnabled by viewModel.isInspectionEnabled.collectAsState()
    val selectedTimerCube by viewModel.selectedTimerCube.collectAsState()
    val solves2x2 by viewModel.solves2x2.collectAsState()
    val solves3x3 by viewModel.solves3x3.collectAsState()
    val bestTime2x2 by viewModel.bestTime2x2.collectAsState()
    val bestTime3x3 by viewModel.bestTime3x3.collectAsState()

    val solves = if (selectedTimerCube == "3x3") solves3x3 else solves2x2
    val bestTime = if (selectedTimerCube == "3x3") bestTime3x3 else bestTime2x2

    // Calculate Ao5 and Ao12
    val ao5 = remember(solves) { calculateAo(solves, 5) }
    val ao12 = remember(solves) { calculateAo(solves, 12) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Timer Latihan Speedcubing",
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
                    FilterChip(
                        selected = isInspectionEnabled,
                        onClick = { viewModel.toggleInspectionMode() },
                        label = { Text("Inspeksi 15s") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Cube Type Selector for Timer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTimerCube == "2x2",
                    onClick = { viewModel.setSelectedTimerCube("2x2") },
                    label = { Text("2x2 Pocket") }
                )
                FilterChip(
                    selected = selectedTimerCube == "3x3",
                    onClick = { viewModel.setSelectedTimerCube("3x3") },
                    label = { Text("3x3 Rubik's") }
                )
            }

            // Scramble Box
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scramble $selectedTimerCube:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentScramble,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { viewModel.newScramble() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scramble Baru",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Big Timer Interactive Pad
            val timerColor = when (timerState) {
                TimerState.HOLDING -> Color(0xFFF59E0B) // Amber
                TimerState.ARMED -> Color(0xFF22C55E) // Bright Green
                TimerState.INSPECTING -> Color(0xFFEF4444) // Red inspection
                TimerState.TIMING -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onBackground
            }

            val formattedTime = formatTimerTime(elapsedTimeMillis)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (timerState) {
                            TimerState.ARMED -> Color(0xFF22C55E).copy(alpha = 0.12f)
                            TimerState.HOLDING -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                    .pointerInput(timerState) {
                        detectTapGestures(
                            onPress = {
                                viewModel.onTimerTouchDown()
                                tryAwaitRelease()
                                viewModel.onTimerTouchUp()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (timerState == TimerState.INSPECTING) {
                        Text(
                            text = "$inspectionSeconds",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Inspeksi (Sentuh & tahan jika siap)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFEF4444)
                        )
                    } else {
                        Text(
                            text = formattedTime,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black,
                            color = timerColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val statusText = when (timerState) {
                            TimerState.HOLDING -> "Tahan hingga hijau..."
                            TimerState.ARMED -> "SIAP! Lepas untuk mulai"
                            TimerState.TIMING -> "Sentuh di mana saja untuk berhenti"
                            TimerState.STOPPED -> "Selesai! Tahan lagi untuk scramble baru"
                            else -> "Tahan layar untuk memulai"
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = timerColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Session Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    label = "PB",
                    value = formatNullableMillis(bestTime),
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Ao5",
                    value = formatNullableMillis(ao5),
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Ao12",
                    value = formatNullableMillis(ao12),
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Total",
                    value = "${solves.size}",
                    modifier = Modifier.weight(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Solve Records History Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Catatan Waktu (${solves.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Solve List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (solves.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada catatan waktu.\nTahan tombol di atas untuk latihan pertama!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                items(solves, key = { it.id }) { record ->
                    SolveRowItem(
                        record = record,
                        onDelete = { viewModel.deleteSolve(record.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SolveRowItem(
    record: SolveRecord,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimerTime(record.timeMillis),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.scramble,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTimerTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10

    return if (minutes > 0) {
        String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
    } else {
        String.format("%02d.%02d", seconds, hundredths)
    }
}

private fun formatNullableMillis(millis: Long?): String {
    return if (millis == null) "-" else formatTimerTime(millis)
}

private fun calculateAo(solves: List<SolveRecord>, count: Int): Long? {
    if (solves.size < count) return null
    val slice = solves.take(count).map { it.timeMillis }.sorted()
    // Drop best and worst
    val trimmed = slice.subList(1, count - 1)
    return trimmed.average().toLong()
}
