package com.example.scanner

import com.example.data.model.AlgorithmItem
import com.example.engine.CubePlaybackEngine
import com.example.engine.CubeState3x3
import com.example.engine.RubikColor
import kotlin.math.roundToInt

data class OLLPattern(
    // 3x3 grid for Up face (indices 0..8): row 0: 0,1,2 (Back); row 1: 3,4,5 (Center); row 2: 6,7,8 (Front)
    val topFace: List<Boolean>,
    // 3 upper stickers on Front, Right, Back, Left faces
    val frontTop: List<Boolean>,
    val rightTop: List<Boolean>,
    val backTop: List<Boolean>,
    val leftTop: List<Boolean>
) {
    /**
     * Rotates this OLL pattern clockwise by 90 degrees around the Y axis (equivalent to U turn).
     */
    fun rotateClockwise(): OLLPattern {
        val newTop = listOf(
            topFace[6], topFace[3], topFace[0],
            topFace[7], topFace[4], topFace[1],
            topFace[8], topFace[5], topFace[2]
        )

        return OLLPattern(
            topFace = newTop,
            frontTop = leftTop,
            rightTop = frontTop,
            backTop = rightTop,
            leftTop = backTop
        )
    }
}

data class OLLMatchResult(
    val algorithm: AlgorithmItem,
    val confidencePercent: Int,
    val rotationIndex: Int, // 0: 0°, 1: 90° CW, 2: 180°, 3: 270° CW
    val isExactMatch: Boolean,
    val expectedPattern: OLLPattern,
    val matchedScannedPattern: OLLPattern
)

object RubikOLLMatcher {

    /**
     * Extracts an OLLPattern from the 6 scanned faces of a 3x3 Rubik's cube.
     */
    fun extractPatternFromScannedFaces(
        scannedFaces: Map<CubeFace, List<RubikColor>>
    ): OLLPattern {
        val upFace = scannedFaces[CubeFace.UP] ?: CubeFace.defaultFaceColors(CubeFace.UP)
        val frontFace = scannedFaces[CubeFace.FRONT] ?: CubeFace.defaultFaceColors(CubeFace.FRONT)
        val rightFace = scannedFaces[CubeFace.RIGHT] ?: CubeFace.defaultFaceColors(CubeFace.RIGHT)
        val backFace = scannedFaces[CubeFace.BACK] ?: CubeFace.defaultFaceColors(CubeFace.BACK)
        val leftFace = scannedFaces[CubeFace.LEFT] ?: CubeFace.defaultFaceColors(CubeFace.LEFT)

        val topPattern = upFace.map { it == RubikColor.YELLOW }

        val fTop = listOf(frontFace[0], frontFace[1], frontFace[2]).map { it == RubikColor.YELLOW }
        val rTop = listOf(rightFace[0], rightFace[1], rightFace[2]).map { it == RubikColor.YELLOW }
        val bTop = listOf(backFace[0], backFace[1], backFace[2]).map { it == RubikColor.YELLOW }
        val lTop = listOf(leftFace[0], leftFace[1], leftFace[2]).map { it == RubikColor.YELLOW }

        return OLLPattern(
            topFace = topPattern,
            frontTop = fTop,
            rightTop = rTop,
            backTop = bTop,
            leftTop = lTop
        )
    }

    private fun reverseAlgorithm(formula: String): String {
        return formula.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.reversed().joinToString(" ") { token ->
            when {
                token.endsWith("2") -> token
                token.endsWith("'") -> token.removeSuffix("'")
                else -> "$token'"
            }
        }
    }

    /**
     * Extracts the simulated OLL pattern for an algorithm by executing its setup moves on a solved 3x3 cube.
     */
    fun extractPatternFromAlgorithm(algorithm: AlgorithmItem): OLLPattern {
        val cube = CubeState3x3.createSolved()

        val setup = algorithm.setupMoves.ifBlank {
            reverseAlgorithm(algorithm.notation)
        }

        if (setup.isNotBlank()) {
            val tokens = setup.split(" ").filter { it.isNotBlank() }
            for (token in tokens) {
                cube.applyMove(token)
            }
        }

        val cubies = cube.cubies

        val topStickers = mutableListOf<Boolean>()
        for (rowZ in listOf(-1f, 0f, 1f)) {
            for (colX in listOf(-1f, 0f, 1f)) {
                val cubie = cubies.find {
                    kotlin.math.abs(it.position.x - colX) < 0.3f &&
                    it.position.y > 0.5f &&
                    kotlin.math.abs(it.position.z - rowZ) < 0.3f
                }
                val sticker = cubie?.stickers?.find { it.currentNormal.y > 0.5f }
                topStickers.add(sticker?.color == RubikColor.YELLOW)
            }
        }

        // Side upper stickers (y = 1f):
        // Front (z = 1f, normal +Z): x in -1, 0, 1
        val fTop = listOf(-1f, 0f, 1f).map { colX ->
            val cubie = cubies.find {
                kotlin.math.abs(it.position.x - colX) < 0.3f &&
                it.position.y > 0.5f &&
                it.position.z > 0.5f
            }
            cubie?.stickers?.find { it.currentNormal.z > 0.5f }?.color == RubikColor.YELLOW
        }

        // Right (x = 1f, normal +X): z in 1 (Front-Right), 0, -1 (Back-Right)
        val rTop = listOf(1f, 0f, -1f).map { rowZ ->
            val cubie = cubies.find {
                it.position.x > 0.5f &&
                it.position.y > 0.5f &&
                kotlin.math.abs(it.position.z - rowZ) < 0.3f
            }
            cubie?.stickers?.find { it.currentNormal.x > 0.5f }?.color == RubikColor.YELLOW
        }

        // Back (z = -1f, normal -Z): x in 1 (Back-Right), 0, -1 (Back-Left)
        val bTop = listOf(1f, 0f, -1f).map { colX ->
            val cubie = cubies.find {
                kotlin.math.abs(it.position.x - colX) < 0.3f &&
                it.position.y > 0.5f &&
                it.position.z < -0.5f
            }
            cubie?.stickers?.find { it.currentNormal.z < -0.5f }?.color == RubikColor.YELLOW
        }

        // Left (x = -1f, normal -X): z in -1 (Back-Left), 0, 1 (Front-Left)
        val lTop = listOf(-1f, 0f, 1f).map { rowZ ->
            val cubie = cubies.find {
                it.position.x < -0.5f &&
                it.position.y > 0.5f &&
                kotlin.math.abs(it.position.z - rowZ) < 0.3f
            }
            cubie?.stickers?.find { it.currentNormal.x < -0.5f }?.color == RubikColor.YELLOW
        }

        return OLLPattern(
            topFace = topStickers,
            frontTop = fTop,
            rightTop = rTop,
            backTop = bTop,
            leftTop = lTop
        )
    }

    /**
     * Calculates similarity percentage between two OLL patterns (0 to 100%).
     */
    fun calculateSimilarity(p1: OLLPattern, p2: OLLPattern): Int {
        var matches = 0
        var total = 21 // 9 top + 3*4 side rim stickers

        for (i in 0 until 9) {
            if (p1.topFace.getOrElse(i) { false } == p2.topFace.getOrElse(i) { false }) {
                matches++
            }
        }
        for (i in 0 until 3) {
            if (p1.frontTop.getOrElse(i) { false } == p2.frontTop.getOrElse(i) { false }) matches++
            if (p1.rightTop.getOrElse(i) { false } == p2.rightTop.getOrElse(i) { false }) matches++
            if (p1.backTop.getOrElse(i) { false } == p2.backTop.getOrElse(i) { false }) matches++
            if (p1.leftTop.getOrElse(i) { false } == p2.leftTop.getOrElse(i) { false }) matches++
        }

        return ((matches.toFloat() / total.toFloat()) * 100f).roundToInt()
    }

    /**
     * Matches scanned cube state against all available OLL algorithms in all 4 rotations.
     * Returns a sorted list of matches, highest confidence first.
     */
    fun matchOLL(
        scannedFaces: Map<CubeFace, List<RubikColor>>,
        ollAlgorithms: List<AlgorithmItem>
    ): List<OLLMatchResult> {
        val rawPattern = extractPatternFromScannedFaces(scannedFaces)

        // 4 rotations of the scanned pattern
        val rotations = mutableListOf<OLLPattern>()
        var currentRot = rawPattern
        for (r in 0 until 4) {
            rotations.add(currentRot)
            currentRot = currentRot.rotateClockwise()
        }

        val results = mutableListOf<OLLMatchResult>()

        for (algo in ollAlgorithms) {
            val expected = extractPatternFromAlgorithm(algo)

            var bestConfidence = -1
            var bestRotIndex = 0
            var bestPattern = rotations[0]

            for (r in 0 until 4) {
                val rotPattern = rotations[r]
                val score = calculateSimilarity(rotPattern, expected)
                if (score > bestConfidence) {
                    bestConfidence = score
                    bestRotIndex = r
                    bestPattern = rotPattern
                }
            }

            results.add(
                OLLMatchResult(
                    algorithm = algo,
                    confidencePercent = bestConfidence,
                    rotationIndex = bestRotIndex,
                    isExactMatch = bestConfidence == 100,
                    expectedPattern = expected,
                    matchedScannedPattern = bestPattern
                )
            )
        }

        return results.sortedWith(
            compareByDescending<OLLMatchResult> { it.confidencePercent }
                .thenBy { it.algorithm.id }
        )
    }

    /**
     * Generates a 6-face map corresponding to an algorithm setup moves.
     */
    fun generateFacesFromAlgorithm(algorithm: AlgorithmItem): Map<CubeFace, List<RubikColor>> {
        val cube = CubeState3x3.createSolved()
        val setup = algorithm.setupMoves.ifBlank { reverseAlgorithm(algorithm.notation) }
        if (setup.isNotBlank()) {
            cube.applyAlgorithm(setup)
        }

        val cubies = cube.cubies
        val faces = mutableMapOf<CubeFace, MutableList<RubikColor>>()

        // UP: y = 1f, rowZ: -1..1, colX: -1..1
        val upList = mutableListOf<RubikColor>()
        for (z in listOf(-1f, 0f, 1f)) {
            for (x in listOf(-1f, 0f, 1f)) {
                val c = cubies.find { it.position.y > 0.5f && kotlin.math.abs(it.position.x - x) < 0.3f && kotlin.math.abs(it.position.z - z) < 0.3f }
                upList.add(c?.stickers?.find { it.currentNormal.y > 0.5f }?.color ?: RubikColor.YELLOW)
            }
        }
        faces[CubeFace.UP] = upList

        // FRONT: z = 1f, rowY: 1..-1, colX: -1..1
        val frontList = mutableListOf<RubikColor>()
        for (y in listOf(1f, 0f, -1f)) {
            for (x in listOf(-1f, 0f, 1f)) {
                val c = cubies.find { it.position.z > 0.5f && kotlin.math.abs(it.position.x - x) < 0.3f && kotlin.math.abs(it.position.y - y) < 0.3f }
                frontList.add(c?.stickers?.find { it.currentNormal.z > 0.5f }?.color ?: RubikColor.GREEN)
            }
        }
        faces[CubeFace.FRONT] = frontList

        // RIGHT: x = 1f, rowY: 1..-1, colZ: 1..-1
        val rightList = mutableListOf<RubikColor>()
        for (y in listOf(1f, 0f, -1f)) {
            for (z in listOf(1f, 0f, -1f)) {
                val c = cubies.find { it.position.x > 0.5f && kotlin.math.abs(it.position.z - z) < 0.3f && kotlin.math.abs(it.position.y - y) < 0.3f }
                rightList.add(c?.stickers?.find { it.currentNormal.x > 0.5f }?.color ?: RubikColor.ORANGE)
            }
        }
        faces[CubeFace.RIGHT] = rightList

        // BACK: z = -1f, rowY: 1..-1, colX: 1..-1
        val backList = mutableListOf<RubikColor>()
        for (y in listOf(1f, 0f, -1f)) {
            for (x in listOf(1f, 0f, -1f)) {
                val c = cubies.find { it.position.z < -0.5f && kotlin.math.abs(it.position.x - x) < 0.3f && kotlin.math.abs(it.position.y - y) < 0.3f }
                backList.add(c?.stickers?.find { it.currentNormal.z < -0.5f }?.color ?: RubikColor.BLUE)
            }
        }
        faces[CubeFace.BACK] = backList

        // LEFT: x = -1f, rowY: 1..-1, colZ: -1..1
        val leftList = mutableListOf<RubikColor>()
        for (y in listOf(1f, 0f, -1f)) {
            for (z in listOf(-1f, 0f, 1f)) {
                val c = cubies.find { it.position.x < -0.5f && kotlin.math.abs(it.position.z - z) < 0.3f && kotlin.math.abs(it.position.y - y) < 0.3f }
                leftList.add(c?.stickers?.find { it.currentNormal.x < -0.5f }?.color ?: RubikColor.RED)
            }
        }
        faces[CubeFace.LEFT] = leftList

        // DOWN: y = -1f, rowZ: 1..-1, colX: -1..1
        val downList = mutableListOf<RubikColor>()
        for (z in listOf(1f, 0f, -1f)) {
            for (x in listOf(-1f, 0f, 1f)) {
                val c = cubies.find { it.position.y < -0.5f && kotlin.math.abs(it.position.x - x) < 0.3f && kotlin.math.abs(it.position.z - z) < 0.3f }
                downList.add(c?.stickers?.find { it.currentNormal.y < -0.5f }?.color ?: RubikColor.WHITE)
            }
        }
        faces[CubeFace.DOWN] = downList

        return faces
    }

    /**
     * Generates a sample scan pattern preset for testing in emulator or instant demonstration.
     */
    fun getPresetFaces(presetName: String = "sune"): Map<CubeFace, List<RubikColor>> {
        val algo = when (presetName.lowercase()) {
            "sune", "oll_27" -> AlgorithmItem(
                id = "3x3_oll_27",
                methodId = "cfop",
                group = "OLL",
                name = "OLL 27 - Sune",
                notation = "R U R' U R U2 R'",
                setupMoves = "R U2 R' U' R U' R'",
                description = "Bentuk ikan.",
                fingerTricks = ""
            )
            "t_shape", "oll_33" -> AlgorithmItem(
                id = "3x3_oll_33",
                methodId = "cfop",
                group = "OLL",
                name = "OLL 33 - T-Shape",
                notation = "R U R' U' R' F R F'",
                setupMoves = "F R' F' R U R U' R'",
                description = "Bentuk T.",
                fingerTricks = ""
            )
            "antisune", "oll_26" -> AlgorithmItem(
                id = "3x3_oll_26",
                methodId = "cfop",
                group = "OLL",
                name = "OLL 26 - Anti-Sune",
                notation = "R' U' R U' R' U2 R",
                setupMoves = "R' U2 R U R' U R",
                description = "Anti-Sune.",
                fingerTricks = ""
            )
            "h_shape", "oll_21" -> AlgorithmItem(
                id = "3x3_oll_21",
                methodId = "cfop",
                group = "OLL",
                name = "OLL 21 - Cross H",
                notation = "F (R U R' U')3 F'",
                setupMoves = "F (R U R' U')3 F'",
                description = "H-Shape.",
                fingerTricks = ""
            )
            else -> AlgorithmItem(
                id = "3x3_oll_27",
                methodId = "cfop",
                group = "OLL",
                name = "OLL 27 - Sune",
                notation = "R U R' U R U2 R'",
                setupMoves = "R U2 R' U' R U' R'",
                description = "Bentuk ikan.",
                fingerTricks = ""
            )
        }

        return generateFacesFromAlgorithm(algo)
    }
}
