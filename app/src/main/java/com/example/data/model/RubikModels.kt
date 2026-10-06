package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CubeType(val displayName: String, val size: Int) {
    TWO_BY_TWO("2x2 Pocket Cube", 2),
    THREE_BY_THREE("3x3 Rubik's Cube", 3),
    FOUR_BY_FOUR("4x4 Rubik's Revenge", 4),
    FIVE_BY_FIVE("5x5 Professor's Cube", 5)
}

enum class MethodType(val id: String, val displayName: String, val difficulty: String, val description: String) {
    ORTEGA(
        "ortega",
        "Metode Ortega",
        "Menengah",
        "Metode cepat 3 langkah untuk 2x2: Selesaikan satu sisi (tanpa peduli layer), orientasi sisi atas (OLL), lalu permutasi kedua layer sekaligus (PBL)."
    ),
    CLL(
        "cll",
        "Metode CLL",
        "Lanjutan",
        "Corners of the Last Layer: Digunakan saat layer bawah sudah selesai sempurna. Menyelesaikan orientasi dan permutasi layer atas sekaligus dalam 1 algoritma (42 rumus)."
    ),
    EG1(
        "eg1",
        "Metode EG-1",
        "Profesional",
        "Erik-Gunnar 1: Digunakan saat layer bawah memiliki satu pasang balok sejajar (adjacent swap). Menyelesaikan seluruh kubus secara langsung dalam 1 algoritma."
    ),
    LBL(
        "lbl",
        "Metode LBL (Pemula)",
        "Pemula",
        "Layer By Layer: Dasar paling intuitif. Langkah 1: selesaikan layer pertama, Langkah 2: OLL sudut kuning, Langkah 3: PLL sudut atas."
    )
}

data class AlgorithmItem(
    val id: String,
    val methodId: String,
    val group: String, // e.g. "OLL", "PBL", "Sune", "Anti-Sune", "H", "Pi", "T", "U", "L"
    val name: String,
    val notation: String,
    val setupMoves: String = "", // Moves to produce this case from solved state
    val description: String,
    val fingerTricks: String = "",
    val moveCount: Int = 0,
    val isMastered: Boolean = false,
    val practiceCount: Int = 0
)

@Entity(tableName = "algorithm_mastery")
data class AlgorithmMastery(
    @PrimaryKey val algorithmId: String,
    val methodId: String,
    val isMastered: Boolean = false,
    val practiceCount: Int = 0,
    val lastPracticedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "solve_records")
data class SolveRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cubeType: String = "2x2",
    val timeMillis: Long,
    val scramble: String,
    val timestamp: Long = System.currentTimeMillis(),
    val penalty: String = "OK" // "OK", "+2", "DNF"
)
