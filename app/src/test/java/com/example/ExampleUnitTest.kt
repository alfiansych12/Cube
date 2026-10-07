package com.example

import com.example.data.model.AlgorithmItem
import com.example.engine.CubePlaybackEngine
import com.example.engine.CubeState2x2
import com.example.engine.CubeState3x3
import com.example.engine.CubeState4x4
import com.example.engine.CubeState5x5
import com.example.engine.RubikColor
import com.example.engine.Vector3D
import com.example.scanner.CubeFace
import com.example.scanner.RubikColorDetector
import com.example.scanner.RubikOLLMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAllCubesYellowOnTopWhiteOnBottom() {
        val cubes = listOf(
            2 to CubeState2x2.createSolved().cubies,
            3 to CubeState3x3.createSolved().cubies,
            4 to CubeState4x4.createSolved().cubies,
            5 to CubeState5x5.createSolved().cubies
        )

        for ((size, cubies) in cubes) {
            val topStickers = cubies.flatMap { it.stickers }.filter { it.currentNormal.y > 0.7f }
            val bottomStickers = cubies.flatMap { it.stickers }.filter { it.currentNormal.y < -0.7f }
            val frontStickers = cubies.flatMap { it.stickers }.filter { it.currentNormal.z > 0.7f }
            val backStickers = cubies.flatMap { it.stickers }.filter { it.currentNormal.z < -0.7f }
            val rightStickers = cubies.flatMap { it.stickers }.filter { it.currentNormal.x > 0.7f }
            val leftStickers = cubies.flatMap { it.stickers }.filter { it.currentNormal.x < -0.7f }

            val expectedCount = size * size

            assertEquals("Top count for size $size", expectedCount, topStickers.size)
            assertEquals("Bottom count for size $size", expectedCount, bottomStickers.size)

            assertTrue("All top stickers must be YELLOW for size $size", topStickers.all { it.color == RubikColor.YELLOW })
            assertTrue("All bottom stickers must be WHITE for size $size", bottomStickers.all { it.color == RubikColor.WHITE })
            assertTrue("All front stickers must be GREEN for size $size", frontStickers.all { it.color == RubikColor.GREEN })
            assertTrue("All back stickers must be BLUE for size $size", backStickers.all { it.color == RubikColor.BLUE })
            assertTrue("All right stickers must be ORANGE for size $size", rightStickers.all { it.color == RubikColor.ORANGE })
            assertTrue("All left stickers must be RED for size $size", leftStickers.all { it.color == RubikColor.RED })
        }
    }

    @Test
    fun testPlaybackEngineSolvedStates() {
        for (size in 2..5) {
            val cubies = CubePlaybackEngine.createSolved(size)
            assertTrue("Cube $size must be solved initially", CubePlaybackEngine.isSolvedState(cubies, size))
        }
    }

    private fun rgbInt(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

    @Test
    fun testColorClassification() {
        // Yellow RGB
        assertEquals(RubikColor.YELLOW, RubikColorDetector.classifyColor(rgbInt(250, 204, 21)))
        // White RGB
        assertEquals(RubikColor.WHITE, RubikColorDetector.classifyColor(rgbInt(248, 250, 252)))
        // Green RGB
        assertEquals(RubikColor.GREEN, RubikColorDetector.classifyColor(rgbInt(34, 197, 94)))
        // Blue RGB
        assertEquals(RubikColor.BLUE, RubikColorDetector.classifyColor(rgbInt(37, 99, 235)))
        // Red RGB
        assertEquals(RubikColor.RED, RubikColorDetector.classifyColor(rgbInt(239, 68, 68)))
        // Orange RGB
        assertEquals(RubikColor.ORANGE, RubikColorDetector.classifyColor(rgbInt(249, 115, 22)))
    }

    @Test
    fun testOLLMatcherWithSune() {
        val suneAlgo = AlgorithmItem(
            id = "3x3_oll_27",
            methodId = "cfop",
            group = "OLL",
            name = "OLL 27 - Sune",
            notation = "R U R' U R U2 R'",
            setupMoves = "R U2 R' U' R U' R'",
            description = "Bentuk ikan.",
            fingerTricks = "Standard Sune."
        )

        val tShapeAlgo = AlgorithmItem(
            id = "3x3_oll_33",
            methodId = "cfop",
            group = "OLL",
            name = "OLL 33 - T-Shape",
            notation = "R U R' U' R' F R F'",
            setupMoves = "F R' F' R U R U' R'",
            description = "Bentuk T.",
            fingerTricks = "Standard T-shape."
        )

        val sunePreset = RubikOLLMatcher.getPresetFaces("sune")
        val results = RubikOLLMatcher.matchOLL(sunePreset, listOf(suneAlgo, tShapeAlgo))

        assertTrue("Results must not be empty", results.isNotEmpty())
        assertEquals("Top match must be Sune OLL 27", "3x3_oll_27", results.first().algorithm.id)
        assertEquals("Sune match confidence should be 100%", 100, results.first().confidencePercent)
    }

    @Test
    fun testOLLMatcherWithTShape() {
        val suneAlgo = AlgorithmItem(
            id = "3x3_oll_27",
            methodId = "cfop",
            group = "OLL",
            name = "OLL 27 - Sune",
            notation = "R U R' U R U2 R'",
            setupMoves = "R U2 R' U' R U' R'",
            description = "Bentuk ikan.",
            fingerTricks = "Standard Sune."
        )

        val tShapeAlgo = AlgorithmItem(
            id = "3x3_oll_33",
            methodId = "cfop",
            group = "OLL",
            name = "OLL 33 - T-Shape",
            notation = "R U R' U' R' F R F'",
            setupMoves = "F R' F' R U R U' R'",
            description = "Bentuk T.",
            fingerTricks = "Standard T-shape."
        )

        val tShapePreset = RubikOLLMatcher.getPresetFaces("t_shape")
        val results = RubikOLLMatcher.matchOLL(tShapePreset, listOf(suneAlgo, tShapeAlgo))

        assertTrue("Results must not be empty", results.isNotEmpty())
        assertEquals("Top match must be T-Shape OLL 33", "3x3_oll_33", results.first().algorithm.id)
        assertEquals("T-shape match confidence should be 100%", 100, results.first().confidencePercent)
    }
}
