package com.example.engine

import kotlin.math.PI

class CubeState3x3 private constructor(val cubies: List<Cubie>) {

    fun copy(): CubeState3x3 {
        return CubeState3x3(cubies.map { it.copy() })
    }

    fun applyMove(move: String) {
        val trimmed = move.trim()
        if (trimmed.isEmpty()) return

        val turns = parseMove(trimmed)
        for (turn in turns) {
            rotateSlice(turn)
        }
    }

    fun applyAlgorithm(algorithm: String) {
        val tokens = algorithm.split("\\s+".toRegex()).filter { it.isNotBlank() }
        for (token in tokens) {
            applyMove(token)
        }
    }

    fun rotateSlice(turn: TurnSpec) {
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

    fun isSolved(): Boolean {
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
            if (colorsOnFace.size != 9) return false
            val first = colorsOnFace[0]
            if (colorsOnFace.any { it != first }) return false
        }
        return true
    }

    companion object {
        private const val HALF_PI = (PI / 2.0).toFloat()

        fun createSolved(): CubeState3x3 {
            val cubies = mutableListOf<Cubie>()
            var idCounter = 0

            val coords = listOf(-1f, 0f, 1f)
            for (x in coords) {
                for (y in coords) {
                    for (z in coords) {
                        // Skip the inner-most hidden core cubie (0, 0, 0)
                        if (x == 0f && y == 0f && z == 0f) continue

                        val pos = Vector3D(x, y, z)
                        val stickers = mutableListOf<Sticker>()

                        // Up (+Y) / Down (-Y)
                        if (y > 0.5f) {
                            stickers.add(Sticker(Vector3D(0f, 1f, 0f), Vector3D(0f, 1f, 0f), RubikColor.YELLOW))
                        } else if (y < -0.5f) {
                            stickers.add(Sticker(Vector3D(0f, -1f, 0f), Vector3D(0f, -1f, 0f), RubikColor.WHITE))
                        }

                        // Right (+X) / Left (-X)
                        if (x > 0.5f) {
                            stickers.add(Sticker(Vector3D(1f, 0f, 0f), Vector3D(1f, 0f, 0f), RubikColor.RED))
                        } else if (x < -0.5f) {
                            stickers.add(Sticker(Vector3D(-1f, 0f, 0f), Vector3D(-1f, 0f, 0f), RubikColor.ORANGE))
                        }

                        // Front (+Z) / Back (-Z)
                        if (z > 0.5f) {
                            stickers.add(Sticker(Vector3D(0f, 0f, 1f), Vector3D(0f, 0f, 1f), RubikColor.GREEN))
                        } else if (z < -0.5f) {
                            stickers.add(Sticker(Vector3D(0f, 0f, -1f), Vector3D(0f, 0f, -1f), RubikColor.BLUE))
                        }

                        cubies.add(Cubie(id = idCounter++, position = pos, stickers = stickers))
                    }
                }
            }
            return CubeState3x3(cubies)
        }

        private const val PI_FLOAT = PI.toFloat()

        fun parseMove(token: String): List<TurnSpec> {
            val isDouble = token.contains("2")
            val isPrime = token.contains("'")
            val base = token.replace("'", "").replace("2", "").trim()

            val singleTurn = getBaseTurn(base, isPrime, isDouble) ?: return emptyList()
            return listOf(singleTurn)
        }

        fun getBaseTurn(base: String, isPrime: Boolean, isDouble: Boolean = false): TurnSpec? {
            val angleMultiplier = if (isDouble) PI_FLOAT else HALF_PI
            val sign = if (isPrime) 1f else -1f
            val angle = sign * angleMultiplier
            return when (base) {
                // Outer faces
                "U" -> TurnSpec(TurnAxis.Y, { it.y > 0.5f }, angle)
                "D" -> TurnSpec(TurnAxis.Y, { it.y < -0.5f }, -angle)

                "R" -> TurnSpec(TurnAxis.X, { it.x > 0.5f }, angle)
                "L" -> TurnSpec(TurnAxis.X, { it.x < -0.5f }, -angle)

                "F" -> TurnSpec(TurnAxis.Z, { it.z > 0.5f }, angle)
                "B" -> TurnSpec(TurnAxis.Z, { it.z < -0.5f }, -angle)

                // Middle slice moves
                // M rotates in direction of L
                "M" -> TurnSpec(TurnAxis.X, { it.x > -0.5f && it.x < 0.5f }, -angle)
                // E rotates in direction of D
                "E" -> TurnSpec(TurnAxis.Y, { it.y > -0.5f && it.y < 0.5f }, -angle)
                // S rotates in direction of F
                "S" -> TurnSpec(TurnAxis.Z, { it.z > -0.5f && it.z < 0.5f }, angle)

                // Wide moves
                "Rw", "r" -> TurnSpec(TurnAxis.X, { it.x > -0.5f }, angle)
                "Lw", "l" -> TurnSpec(TurnAxis.X, { it.x < 0.5f }, -angle)
                "Uw", "u" -> TurnSpec(TurnAxis.Y, { it.y > 0.5f }, angle)
                "Dw", "d" -> TurnSpec(TurnAxis.Y, { it.y < 0.5f }, -angle)
                "Fw", "f" -> TurnSpec(TurnAxis.Z, { it.z > 0.5f }, angle)
                "Bw", "b" -> TurnSpec(TurnAxis.Z, { it.z < 0.5f }, -angle)

                // Whole cube rotations
                "x" -> TurnSpec(TurnAxis.X, { true }, angle)
                "y" -> TurnSpec(TurnAxis.Y, { true }, angle)
                "z" -> TurnSpec(TurnAxis.Z, { true }, angle)

                else -> null
            }
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

        fun generate3x3Scramble(): String {
            val faces = listOf("R", "L", "U", "D", "F", "B")
            val modifiers = listOf("", "'", "2")
            val scrambleMoves = mutableListOf<String>()
            var lastFace = ""
            var secondLastFace = ""

            val length = 20 + (0..2).random()
            for (i in 0 until length) {
                val availableFaces = faces.filter { it != lastFace && it != secondLastFace }
                val face = availableFaces.random()
                val mod = modifiers.random()
                scrambleMoves.add("$face$mod")
                secondLastFace = lastFace
                lastFace = face
            }
            return scrambleMoves.joinToString(" ")
        }
    }
}
