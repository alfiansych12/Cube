package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.AlgorithmDetailScreen
import com.example.ui.screens.FreePlayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.RoadmapDetailScreen
import com.example.ui.screens.ThreeByThreeScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.screens.TwoByTwoScreen
import com.example.ui.theme.RubikMasterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RubikMasterTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen !is Screen.Home) {
        viewModel.navigateBack()
    }

    // Determine which screens hide the bottom navigation bar
    val isSubScreen = currentScreen is Screen.AlgorithmDetail ||
            currentScreen is Screen.RoadmapDetail ||
            currentScreen is Screen.ThreeByThree ||
            currentScreen is Screen.TwoByTwo ||
            currentScreen is Screen.CameraScanner

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Timer,
                        onClick = { viewModel.navigateTo(Screen.Timer) },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "Timer Latihan") },
                        label = { Text("Timer") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.FreePlay3D,
                        onClick = { viewModel.navigateTo(Screen.FreePlay3D) },
                        icon = { Icon(Icons.Default.ViewInAr, contentDescription = "Lab 3D") },
                        label = { Text("Lab 3D") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Progress,
                        onClick = { viewModel.navigateTo(Screen.Progress) },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Progres") },
                        label = { Text("Progres") }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.TwoByTwo -> {
                TwoByTwoScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.ThreeByThree -> {
                ThreeByThreeScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.AlgorithmDetail -> {
                AlgorithmDetailScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.Timer -> {
                TimerScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.FreePlay3D -> {
                FreePlayScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.Progress -> {
                ProgressScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.RoadmapDetail -> {
                RoadmapDetailScreen(cube = screen.cube, viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
            is Screen.CameraScanner -> {
                com.example.ui.screens.CameraScannerScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
