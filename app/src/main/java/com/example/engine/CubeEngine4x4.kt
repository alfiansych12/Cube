package com.example.engine

import kotlin.math.PI

class CubeState4x4 private constructor(val cubies: List<Cubie>) {

    fun copy(): CubeState4x4 {
        return CubeState4x4(cubies.map { it.copy() })
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
            if (colorsOnFace.size != 16) return false
            val first = colorsOnFace[0]
            if (colorsOnFace.any { it != first }) return false
        }
        return true
    }

    companion object {
        private const val HALF_PI = (PI / 2.0).toFloat()

        fun createSolved(): CubeState4x4 {
            val cubies = mutableListOf<Cubie>()
            var idCounter = 0

            val coords = listOf(-1.5f, -0.5f, 0.5f, 1.5f)
            for (x in coords) {
                for (y in coords) {
                    for (z in coords) {
                        // Skip fully hidden internal core cubies
                        if (kotlin.math.abs(x) < 1.0f && kotlin.math.abs(y) < 1.0f && kotlin.math.abs(z) < 1.0f) continue

                        val pos = Vector3D(x, y, z)
                        val stickers = mutableListOf<Sticker>()

                        // Up (+Y) = Yellow / Down (-Y) = White
                        if (y > 1.0f) {
                            stickers.add(Sticker(Vector3D(0f, 1f, 0f), Vector3D(0f, 1f, 0f), RubikColor.YELLOW))
                        } else if (y < -1.0f) {
                            stickers.add(Sticker(Vector3D(0f, -1f, 0f), Vector3D(0f, -1f, 0f), RubikColor.WHITE))
                        }

                        // Right (+X) = Red / Left (-X) = Orange
                        if (x > 1.0f) {
                            stickers.add(Sticker(Vector3D(1f, 0f, 0f), Vector3D(1f, 0f, 0f), RubikColor.RED))
                        } else if (x < -1.0f) {
                            stickers.add(Sticker(Vector3D(-1f, 0f, 0f), Vector3D(-1f, 0f, 0f), RubikColor.ORANGE))
                        }

                        // Front (+Z) = Green / Back (-Z) = Blue
                        if (z > 1.0f) {
                            stickers.add(Sticker(Vector3D(0f, 0f, 1f), Vector3D(0f, 0f, 1f), RubikColor.GREEN))
                        } else if (z < -1.0f) {
                            stickers.add(Sticker(Vector3D(0f, 0f, -1f), Vector3D(0f, 0f, -1f), RubikColor.BLUE))
                        }

                        cubies.add(Cubie(id = idCounter++, position = pos, stickers = stickers))
                    }
                }
            }
            return CubeState4x4(cubies)
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
                "U" -> TurnSpec(TurnAxis.Y, { it.y > 1.0f }, angle)
                "D" -> TurnSpec(TurnAxis.Y, { it.y < -1.0f }, -angle)
                "R" -> TurnSpec(TurnAxis.X, { it.x > 1.0f }, angle)
                "L" -> TurnSpec(TurnAxis.X, { it.x < -1.0f }, -angle)
                "F" -> TurnSpec(TurnAxis.Z, { it.z > 1.0f }, angle)
                "B" -> TurnSpec(TurnAxis.Z, { it.z < -1.0f }, -angle)

                // Wide moves (2 layers)
                "Rw", "r" -> TurnSpec(TurnAxis.X, { it.x > 0.0f }, angle)
                "Lw", "l" -> TurnSpec(TurnAxis.X, { it.x < 0.0f }, -angle)
                "Uw", "u" -> TurnSpec(TurnAxis.Y, { it.y > 0.0f }, angle)
                "Dw", "d" -> TurnSpec(TurnAxis.Y, { it.y < 0.0f }, -angle)
                "Fw", "f" -> TurnSpec(TurnAxis.Z, { it.z > 0.0f }, angle)
                "Bw", "b" -> TurnSpec(TurnAxis.Z, { it.z < 0.0f }, -angle)

                // Whole cube rotations
                "x" -> TurnSpec(TurnAxis.X, { true }, angle)
                "y" -> TurnSpec(TurnAxis.Y, { true }, angle)
                "z" -> TurnSpec(TurnAxis.Z, { true }, angle)

                else -> null
            }
        }
    }
}
