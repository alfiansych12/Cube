package com.example.engine

import com.example.data.model.AlgorithmItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaybackState(
    val cubies: List<Cubie>,
    val cubeSize: Int = 2,
    val moves: List<String> = emptyList(),
    val currentStepIndex: Int = -1,
    val isPlaying: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val activeTurn: TurnSpec? = null,
    val turnProgress: Float = 0f,
    val isSolved: Boolean = false,
    val currentMoveName: String = "",
    val currentExplanation: String = "",
    val currentFingerTrick: String = ""
)

class CubePlaybackEngine(
    private val scope: CoroutineScope,
    private var defaultCubeSize: Int = 2
) {

    private var activeAlgorithm: AlgorithmItem? = null
    private var currentCubeSize: Int = defaultCubeSize
    private var currentCubies: List<Cubie> = createSolved(defaultCubeSize)
    private var playbackJob: Job? = null

    private val _state = MutableStateFlow(
        PlaybackState(
            cubies = currentCubies.map { it.copy() },
            cubeSize = currentCubeSize,
            isSolved = isSolvedState(currentCubies, currentCubeSize)
        )
    )
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun setCubeSize(size: Int) {
        playbackJob?.cancel()
        currentCubeSize = size
        resetToSolved()
    }

    fun loadAlgorithm(algorithm: AlgorithmItem, cubeSize: Int = if (algorithm.id.startsWith("3x3")) 3 else 2) {
        playbackJob?.cancel()
        activeAlgorithm = algorithm
        currentCubeSize = cubeSize

        val moves = algorithm.notation.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val setupString = if (algorithm.setupMoves.isNotBlank()) {
            algorithm.setupMoves
        } else {
            invertAlgorithm(algorithm.notation)
        }

        val fresh = createSolved(currentCubeSize)
        applyAlgorithmToCubies(fresh, setupString, currentCubeSize)
        currentCubies = fresh

        _state.value = PlaybackState(
            cubies = currentCubies.map { it.copy() },
            cubeSize = currentCubeSize,
            moves = moves,
            currentStepIndex = -1,
            isPlaying = false,
            speedMultiplier = _state.value.speedMultiplier,
            isSolved = isSolvedState(currentCubies, currentCubeSize),
            currentMoveName = if (moves.isNotEmpty()) moves[0] else "",
            currentExplanation = if (moves.isNotEmpty()) getMoveExplanation(moves[0]) else "",
            currentFingerTrick = if (moves.isNotEmpty()) getFingerTrick(moves[0]) else ""
        )
    }

    fun resetToStart() {
        playbackJob?.cancel()
        val algo = activeAlgorithm
        if (algo != null) {
            loadAlgorithm(algo, currentCubeSize)
        } else {
            resetToSolved()
        }
    }

    fun resetToSolved() {
        playbackJob?.cancel()
        currentCubies = createSolved(currentCubeSize)
        _state.value = _state.value.copy(
            cubies = currentCubies.map { it.copy() },
            cubeSize = currentCubeSize,
            currentStepIndex = -1,
            isPlaying = false,
            isSolved = true,
            activeTurn = null,
            turnProgress = 0f
        )
    }

    fun stepForward() {
        if (_state.value.isPlaying) return
        val moves = _state.value.moves
        val nextIndex = _state.value.currentStepIndex + 1
        if (nextIndex < moves.size) {
            scope.launch {
                executeAnimatedMove(moves[nextIndex], nextIndex)
            }
        }
    }

    fun stepBackward() {
        if (_state.value.isPlaying) return
        val currentIndex = _state.value.currentStepIndex
        if (currentIndex >= 0) {
            playbackJob?.cancel()
            val moves = _state.value.moves
            val targetIndex = currentIndex - 1

            val fresh = createSolved(currentCubeSize)
            val setupString = if (activeAlgorithm?.setupMoves?.isNotBlank() == true) {
                activeAlgorithm!!.setupMoves
            } else if (activeAlgorithm != null) {
                invertAlgorithm(activeAlgorithm!!.notation)
            } else ""

            if (setupString.isNotBlank()) {
                applyAlgorithmToCubies(fresh, setupString, currentCubeSize)
            }

            for (i in 0..targetIndex) {
                applyMoveToCubies(fresh, moves[i], currentCubeSize)
            }
            currentCubies = fresh

            val currentMove = if (targetIndex + 1 < moves.size) moves[targetIndex + 1] else ""
            _state.value = _state.value.copy(
                cubies = currentCubies.map { it.copy() },
                cubeSize = currentCubeSize,
                currentStepIndex = targetIndex,
                isPlaying = false,
                isSolved = isSolvedState(currentCubies, currentCubeSize),
                activeTurn = null,
                turnProgress = 0f,
                currentMoveName = currentMove,
                currentExplanation = if (currentMove.isNotEmpty()) getMoveExplanation(currentMove) else "",
                currentFingerTrick = if (currentMove.isNotEmpty()) getFingerTrick(currentMove) else ""
            )
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_state.value.isPlaying) return
        val moves = _state.value.moves
        if (moves.isEmpty()) return

        if (_state.value.currentStepIndex >= moves.size - 1) {
            val algo = activeAlgorithm
            if (algo != null) {
                loadAlgorithm(algo, currentCubeSize)
            }
        }

        _state.value = _state.value.copy(isPlaying = true)

        playbackJob = scope.launch {
            while (_state.value.isPlaying && _state.value.currentStepIndex < _state.value.moves.size - 1) {
                val nextIdx = _state.value.currentStepIndex + 1
                val move = _state.value.moves[nextIdx]
                executeAnimatedMove(move, nextIdx)
                delay((320 / _state.value.speedMultiplier).toLong())
            }
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun pause() {
        playbackJob?.cancel()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun setSpeedMultiplier(speed: Float) {
        _state.value = _state.value.copy(speedMultiplier = speed)
    }

    fun performFreeMove(moveNotation: String) {
        scope.launch {
            val turns = parseMove(moveNotation, currentCubeSize)
            val isDouble = moveNotation.contains("2")
            val baseDuration = if (isDouble) 340f else 240f
            val duration = (baseDuration / _state.value.speedMultiplier).toInt().coerceAtLeast(30)

            for (turn in turns) {
                animateTurn(turn, duration)
                rotateSlice(currentCubies, turn)
                _state.value = _state.value.copy(
                    cubies = currentCubies.map { it.copy() },
                    activeTurn = null,
                    turnProgress = 0f,
                    isSolved = isSolvedState(currentCubies, currentCubeSize)
                )
            }
        }
    }

    fun scrambleCube(scrambleMoves: String? = null) {
        playbackJob?.cancel()
        val scramble = scrambleMoves ?: if (currentCubeSize == 3) {
            CubeState3x3.generate3x3Scramble()
        } else {
            CubeState2x2.generate2x2Scramble()
        }

        val fresh = createSolved(currentCubeSize)
        applyAlgorithmToCubies(fresh, scramble, currentCubeSize)
        currentCubies = fresh

        _state.value = PlaybackState(
            cubies = currentCubies.map { it.copy() },
            cubeSize = currentCubeSize,
            moves = scramble.split("\\s+".toRegex()).filter { it.isNotBlank() },
            currentStepIndex = -1,
            isPlaying = false,
            speedMultiplier = _state.value.speedMultiplier,
            isSolved = isSolvedState(currentCubies, currentCubeSize)
        )
    }

    private fun speedcubeEase(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        // Quintic smootherstep with zero jerk at start and finish:
        return clamped * clamped * clamped * (clamped * (clamped * 6f - 15f) + 10f)
    }

    private suspend fun animateTurn(turn: TurnSpec, durationMillis: Int) {
        _state.value = _state.value.copy(
            activeTurn = turn,
            turnProgress = 0f
        )
        val startTime = System.nanoTime()
        val totalNanos = durationMillis.coerceAtLeast(16) * 1_000_000L
        while (true) {
            val elapsedNanos = System.nanoTime() - startTime
            val rawProgress = (elapsedNanos.toFloat() / totalNanos).coerceIn(0f, 1f)
            val easedProgress = speedcubeEase(rawProgress)
            _state.value = _state.value.copy(turnProgress = easedProgress)
            if (rawProgress >= 1f) break
            delay(8)
        }
    }

    private suspend fun executeAnimatedMove(move: String, targetStepIndex: Int) {
        val turns = parseMove(move, currentCubeSize)

        _state.value = _state.value.copy(
            currentMoveName = move,
            currentExplanation = getMoveExplanation(move),
            currentFingerTrick = getFingerTrick(move)
        )

        val isDouble = move.contains("2")
        val baseDuration = if (isDouble) 380f else 280f
        val duration = (baseDuration / _state.value.speedMultiplier).toInt().coerceAtLeast(30)

        for (turn in turns) {
            animateTurn(turn, duration)
            rotateSlice(currentCubies, turn)
            _state.value = _state.value.copy(
                cubies = currentCubies.map { it.copy() },
                activeTurn = null,
                turnProgress = 0f
            )
        }

        val nextMove = if (targetStepIndex + 1 < _state.value.moves.size) {
            _state.value.moves[targetStepIndex + 1]
        } else ""

        _state.value = _state.value.copy(
            currentStepIndex = targetStepIndex,
            isSolved = isSolvedState(currentCubies, currentCubeSize),
            currentMoveName = if (nextMove.isNotEmpty()) nextMove else move,
            currentExplanation = if (nextMove.isNotEmpty()) getMoveExplanation(nextMove) else "Algoritma Selesai!",
            currentFingerTrick = if (nextMove.isNotEmpty()) getFingerTrick(nextMove) else ""
        )
    }

    companion object {
        fun createSolved(size: Int): List<Cubie> {
            return when (size) {
                5 -> CubeState5x5.createSolved().cubies
                4 -> CubeState4x4.createSolved().cubies
                3 -> CubeState3x3.createSolved().cubies
                else -> CubeState2x2.createSolved().cubies
            }
        }

        fun parseMove(token: String, size: Int): List<TurnSpec> {
            return when (size) {
                5 -> CubeState5x5.parseMove(token)
                4 -> CubeState4x4.parseMove(token)
                3 -> CubeState3x3.parseMove(token)
                else -> CubeState2x2.parseMove(token)
            }
        }

        fun rotateSlice(cubies: List<Cubie>, turn: TurnSpec) {
            for (cubie in cubies) {
                if (turn.sliceFilter(cubie.position)) {
                    cubie.position = when (turn.axis) {
                        TurnAxis.X -> cubie.position.rotateX(turn.angleRadians)
                        TurnAxis.Y -> cubie.position.rotateY(turn.angleRadians)
                        TurnAxis.Z -> cubie.position.rotateZ(turn.angleRadians)
                    }.snapToGrid()

                    for (sticker in cubie.stickers) {
                        sticker.currentNormal = when (turn.axis) {
                            TurnAxis.X -> sticker.currentNormal.rotateX(turn.angleRadians)
                            TurnAxis.Y -> sticker.currentNormal.rotateY(turn.angleRadians)
                            TurnAxis.Z -> sticker.currentNormal.rotateZ(turn.angleRadians)
                        }.snapNormal()
                    }
                }
            }
        }

        fun applyMoveToCubies(cubies: List<Cubie>, move: String, size: Int) {
            val turns = parseMove(move.trim(), size)
            for (turn in turns) {
                rotateSlice(cubies, turn)
            }
        }

        fun applyAlgorithmToCubies(cubies: List<Cubie>, algorithm: String, size: Int) {
            val tokens = algorithm.split("\\s+".toRegex()).filter { it.isNotBlank() }
            for (token in tokens) {
                applyMoveToCubies(cubies, token, size)
            }
        }

        fun isSolvedState(cubies: List<Cubie>, size: Int): Boolean {
            val expectedPerFace = size * size
            val directions = listOf(
                Vector3D(1f, 0f, 0f),
                Vector3D(-1f, 0f, 0f),
                Vector3D(0f, 1f, 0f),
                Vector3D(0f, -1f, 0f),
                Vector3D(0f, 0f, 1f),
                Vector3D(0f, 0f, -1f)
            )

            for (dir in directions) {
                val colorsOnFace = mutableListOf<RubikColor>()
                for (cubie in cubies) {
                    for (sticker in cubie.stickers) {
                        if (sticker.currentNormal.dot(dir) > 0.8f) {
                            colorsOnFace.add(sticker.color)
                        }
                    }
                }
                if (colorsOnFace.size != expectedPerFace) return false
                val first = colorsOnFace[0]
                if (colorsOnFace.any { it != first }) return false
            }
            return true
        }

        fun invertAlgorithm(algorithm: String): String {
            val tokens = algorithm.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val inverted = tokens.asReversed().map { token ->
                when {
                    token.endsWith("2") -> token
                    token.endsWith("'") -> token.dropLast(1)
                    else -> "$token'"
                }
            }
            return inverted.joinToString(" ")
        }

        fun getMoveExplanation(move: String): String {
            return when (move) {
                "R" -> "Putar sisi Kanan (Right) 90° searah jarum jam (ke atas)"
                "R'" -> "Putar sisi Kanan (Right) 90° berlawanan arah jarum jam (ke bawah)"
                "R2" -> "Putar sisi Kanan (Right) 180°"
                "U" -> "Putar sisi Atas (Up) 90° searah jarum jam (ke kiri)"
                "U'" -> "Putar sisi Atas (Up) 90° berlawanan arah jarum jam (ke kanan)"
                "U2" -> "Putar sisi Atas (Up) 180°"
                "F" -> "Putar sisi Depan (Front) 90° searah jarum jam"
                "F'" -> "Putar sisi Depan (Front) 90° berlawanan arah jarum jam"
                "F2" -> "Putar sisi Depan (Front) 180°"
                "L" -> "Putar sisi Kiri (Left) 90° searah jarum jam (ke bawah)"
                "L'" -> "Putar sisi Kiri (Left) 90° berlawanan arah jarum jam (ke atas)"
                "L2" -> "Putar sisi Kiri (Left) 180°"
                "B" -> "Putar sisi Belakang (Back) 90° searah jarum jam"
                "B'" -> "Putar sisi Belakang (Back) 90° berlawanan arah jarum jam"
                "B2" -> "Putar sisi Belakang (Back) 180°"
                "D" -> "Putar sisi Bawah (Down) 90° searah jarum jam"
                "D'" -> "Putar sisi Bawah (Down) 90° berlawanan arah jarum jam"
                "D2" -> "Putar sisi Bawah (Down) 180°"
                "M" -> "Putar layer tengah (Middle) ke bawah (searah L)"
                "M'" -> "Putar layer tengah (Middle) ke atas (searah R)"
                "M2" -> "Putar layer tengah (Middle) 180°"
                "Rw", "r" -> "Putar 2 layer kanan sekaligus ke atas"
                "Rw'", "r'" -> "Putar 2 layer kanan sekaligus ke bawah"
                "Lw", "l" -> "Putar 2 layer kiri sekaligus ke bawah"
                "Uw", "u" -> "Putar 2 layer atas sekaligus ke kiri"
                "x" -> "Rotasi seluruh kubus mengikuti arah R"
                "y" -> "Rotasi seluruh kubus mengikuti arah U"
                "z" -> "Rotasi seluruh kubus mengikuti arah F"
                else -> "Putar gerakan $move"
            }
        }

        fun getFingerTrick(move: String): String {
            return when (move) {
                "R" -> "Dorong layer kanan ke atas dengan pergelangan tangan kanan."
                "R'" -> "Tarik layer kanan ke bawah dengan pergelangan tangan kanan."
                "U" -> "Jentikkan layer atas ke kiri menggunakan jari telunjuk tangan kanan."
                "U'" -> "Jentikkan layer atas ke kanan menggunakan jari telunjuk tangan kiri."
                "U2" -> "Gunakan kombinasi double flick: telunjuk kanan diikuti jari tengah kanan."
                "F" -> "Dorong layer depan ke bawah menggunakan telunjuk kanan."
                "F'" -> "Dorong layer depan ke atas menggunakan jempol kanan."
                "M'" -> "Dorong layer tengah M dari bawah ke atas menggunakan jari manis kiri/kanan."
                "M2" -> "Double flick layer tengah M menggunakan jari manis dan jari tengah dari bawah."
                "R2" -> "Putar layer kanan 180° dalam satu gerakan halus pergelangan tangan."
                "F2" -> "Gunakan dua jari untuk memutar layer depan 180°."
                "B2" -> "Gunakan jari manis dan tengah di sisi belakang untuk memutar 180°."
                else -> "Pastikan pegangan kubus tetap stabil di kedua tangan."
            }
        }
    }
}
