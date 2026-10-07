package com.example.engine

import kotlin.math.PI

data class Sticker(
    val baseNormal: Vector3D,
    var currentNormal: Vector3D,
    val color: RubikColor
)

data class Cubie(
    val id: Int,
    var position: Vector3D,
    val stickers: List<Sticker>
) {
    fun copy(): Cubie {
        return Cubie(
            id = id,
            position = position,
            stickers = stickers.map { it.copy() }
        )
    }
}

enum class TurnAxis { X, Y, Z }

data class TurnSpec(
    val axis: TurnAxis,
    val sliceFilter: (Vector3D) -> Boolean,
    val angleRadians: Float
)

class CubeState2x2 private constructor(val cubies: List<Cubie>) {

    fun copy(): CubeState2x2 {
        return CubeState2x2(cubies.map { it.copy() })
    }

    /**
     * Executes a single turn notation (e.g. "R", "U'", "F2", "x")
     */
    fun applyMove(move: String) {
        val trimmed = move.trim()
        if (trimmed.isEmpty()) return

        val turns = parseMove(trimmed)
        for (turn in turns) {
            rotateSlice(turn)
        }
    }

    /**
     * Executes a sequence of space-separated moves (e.g. "R U R' U' R' F R2 U' R'")
     */
    fun applyAlgorithm(algorithm: String) {
        val tokens = algorithm.split("\\s+".toRegex()).filter { it.isNotBlank() }
        for (token in tokens) {
            applyMove(token)
        }
    }

    /**
     * Rotates all cubies matching sliceFilter around the given axis.
     */
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
        // For each of the 6 outer directions (+X, -X, +Y, -Y, +Z, -Z), all 4 visible stickers must have identical color
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
            if (colorsOnFace.size != 4) return false
            val first = colorsOnFace[0]
            if (colorsOnFace.any { it != first }) return false
        }
        return true
    }

    companion object {
        private const val HALF_PI = (PI / 2.0).toFloat()
        private const val PI_FLOAT = PI.toFloat()

        fun createSolved(): CubeState2x2 {
            val cubies = mutableListOf<Cubie>()
            var idCounter = 0

            val coords = listOf(-0.5f, 0.5f)
            for (x in coords) {
                for (y in coords) {
                    for (z in coords) {
                        val pos = Vector3D(x, y, z)
                        val stickers = mutableListOf<Sticker>()

                        // Up (+Y) / Down (-Y)
                        if (y > 0) {
                            stickers.add(Sticker(Vector3D(0f, 1f, 0f), Vector3D(0f, 1f, 0f), RubikColor.YELLOW))
                        } else {
                            stickers.add(Sticker(Vector3D(0f, -1f, 0f), Vector3D(0f, -1f, 0f), RubikColor.WHITE))
                        }

                        // Right (+X) / Left (-X)
                        if (x > 0) {
                            stickers.add(Sticker(Vector3D(1f, 0f, 0f), Vector3D(1f, 0f, 0f), RubikColor.ORANGE))
                        } else {
                            stickers.add(Sticker(Vector3D(-1f, 0f, 0f), Vector3D(-1f, 0f, 0f), RubikColor.RED))
                        }

                        // Front (+Z) / Back (-Z)
                        if (z > 0) {
                            stickers.add(Sticker(Vector3D(0f, 0f, 1f), Vector3D(0f, 0f, 1f), RubikColor.GREEN))
                        } else {
                            stickers.add(Sticker(Vector3D(0f, 0f, -1f), Vector3D(0f, 0f, -1f), RubikColor.BLUE))
                        }

                        cubies.add(Cubie(id = idCounter++, position = pos, stickers = stickers))
                    }
                }
            }
            return CubeState2x2(cubies)
        }

        fun parseMove(token: String): List<TurnSpec> {
            val base = token.replace("'", "").replace("2", "").trim()
            val isPrime = token.contains("'")
            val isDouble = token.contains("2")

            val singleTurn = getBaseTurn(base, isPrime, isDouble) ?: return emptyList()
            return listOf(singleTurn)
        }

        fun getBaseTurn(base: String, isPrime: Boolean, isDouble: Boolean = false): TurnSpec? {
            val angleMultiplier = if (isDouble) PI_FLOAT else HALF_PI
            val sign = if (isPrime) 1f else -1f
            val angle = sign * angleMultiplier
            return when (base) {
                // U: rotate Y around negative direction for clockwise
                "U" -> TurnSpec(TurnAxis.Y, { it.y > 0.1f }, angle)
                "D" -> TurnSpec(TurnAxis.Y, { it.y < -0.1f }, -angle)

                // R: rotate X around negative direction for clockwise
                "R" -> TurnSpec(TurnAxis.X, { it.x > 0.1f }, angle)
                "L" -> TurnSpec(TurnAxis.X, { it.x < -0.1f }, -angle)

                // F: rotate Z around negative direction for clockwise
                "F" -> TurnSpec(TurnAxis.Z, { it.z > 0.1f }, angle)
                "B" -> TurnSpec(TurnAxis.Z, { it.z < -0.1f }, -angle)

                // Whole cube rotations
                "x" -> TurnSpec(TurnAxis.X, { true }, angle)
                "y" -> TurnSpec(TurnAxis.Y, { true }, angle)
                "z" -> TurnSpec(TurnAxis.Z, { true }, angle)

                else -> null
            }
        }

        /**
         * Calculates the inverse of an algorithm string.
         * Example: "R U R' U'" -> "U R U' R'"
         */
        fun invertAlgorithm(algorithm: String): String {
            val tokens = algorithm.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val inverted = tokens.asReversed().map { token ->
                when {
                    token.endsWith("2") -> token // R2 inverse is R2
                    token.endsWith("'") -> token.dropLast(1) // R' inverse is R
                    else -> "$token'" // R inverse is R'
                }
            }
            return inverted.joinToString(" ")
        }

        /**
         * Generates a random WCA-style scramble for 2x2 (9-11 moves).
         */
        fun generate2x2Scramble(): String {
            val faces = listOf("R", "U", "F")
            val modifiers = listOf("", "'", "2")
            val scrambleMoves = mutableListOf<String>()
            var lastFace = ""

            val length = 9 + (0..2).random()
            for (i in 0 until length) {
                val availableFaces = faces.filter { it != lastFace }
                val face = availableFaces.random()
                val mod = modifiers.random()
                scrambleMoves.add("$face$mod")
                lastFace = face
            }
            return scrambleMoves.joinToString(" ")
        }
    }
}
