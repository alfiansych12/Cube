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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.masteryStats.collectAsState()
    val bestTime2x2 by viewModel.bestTime2x2.collectAsState()
    val bestTime3x3 by viewModel.bestTime3x3.collectAsState()
    val solves2x2 by viewModel.solves2x2.collectAsState()
    val solves3x3 by viewModel.solves3x3.collectAsState()

    val totalPercentage = if (stats.totalAlgorithms > 0) {
        (stats.masteredAlgorithms.toFloat() / stats.totalAlgorithms.toFloat()) * 100f
    } else 0f

    val rankTitle = when {
        stats.masteredAlgorithms >= 20 -> "Elite Speedcuber"
        stats.masteredAlgorithms >= 10 -> "Advanced Cuber"
        stats.masteredAlgorithms >= 4 -> "Intermediate Cuber"
        else -> "Beginner Cuber"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pelacakan Progres & Statistik",
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
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rank Badge Hero Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = rankTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "${stats.masteredAlgorithms} dari ${stats.totalAlgorithms} Algoritma Telah Dikuasai (${totalPercentage.toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { totalPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Quick Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFEAB308),
                        title = "Dikuasai",
                        value = "${stats.masteredAlgorithms}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.Timer,
                        iconColor = Color(0xFF22C55E),
                        title = "PB 2x2",
                        value = if (bestTime2x2 != null) String.format("%.2fs", bestTime2x2!! / 1000f) else "-",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.Timer,
                        iconColor = Color(0xFF10B981),
                        title = "PB 3x3",
                        value = if (bestTime3x3 != null) String.format("%.2fs", bestTime3x3!! / 1000f) else "-",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        icon = Icons.Default.EmojiEvents,
                        iconColor = Color(0xFF3B82F6),
                        title = "Solves",
                        value = "${solves2x2.size + solves3x3.size}",
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }

            // Method Breakdown Section 2x2
            item {
                Text(
                    text = "Progres Metode 2x2 Pocket Cube",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                val breakdown = stats.methodBreakdown
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MethodProgressRow(
                        name = "Metode Ortega (OLL & PBL)",
                        mastered = breakdown["ortega"]?.first ?: 0,
                        total = breakdown["ortega"]?.second ?: 0,
                        accentColor = Color(0xFF3B82F6)
                    )
                    MethodProgressRow(
                        name = "Metode CLL (Corners of Last Layer)",
                        mastered = breakdown["cll"]?.first ?: 0,
                        total = breakdown["cll"]?.second ?: 0,
                        accentColor = Color(0xFF8B5CF6)
                    )
                    MethodProgressRow(
                        name = "Metode EG-1 (1-Look Solve)",
                        mastered = breakdown["eg1"]?.first ?: 0,
                        total = breakdown["eg1"]?.second ?: 0,
                        accentColor = Color(0xFFEC4899)
                    )
                    MethodProgressRow(
                        name = "Metode LBL 2x2 (Pemula)",
                        mastered = breakdown["lbl"]?.first ?: 0,
                        total = breakdown["lbl"]?.second ?: 0,
                        accentColor = Color(0xFF10B981)
                    )
                }
            }

            // Method Breakdown Section 3x3
            item {
                Text(
                    text = "Progres Metode 3x3 Rubik's Cube",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                val breakdown = stats.methodBreakdown
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MethodProgressRow(
                        name = "Metode CFOP (F2L, OLL, PLL)",
                        mastered = breakdown["cfop"]?.first ?: 0,
                        total = breakdown["cfop"]?.second ?: 0,
                        accentColor = Color(0xFF2563EB)
                    )
                    MethodProgressRow(
                        name = "Metode Pemula 3x3 (LBL)",
                        mastered = breakdown["lbl_3x3"]?.first ?: 0,
                        total = breakdown["lbl_3x3"]?.second ?: 0,
                        accentColor = Color(0xFF16A34A)
                    )
                    MethodProgressRow(
                        name = "Metode Roux (Block & M-Slice)",
                        mastered = breakdown["roux"]?.first ?: 0,
                        total = breakdown["roux"]?.second ?: 0,
                        accentColor = Color(0xFFF59E0B)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MethodProgressRow(
    name: String,
    mastered: Int,
    total: Int,
    accentColor: Color
) {
    val fraction = if (total > 0) mastered.toFloat() / total.toFloat() else 0f
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$mastered / $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
