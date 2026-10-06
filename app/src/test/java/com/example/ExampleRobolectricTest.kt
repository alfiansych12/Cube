package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CubeState2x2
import com.example.engine.CubeState3x3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RubikMaster", appName)
  }

  @Test
  fun `cube 2x2 starts solved`() {
    val cube = CubeState2x2.createSolved()
    assertTrue(cube.isSolved())
  }

  @Test
  fun `four R moves on 2x2 returns to solved`() {
    val cube = CubeState2x2.createSolved()
    cube.applyAlgorithm("R R R R")
    assertTrue(cube.isSolved())
  }

  @Test
  fun `sexy move six times on 2x2 returns to solved`() {
    val cube = CubeState2x2.createSolved()
    cube.applyAlgorithm("R U R' U' R U R' U' R U R' U' R U R' U' R U R' U' R U R' U'")
    assertTrue(cube.isSolved())
  }

  @Test
  fun `cube 3x3 starts solved`() {
    val cube = CubeState3x3.createSolved()
    assertTrue(cube.isSolved())
  }

  @Test
  fun `four R moves on 3x3 returns to solved`() {
    val cube = CubeState3x3.createSolved()
    cube.applyAlgorithm("R R R R")
    assertTrue(cube.isSolved())
  }

  @Test
  fun `six sexy moves on 3x3 returns to solved`() {
    val cube = CubeState3x3.createSolved()
    cube.applyAlgorithm("R U R' U' R U R' U' R U R' U' R U R' U' R U R' U' R U R' U'")
    assertTrue(cube.isSolved())
  }

  @Test
  fun `H-perm twice on 3x3 returns to solved`() {
    val cube = CubeState3x3.createSolved()
    val hPerm = "M2 U M2 U2 M2 U M2"
    cube.applyAlgorithm("$hPerm $hPerm")
    assertTrue(cube.isSolved())
  }

  @Test
  fun `T-perm twice on 3x3 returns to solved`() {
    val cube = CubeState3x3.createSolved()
    val tPerm = "R U R' U' R' F R2 U' R' U' R U R' F'"
    cube.applyAlgorithm("$tPerm $tPerm")
    assertTrue(cube.isSolved())
  }

  @Test
  fun `verify full CFOP algorithm counts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val jsonString = context.assets.open("algorithms_3x3.json").bufferedReader().use { it.readText() }
    val root = org.json.JSONObject(jsonString)
    val algosArr = root.getJSONArray("algorithms")

    var f2lCount = 0
    var ollCount = 0
    var pllCount = 0
    val ids = mutableSetOf<String>()

    for (i in 0 until algosArr.length()) {
      val obj = algosArr.getJSONObject(i)
      val id = obj.getString("id")
      val methodId = obj.getString("methodId")
      val group = obj.getString("group")

      assertTrue("ID $id must be unique", ids.add(id))

      if (methodId == "cfop") {
        when (group) {
          "F2L" -> f2lCount++
          "OLL" -> ollCount++
          "PLL" -> pllCount++
        }
      }
    }

    assertEquals("F2L must have exactly 41 algorithms", 41, f2lCount)
    assertEquals("OLL must have exactly 57 algorithms", 57, ollCount)
    assertEquals("PLL must have exactly 21 algorithms", 21, pllCount)
    assertEquals("CFOP total must be 119", 119, f2lCount + ollCount + pllCount)
  }
}
