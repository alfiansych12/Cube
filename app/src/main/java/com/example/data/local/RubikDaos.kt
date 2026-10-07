package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlgorithmMastery
import com.example.data.model.SolveRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AlgorithmMasteryDao {
    @Query("SELECT * FROM algorithm_mastery")
    fun getAllMastery(): Flow<List<AlgorithmMastery>>

    @Query("SELECT * FROM algorithm_mastery WHERE algorithmId = :id LIMIT 1")
    suspend fun getMasteryById(id: String): AlgorithmMastery?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mastery: AlgorithmMastery)

    @Query("UPDATE algorithm_mastery SET isMastered = :isMastered WHERE algorithmId = :id")
    suspend fun updateMastered(id: String, isMastered: Boolean)

    @Query("UPDATE algorithm_mastery SET isBookmarked = :isBookmarked WHERE algorithmId = :id")
    suspend fun updateBookmarked(id: String, isBookmarked: Boolean)

    @Query("UPDATE algorithm_mastery SET practiceCount = practiceCount + 1, lastPracticedAt = :timestamp WHERE algorithmId = :id")
    suspend fun incrementPractice(id: String, timestamp: Long)
}

@Dao
interface SolveRecordDao {
    @Query("SELECT * FROM solve_records WHERE cubeType = :cubeType ORDER BY timestamp DESC")
    fun getSolvesByCube(cubeType: String): Flow<List<SolveRecord>>

    @Query("SELECT * FROM solve_records ORDER BY timestamp DESC")
    fun getAllSolves(): Flow<List<SolveRecord>>

    @Query("SELECT MIN(timeMillis) FROM solve_records WHERE cubeType = :cubeType AND penalty != 'DNF'")
    fun getBestTime(cubeType: String): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolve(record: SolveRecord): Long

    @Query("DELETE FROM solve_records WHERE id = :id")
    suspend fun deleteSolve(id: Long)

    @Query("DELETE FROM solve_records WHERE cubeType = :cubeType")
    suspend fun clearSolvesForCube(cubeType: String)
}
