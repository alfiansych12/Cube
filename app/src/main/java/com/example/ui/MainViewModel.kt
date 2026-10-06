package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlgorithmItem
import com.example.data.model.SolveRecord
import com.example.data.repository.MasteryStats
import com.example.data.repository.MethodSummary
import com.example.data.repository.RoadmapStage
import com.example.data.repository.RubikRepository
import com.example.engine.CubePlaybackEngine
import com.example.engine.CubeState2x2
import com.example.engine.CubeState3x3
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    data object Home : Screen()
    data class TwoByTwo(val initialMethodId: String? = null) : Screen()
    data class ThreeByThree(val initialMethodId: String? = null) : Screen()
    data class AlgorithmDetail(val algorithmId: String) : Screen()
    data object Timer : Screen()
    data object FreePlay3D : Screen()
    data object Progress : Screen()
    data class RoadmapDetail(val cube: String) : Screen()
}

enum class TimerState {
    IDLE,
    HOLDING,
    ARMED,
    INSPECTING,
    TIMING,
    STOPPED
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = RubikRepository(application)
    val playbackEngine = CubePlaybackEngine(viewModelScope, 2)
    val freePlayEngine = CubePlaybackEngine(viewModelScope, 3)

    // Navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<Screen>(Screen.Home)

    // Methods & Roadmaps
    val methods2x2: List<MethodSummary> = repository.getMethodsForCube("2x2")
    val methods3x3: List<MethodSummary> = repository.getMethodsForCube("3x3")
    val roadmaps: List<RoadmapStage> = repository.getRoadmaps()

    // Backwards-compatible aliases for 2x2
    val methods: List<MethodSummary> get() = methods2x2
    val selectedMethodId: StateFlow<String> get() = selectedMethodId2x2
    val selectedGroupFilter: StateFlow<String?> get() = selectedGroupFilter2x2
    val currentAlgorithms: StateFlow<List<AlgorithmItem>> get() = currentAlgorithms2x2

    // 2x2 Selected Method
    private val _selectedMethodId2x2 = MutableStateFlow("ortega")
    val selectedMethodId2x2: StateFlow<String> = _selectedMethodId2x2.asStateFlow()

    private val _selectedGroupFilter2x2 = MutableStateFlow<String?>(null)
    val selectedGroupFilter2x2: StateFlow<String?> = _selectedGroupFilter2x2.asStateFlow()

    private val _currentAlgorithms2x2 = MutableStateFlow<List<AlgorithmItem>>(emptyList())
    val currentAlgorithms2x2: StateFlow<List<AlgorithmItem>> = _currentAlgorithms2x2.asStateFlow()

    fun setSelectedMethod(methodId: String) = setSelectedMethod2x2(methodId)
    fun setSelectedGroupFilter(group: String?) = setSelectedGroupFilter2x2(group)

    // 3x3 Selected Method
    private val _selectedMethodId3x3 = MutableStateFlow("cfop")
    val selectedMethodId3x3: StateFlow<String> = _selectedMethodId3x3.asStateFlow()

    private val _selectedGroupFilter3x3 = MutableStateFlow<String?>(null)
    val selectedGroupFilter3x3: StateFlow<String?> = _selectedGroupFilter3x3.asStateFlow()

    private val _currentAlgorithms3x3 = MutableStateFlow<List<AlgorithmItem>>(emptyList())
    val currentAlgorithms3x3: StateFlow<List<AlgorithmItem>> = _currentAlgorithms3x3.asStateFlow()

    // Selected Algorithm for Detail Screen
    private val _selectedAlgorithm = MutableStateFlow<AlgorithmItem?>(null)
    val selectedAlgorithm: StateFlow<AlgorithmItem?> = _selectedAlgorithm.asStateFlow()

    // Free Play Active Cube Size (2 or 3)
    private val _freePlayCubeSize = MutableStateFlow(3)
    val freePlayCubeSize: StateFlow<Int> = _freePlayCubeSize.asStateFlow()

    // Stats
    val masteryStats: StateFlow<MasteryStats> = repository.getMasteryStatsFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MasteryStats(0, 0, 0, emptyMap())
        )

    val bestTime2x2: StateFlow<Long?> = repository.getBestTimeFlow("2x2")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val solves2x2: StateFlow<List<SolveRecord>> = repository.getSolvesFlow("2x2")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bestTime3x3: StateFlow<Long?> = repository.getBestTimeFlow("3x3")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val solves3x3: StateFlow<List<SolveRecord>> = repository.getSolvesFlow("3x3")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Timer State
    private val _selectedTimerCube = MutableStateFlow("3x3")
    val selectedTimerCube: StateFlow<String> = _selectedTimerCube.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _elapsedTimeMillis = MutableStateFlow(0L)
    val elapsedTimeMillis: StateFlow<Long> = _elapsedTimeMillis.asStateFlow()

    private val _inspectionSecondsLeft = MutableStateFlow(15)
    val inspectionSecondsLeft: StateFlow<Int> = _inspectionSecondsLeft.asStateFlow()

    private val _currentScramble = MutableStateFlow(CubeState3x3.generate3x3Scramble())
    val currentScramble: StateFlow<String> = _currentScramble.asStateFlow()

    private val _isInspectionEnabled = MutableStateFlow(false)
    val isInspectionEnabled: StateFlow<Boolean> = _isInspectionEnabled.asStateFlow()

    private var timerJob: Job? = null
    private var inspectionJob: Job? = null
    private var timerStartTime = 0L

    init {
        loadAlgorithmsForMethod2x2("ortega")
        loadAlgorithmsForMethod3x3("cfop")
    }

    fun navigateTo(screen: Screen) {
        screenStack.add(screen)
        _currentScreen.value = screen
        if (screen is Screen.AlgorithmDetail) {
            val algo = repository.getAlgorithmById(screen.algorithmId)
            _selectedAlgorithm.value = algo
            if (algo != null) {
                val size = if (algo.id.startsWith("3x3")) 3 else 2
                playbackEngine.loadAlgorithm(algo, size)
                viewModelScope.launch {
                    repository.recordPractice(algo.id, algo.methodId)
                }
            }
        } else if (screen is Screen.TwoByTwo) {
            if (screen.initialMethodId != null) {
                setSelectedMethod2x2(screen.initialMethodId)
            }
        } else if (screen is Screen.ThreeByThree) {
            if (screen.initialMethodId != null) {
                setSelectedMethod3x3(screen.initialMethodId)
            }
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            val prev = screenStack.last()
            _currentScreen.value = prev
            if (prev is Screen.AlgorithmDetail) {
                val algo = repository.getAlgorithmById(prev.algorithmId)
                _selectedAlgorithm.value = algo
                if (algo != null) {
                    val size = if (algo.id.startsWith("3x3")) 3 else 2
                    playbackEngine.loadAlgorithm(algo, size)
                }
            }
            return true
        }
        return false
    }

    fun setSelectedMethod2x2(methodId: String) {
        _selectedMethodId2x2.value = methodId
        _selectedGroupFilter2x2.value = null
        loadAlgorithmsForMethod2x2(methodId)
    }

    fun setSelectedGroupFilter2x2(group: String?) {
        _selectedGroupFilter2x2.value = group
    }

    private fun loadAlgorithmsForMethod2x2(methodId: String) {
        viewModelScope.launch {
            repository.getAlgorithmsFlow(methodId, "2x2").collect { algos ->
                _currentAlgorithms2x2.value = algos
                val current = _selectedAlgorithm.value
                if (current != null && !current.id.startsWith("3x3")) {
                    _selectedAlgorithm.value = algos.find { it.id == current.id } ?: current
                }
            }
        }
    }

    fun setSelectedMethod3x3(methodId: String) {
        _selectedMethodId3x3.value = methodId
        _selectedGroupFilter3x3.value = null
        loadAlgorithmsForMethod3x3(methodId)
    }

    fun setSelectedGroupFilter3x3(group: String?) {
        _selectedGroupFilter3x3.value = group
    }

    private fun loadAlgorithmsForMethod3x3(methodId: String) {
        viewModelScope.launch {
            repository.getAlgorithmsFlow(methodId, "3x3").collect { algos ->
                _currentAlgorithms3x3.value = algos
                val current = _selectedAlgorithm.value
                if (current != null && current.id.startsWith("3x3")) {
                    _selectedAlgorithm.value = algos.find { it.id == current.id } ?: current
                }
            }
        }
    }

    fun setFreePlayCubeSize(size: Int) {
        _freePlayCubeSize.value = size
        freePlayEngine.setCubeSize(size)
    }

    fun setSelectedTimerCube(cube: String) {
        _selectedTimerCube.value = cube
        newScramble()
    }

    fun toggleMastery(algo: AlgorithmItem) {
        vibrate(30)
        viewModelScope.launch {
            repository.toggleMastery(algo.id, algo.methodId)
            val updated = algo.copy(isMastered = !algo.isMastered)
            _selectedAlgorithm.value = updated
        }
    }

    // Timer Controls
    fun onTimerTouchDown() {
        if (_timerState.value == TimerState.TIMING) {
            stopTimer()
            return
        }
        if (_timerState.value == TimerState.IDLE || _timerState.value == TimerState.STOPPED) {
            _timerState.value = TimerState.HOLDING
            viewModelScope.launch {
                delay(350)
                if (_timerState.value == TimerState.HOLDING) {
                    _timerState.value = TimerState.ARMED
                    vibrate(40)
                }
            }
        }
    }

    fun onTimerTouchUp() {
        if (_timerState.value == TimerState.ARMED) {
            if (_isInspectionEnabled.value && _timerState.value != TimerState.INSPECTING) {
                startInspection()
            } else {
                startTiming()
            }
        } else if (_timerState.value == TimerState.HOLDING) {
            _timerState.value = TimerState.IDLE
        }
    }

    private fun startInspection() {
        _timerState.value = TimerState.INSPECTING
        _inspectionSecondsLeft.value = 15
        inspectionJob?.cancel()
        inspectionJob = viewModelScope.launch {
            for (sec in 15 downTo 1) {
                _inspectionSecondsLeft.value = sec
                if (sec == 8 || sec == 3) vibrate(50)
                delay(1000)
            }
            startTiming()
        }
    }

    private fun startTiming() {
        inspectionJob?.cancel()
        _timerState.value = TimerState.TIMING
        _elapsedTimeMillis.value = 0L
        timerStartTime = System.currentTimeMillis()
        vibrate(25)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value == TimerState.TIMING) {
                _elapsedTimeMillis.value = System.currentTimeMillis() - timerStartTime
                delay(16)
            }
        }
    }

    fun stopTimer() {
        if (_timerState.value == TimerState.TIMING) {
            timerJob?.cancel()
            val finalTime = System.currentTimeMillis() - timerStartTime
            _elapsedTimeMillis.value = finalTime
            _timerState.value = TimerState.STOPPED
            vibrate(60)

            val scramble = _currentScramble.value
            val cube = _selectedTimerCube.value
            viewModelScope.launch {
                repository.saveSolve(
                    timeMillis = finalTime,
                    scramble = scramble,
                    cubeType = cube
                )
                newScramble()
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        inspectionJob?.cancel()
        _timerState.value = TimerState.IDLE
        _elapsedTimeMillis.value = 0L
        newScramble()
    }

    fun toggleInspectionMode() {
        _isInspectionEnabled.value = !_isInspectionEnabled.value
    }

    fun deleteSolve(id: Long) {
        viewModelScope.launch {
            repository.deleteSolve(id)
        }
    }

    fun newScramble() {
        _currentScramble.value = if (_selectedTimerCube.value == "3x3") {
            CubeState3x3.generate3x3Scramble()
        } else {
            CubeState2x2.generate2x2Scramble()
        }
    }

    private fun vibrate(durationMillis: Long) {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) {}
    }
}
