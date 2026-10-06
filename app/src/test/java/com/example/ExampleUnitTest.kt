package com.example

import com.example.engine.CubePlaybackEngine
import com.example.engine.CubeState2x2
import com.example.engine.CubeState3x3
import com.example.engine.CubeState4x4
import com.example.engine.CubeState5x5
import com.example.engine.RubikColor
import com.example.engine.Vector3D
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
            assertTrue("All right stickers must be RED for size $size", rightStickers.all { it.color == RubikColor.RED })
            assertTrue("All left stickers must be ORANGE for size $size", leftStickers.all { it.color == RubikColor.ORANGE })
        }
    }

    @Test
    fun testPlaybackEngineSolvedStates() {
        for (size in 2..5) {
            val cubies = CubePlaybackEngine.createSolved(size)
            assertTrue("Cube $size must be solved initially", CubePlaybackEngine.isSolvedState(cubies, size))
        }
    }
}
