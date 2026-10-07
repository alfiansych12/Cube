package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.engine.RubikColor
import com.example.scanner.CubeFace
import com.example.scanner.OLLMatchResult
import com.example.scanner.RubikOLLMatcher
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.CameraScannerView
import com.example.ui.components.FaceColorEditorDialog
import com.example.ui.components.MiniCubeNetView
import com.example.ui.components.OLLMatchResultDialog
import com.example.ui.components.RubikGridOverlay
import kotlinx.coroutines.launch

/**
 * Full camera detection screen with watermark frame, 6-face guided scanning,
 * and smart OLL algorithm pattern matching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Izin kamera diperlukan untuk memindai Rubik secara live.")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera Controls
    var isFlashEnabled by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var triggerCapture by remember { mutableStateOf(false) }

    // 6-Face Scanning State
    val faces = remember { CubeFace.entries }
    var currentFaceIndex by remember { mutableIntStateOf(0) }
    val currentFace = faces[currentFaceIndex]
    val scannedFaces = remember { mutableStateMapOf<CubeFace, List<RubikColor>>() }

    // Dialog States
    var showColorEditorDialog by remember { mutableStateOf(false) }
    var tempCapturedColors by remember { mutableStateOf<List<RubikColor>>(emptyList()) }
    var showCategorySelectorDialog by remember { mutableStateOf(false) }
    var showMatchResultsDialog by remember { mutableStateOf(false) }
    var matchResults by remember { mutableStateOf<List<OLLMatchResult>>(emptyList()) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDemoPresetsDialog by remember { mutableStateOf(false) }

    // All available algorithms from repo for OLL
    val algorithms3x3 by viewModel.currentAlgorithms3x3.collectAsState()
    val ollAlgorithms = remember(algorithms3x3) {
        algorithms3x3.filter { it.group.equals("OLL", ignoreCase = true) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Rubik Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${currentFace.title} (${currentFaceIndex + 1}/6)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF38BDF8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Help Dialog
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Panduan",
                            tint = Color.White
                        )
                    }
                    // Test Demo Presets
                    IconButton(onClick = { showDemoPresetsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Demo Preset OLL",
                            tint = Color(0xFFFACC15)
                        )
                    }
                    // Flashlight
                    if (hasCameraPermission) {
                        IconButton(onClick = { isFlashEnabled = !isFlashEnabled }) {
                            Icon(
                                imageVector = if (isFlashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flash",
                                tint = if (isFlashEnabled) Color(0xFFFACC15) else Color.White
                            )
                        }
                        // Flip Camera
                        IconButton(onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Ganti Kamera",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (hasCameraPermission) {
                // Live Camera View
                CameraScannerView(
                    currentFace = currentFace,
                    isFlashEnabled = isFlashEnabled,
                    lensFacing = lensFacing,
                    triggerCapture = triggerCapture,
                    onCaptureProcessed = { colors, _ ->
                        triggerCapture = false
                        viewModel.triggerHapticFeedback()
                        tempCapturedColors = colors
                        showColorEditorDialog = true
                    },
                    onCaptureFailed = { errorMsg ->
                        triggerCapture = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(errorMsg)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 3x3 Rubik Frame Watermark Overlay
                RubikGridOverlay(
                    currentFace = currentFace,
                    previewColors = scannedFaces[currentFace],
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Camera Permission Denied / Fallback Interface
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Akses Kamera Diperlukan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fitur scanner mendeteksi 9 warna blok Rubik secara otomatis dari kamera untuk mencocokkan rumus OLL.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Izinkan Kamera")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { showDemoPresetsDialog = true }) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coba Contoh Scan OLL (Demo Mode)")
                    }
                }
            }

            // Top Instruction Banner HUD
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(currentFace.centerColor.color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentFace.instruction,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = currentFace.guideHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Bottom Controls HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mini 2D Cube Net Progress Indicator
                MiniCubeNetView(
                    scannedFaces = scannedFaces,
                    currentFace = currentFace,
                    onSelectFace = { face ->
                        currentFaceIndex = faces.indexOf(face).coerceAtLeast(0)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Shutter & Analysis Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset / Clear
                    IconButton(
                        onClick = {
                            scannedFaces.clear()
                            currentFaceIndex = 0
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Scan",
                            tint = Color.White
                        )
                    }

                    // Main Capture Shutter Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .border(3.dp, Color.White, CircleShape)
                            .clickable {
                                if (hasCameraPermission) {
                                    triggerCapture = true
                                } else {
                                    // Fallback demo colors for current face
                                    tempCapturedColors = CubeFace.defaultFaceColors(currentFace)
                                    showColorEditorDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    // Scan & Analyze Button (Active once scanned faces exist)
                    IconButton(
                        onClick = { showCategorySelectorDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (scannedFaces.isNotEmpty()) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.2f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Analisis Rumus",
                            tint = if (scannedFaces.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else Color.White
                        )
                    }
                }
            }
        }
    }

    // 1. Color Verification Dialog
    if (showColorEditorDialog) {
        FaceColorEditorDialog(
            face = currentFace,
            initialColors = tempCapturedColors,
            onDismiss = { showColorEditorDialog = false },
            onRetake = {
                showColorEditorDialog = false
            },
            onSaveAndNext = { verifiedColors ->
                scannedFaces[currentFace] = verifiedColors
                showColorEditorDialog = false

                // If not last face, advance to next
                if (currentFaceIndex < faces.size - 1) {
                    currentFaceIndex++
                } else {
                    // All 6 faces scanned! Prompt category selection
                    showCategorySelectorDialog = true
                }
            }
        )
    }

    // 2. Category Selection Modal
    if (showCategorySelectorDialog) {
        AlertDialog(
            onDismissRequest = { showCategorySelectorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Kategori Rumus")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Sistem akan menganalisis orientasi warna dari 6 sisi kubus yang telah Anda foto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // OLL Option (Available & Recommended)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                showCategorySelectorDialog = false
                                val targetAlgos = if (ollAlgorithms.isNotEmpty()) ollAlgorithms
                                else viewModel.repository.getAlgorithms("3x3", "cfop").filter { it.group.equals("OLL", ignoreCase = true) }
                                val results = RubikOLLMatcher.matchOLL(scannedFaces, targetAlgos)
                                matchResults = results
                                showMatchResultsDialog = true
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("OLL", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OLL (Orientation of Last Layer)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "57 Rumus untuk membuat seluruh layer atas menjadi kuning.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Info for other stages
                    Text(
                        text = "Tahap F2L & PLL dapat diakses langsung melalui menu modul CFOP.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategorySelectorDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // 3. Match Results Dialog
    if (showMatchResultsDialog) {
        OLLMatchResultDialog(
            results = matchResults,
            onSelectAlgorithm = { selectedAlgo ->
                showMatchResultsDialog = false
                viewModel.selectAlgorithm(selectedAlgo)
                viewModel.navigateTo(Screen.AlgorithmDetail(selectedAlgo.id))
            },
            onDismiss = { showMatchResultsDialog = false },
            onRescan = {
                showMatchResultsDialog = false
                scannedFaces.clear()
                currentFaceIndex = 0
            }
        )
    }

    // 4. Help / Guidance Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Cara Menggunakan AI Rubik Scanner") },
            text = {
                Column {
                    Text(
                        text = "1. Pas kan 9 kotak kerangka pada layar dengan 9 stiker kubus Rubik Anda.\n\n" +
                               "2. Ambil foto secara berurutan untuk 6 sisi:\n" +
                               "   - Atas (Kuning)\n" +
                               "   - Depan (Hijau)\n" +
                               "   - Kanan (Merah)\n" +
                               "   - Belakang (Biru)\n" +
                               "   - Kiri (Oranye)\n" +
                               "   - Bawah (Putih)\n\n" +
                               "3. Jika warna terdeteksi kurang pas akibat pantulan lampu, ketuk kotak pada dialog untuk mengoreksi warnanya.\n\n" +
                               "4. Klik 'Analisis Rumus' -> Pilih OLL untuk langsung melihat nama rumus, diagram 3D, dan trik jari.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    // 5. Demo Presets Dialog (for fast testing on emulator / without cube)
    if (showDemoPresetsDialog) {
        AlertDialog(
            onDismissRequest = { showDemoPresetsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Contoh Pola OLL (Demo)")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih salah satu contoh pola acakan OLL siap uji:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val presets = listOf(
                        "sune" to "OLL 27 - Sune (Kasus Ikan Khas)",
                        "t_shape" to "OLL 33 - T-Shape (Bentuk Huruf T)",
                        "antisune" to "OLL 26 - Anti-Sune (Kebalikan Sune)",
                        "h_shape" to "OLL 21 - H-Shape (Cross 4 Sudut Kanan/Kiri)"
                    )

                    presets.forEach { (key, label) ->
                        OutlinedButton(
                            onClick = {
                                val presetData = RubikOLLMatcher.getPresetFaces(key)
                                scannedFaces.clear()
                                scannedFaces.putAll(presetData)
                                showDemoPresetsDialog = false
                                // Automatically analyze
                                val targetAlgos = if (ollAlgorithms.isNotEmpty()) ollAlgorithms
                                else viewModel.repository.getAlgorithms("3x3", "cfop").filter { it.group.equals("OLL", ignoreCase = true) }
                                val results = RubikOLLMatcher.matchOLL(scannedFaces, targetAlgos)
                                matchResults = results
                                showMatchResultsDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDemoPresetsDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
