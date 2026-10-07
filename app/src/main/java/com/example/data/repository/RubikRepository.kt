package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.AlgorithmItem
import com.example.data.model.AlgorithmMastery
import com.example.data.model.MethodType
import com.example.data.model.SolveRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class MethodSummary(
    val id: String,
    val name: String,
    val difficulty: String,
    val description: String,
    val steps: List<String>,
    val cubeType: String = "2x2"
)

data class RoadmapStage(
    val cube: String,
    val name: String,
    val tagline: String,
    val difficulty: String,
    val methods: List<RoadmapMethod>
)

data class RoadmapMethod(
    val name: String,
    val stages: List<String>
)

data class MasteryStats(
    val totalAlgorithms: Int,
    val masteredAlgorithms: Int,
    val totalPracticed: Int,
    val methodBreakdown: Map<String, Pair<Int, Int>>
)

class RubikRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val masteryDao = database.algorithmMasteryDao()
    private val solveDao = database.solveRecordDao()

    private val cachedAlgorithms = mutableListOf<AlgorithmItem>()
    private val cachedMethods = mutableListOf<MethodSummary>()
    private val cachedRoadmaps = mutableListOf<RoadmapStage>()

    init {
        loadDataFromAssets()
    }

    private fun loadDataFromAssets() {
        loadJsonAsset("algorithms_2x2.json", "2x2")
        loadJsonAsset("algorithms_3x3.json", "3x3")
    }

    private fun loadJsonAsset(fileName: String, defaultCubeType: String) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val cube = root.optString("cube", defaultCubeType)

            // Methods
            val methodsArr = root.optJSONArray("methods")
            if (methodsArr != null) {
                for (i in 0 until methodsArr.length()) {
                    val obj = methodsArr.getJSONObject(i)
                    val stepsList = mutableListOf<String>()
                    val stepsArr = obj.optJSONArray("steps")
                    if (stepsArr != null) {
                        for (s in 0 until stepsArr.length()) {
                            stepsList.add(stepsArr.getString(s))
                        }
                    }
                    cachedMethods.add(
                        MethodSummary(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            difficulty = obj.getString("difficulty"),
                            description = obj.getString("description"),
                            steps = stepsList,
                            cubeType = cube
                        )
                    )
                }
            }

            // Algorithms
            val algosArr = root.optJSONArray("algorithms")
            if (algosArr != null) {
                for (i in 0 until algosArr.length()) {
                    val obj = algosArr.getJSONObject(i)
                    val notation = obj.getString("notation")
                    val moveCount = notation.split("\\s+".toRegex()).count { it.isNotBlank() }
                    cachedAlgorithms.add(
                        AlgorithmItem(
                            id = obj.getString("id"),
                            methodId = obj.getString("methodId"),
                            group = obj.getString("group"),
                            name = obj.getString("name"),
                            notation = notation,
                            setupMoves = obj.optString("setupMoves", ""),
                            description = obj.optString("description", ""),
                            fingerTricks = obj.optString("fingerTricks", ""),
                            moveCount = moveCount
                        )
                    )
                }
            }

            // Roadmaps
            val roadmapsArr = root.optJSONArray("roadmaps")
            if (roadmapsArr != null) {
                for (i in 0 until roadmapsArr.length()) {
                    val rObj = roadmapsArr.getJSONObject(i)
                    val methodsList = mutableListOf<RoadmapMethod>()
                    val rMethods = rObj.optJSONArray("methods")
                    if (rMethods != null) {
                        for (m in 0 until rMethods.length()) {
                            val mObj = rMethods.getJSONObject(m)
                            val stagesList = mutableListOf<String>()
                            val stagesArr = mObj.optJSONArray("stages")
                            if (stagesArr != null) {
                                for (s in 0 until stagesArr.length()) {
                                    stagesList.add(stagesArr.getString(s))
                                }
                            }
                            methodsList.add(RoadmapMethod(mObj.getString("name"), stagesList))
                        }
                    }
                    cachedRoadmaps.add(
                        RoadmapStage(
                            cube = rObj.getString("cube"),
                            name = rObj.getString("name"),
                            tagline = rObj.getString("tagline"),
                            difficulty = rObj.getString("difficulty"),
                            methods = methodsList
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllMethods(): List<MethodSummary> = cachedMethods

    fun getMethodsForCube(cube: String): List<MethodSummary> {
        return cachedMethods.filter { it.cubeType.equals(cube, ignoreCase = true) }
    }

    fun getRoadmaps(): List<RoadmapStage> = cachedRoadmaps

    fun getAlgorithmById(id: String): AlgorithmItem? {
        return cachedAlgorithms.find { it.id == id }
    }

    fun getAlgorithms(methodId: String? = null, cubeType: String? = null): List<AlgorithmItem> {
        var filtered = cachedAlgorithms.toList()
        if (!cubeType.isNullOrBlank()) {
            filtered = if (cubeType == "3x3") {
                filtered.filter { it.id.startsWith("3x3") }
            } else {
                filtered.filter { !it.id.startsWith("3x3") }
            }
        }
        if (!methodId.isNullOrBlank()) {
            filtered = filtered.filter { it.methodId.equals(methodId, ignoreCase = true) }
        }
        return filtered
    }

    fun getAlgorithmsFlow(methodId: String? = null, cubeType: String? = null): Flow<List<AlgorithmItem>> {
        return masteryDao.getAllMastery().map { masteryList ->
            val masteryMap = masteryList.associateBy { it.algorithmId }
            var filtered = cachedAlgorithms.toList()

            if (!cubeType.isNullOrBlank()) {
                filtered = if (cubeType == "3x3") {
                    filtered.filter { it.id.startsWith("3x3") }
                } else {
                    filtered.filter { !it.id.startsWith("3x3") }
                }
            }

            if (!methodId.isNullOrBlank()) {
                filtered = filtered.filter { it.methodId.equals(methodId, ignoreCase = true) }
            }

            filtered.map { algo ->
                val mastery = masteryMap[algo.id]
                algo.copy(
                    isMastered = mastery?.isMastered ?: false,
                    practiceCount = mastery?.practiceCount ?: 0
                )
            }
        }
    }

    suspend fun toggleMastery(algorithmId: String, methodId: String) = withContext(Dispatchers.IO) {
        val existing = masteryDao.getMasteryById(algorithmId)
        if (existing == null) {
            masteryDao.insertOrUpdate(
                AlgorithmMastery(
                    algorithmId = algorithmId,
                    methodId = methodId,
                    isMastered = true,
                    practiceCount = 1,
                    lastPracticedAt = System.currentTimeMillis()
                )
            )
        } else {
            masteryDao.updateMastered(algorithmId, !existing.isMastered)
        }
    }

    suspend fun recordPractice(algorithmId: String, methodId: String) = withContext(Dispatchers.IO) {
        val existing = masteryDao.getMasteryById(algorithmId)
        if (existing == null) {
            masteryDao.insertOrUpdate(
                AlgorithmMastery(
                    algorithmId = algorithmId,
                    methodId = methodId,
                    isMastered = false,
                    practiceCount = 1,
                    lastPracticedAt = System.currentTimeMillis()
                )
            )
        } else {
            masteryDao.incrementPractice(algorithmId, System.currentTimeMillis())
        }
    }

    fun getMasteryStatsFlow(): Flow<MasteryStats> {
        return masteryDao.getAllMastery().map { masteryList ->
            val masteryMap = masteryList.associateBy { it.algorithmId }
            var masteredCount = 0
            var totalPracticedCount = 0

            val breakdown = mutableMapOf<String, Pair<Int, Int>>()
            val methods = listOf("ortega", "cll", "eg1", "lbl", "cfop", "lbl_3x3", "roux")

            for (m in methods) {
                val inMethod = cachedAlgorithms.filter { it.methodId == m }
                val masteredInMethod = inMethod.count { masteryMap[it.id]?.isMastered == true }
                breakdown[m] = Pair(masteredInMethod, inMethod.size)
            }

            for (algo in cachedAlgorithms) {
                val m = masteryMap[algo.id]
                if (m?.isMastered == true) masteredCount++
                if ((m?.practiceCount ?: 0) > 0) totalPracticedCount++
            }

            MasteryStats(
                totalAlgorithms = cachedAlgorithms.size,
                masteredAlgorithms = masteredCount,
                totalPracticed = totalPracticedCount,
                methodBreakdown = breakdown
            )
        }
    }

    fun getSolvesFlow(cubeType: String = "2x2"): Flow<List<SolveRecord>> = solveDao.getSolvesByCube(cubeType)

    fun getBestTimeFlow(cubeType: String = "2x2"): Flow<Long?> = solveDao.getBestTime(cubeType)

    suspend fun saveSolve(timeMillis: Long, scramble: String, cubeType: String = "2x2", penalty: String = "OK") =
        withContext(Dispatchers.IO) {
            solveDao.insertSolve(
                SolveRecord(
                    cubeType = cubeType,
                    timeMillis = timeMillis,
                    scramble = scramble,
                    penalty = penalty
                )
            )
        }

    suspend fun deleteSolve(id: Long) = withContext(Dispatchers.IO) {
        solveDao.deleteSolve(id)
    }
}
