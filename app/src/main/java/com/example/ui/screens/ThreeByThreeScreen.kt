package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AlgorithmCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeByThreeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMethodId by viewModel.selectedMethodId3x3.collectAsState()
    val selectedGroupFilter by viewModel.selectedGroupFilter3x3.collectAsState()
    val algorithms by viewModel.currentAlgorithms3x3.collectAsState()

    val currentMethod = viewModel.methods3x3.find { it.id == selectedMethodId }

    val availableGroups = remember(algorithms) {
        algorithms.map { it.group }.distinct()
    }

    val filteredAlgorithms = remember(algorithms, selectedGroupFilter) {
        if (selectedGroupFilter.isNullOrBlank()) algorithms
        else algorithms.filter { it.group == selectedGroupFilter }
    }

    val methodTabs = listOf(
        Pair("cfop", "CFOP (Speedcubing)"),
        Pair("lbl_3x3", "Pemula (LBL)"),
        Pair("roux", "Roux")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Modul 3x3 Rubik's Cube",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${algorithms.size} Rumus Terstruktur",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    IconButton(onClick = {
                        viewModel.setFreePlayCubeSize(3)
                        viewModel.navigateTo(Screen.FreePlay3D)
                    }) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D Playground Bebas",
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
        ) {
            // Method Tabs Row
            val selectedTabIndex = methodTabs.indexOfFirst { it.first == selectedMethodId }.coerceAtLeast(0)
            TabRow(selectedTabIndex = selectedTabIndex) {
                methodTabs.forEachIndexed { index, (id, label) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.setSelectedMethod3x3(id) },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Method Explanatory Card
                if (currentMethod != null) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentMethod.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = currentMethod.difficulty,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentMethod.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (currentMethod.steps.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    currentMethod.steps.forEach { step ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = step,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sub-group / Category Filter Chips
                if (availableGroups.size > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { viewModel.setSelectedGroupFilter3x3(null) },
                                label = { Text("Semua (${algorithms.size})") },
                                colors = FilterChipDefaults.filterChipColors()
                            )

                            availableGroups.forEach { group ->
                                val count = algorithms.count { it.group == group }
                                FilterChip(
                                    selected = selectedGroupFilter == group,
                                    onClick = { viewModel.setSelectedGroupFilter3x3(group) },
                                    label = { Text("$group ($count)") }
                                )
                            }
                        }
                    }
                }

                // Algorithm items
                items(filteredAlgorithms, key = { it.id }) { algo ->
                    AlgorithmCard(
                        algorithm = algo,
                        onClick = { viewModel.navigateTo(Screen.AlgorithmDetail(algo.id)) },
                        onToggleMastery = { viewModel.toggleMastery(algo) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
