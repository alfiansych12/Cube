package com.example.scanner

import com.example.engine.RubikColor

/**
 * Represents each of the 6 faces of a 3x3 Rubik's cube during guided scanning.
 */
enum class CubeFace(
    val code: String,
    val title: String,
    val subtitle: String,
    val centerColor: RubikColor,
    val instruction: String,
    val guideHint: String
) {
    UP(
        code = "U",
        title = "Sisi Atas (Up)",
        subtitle = "Pusat Kuning",
        centerColor = RubikColor.YELLOW,
        instruction = "Posisikan sisi KUNING menghadap kamera.",
        guideHint = "Pastikan sisi Hijau berada di arah bawah pandangan Anda."
    ),
    FRONT(
        code = "F",
        title = "Sisi Depan (Front)",
        subtitle = "Pusat Hijau",
        centerColor = RubikColor.GREEN,
        instruction = "Posisikan sisi HIJAU menghadap kamera.",
        guideHint = "Sisi Kuning tetap berada di bagian atas."
    ),
    RIGHT(
        code = "R",
        title = "Sisi Kanan (Right)",
        subtitle = "Pusat Oranye",
        centerColor = RubikColor.ORANGE,
        instruction = "Putar kubus 90° ke kiri, arahkan sisi ORANYE ke kamera.",
        guideHint = "Sisi Kuning tetap berada di bagian atas."
    ),
    BACK(
        code = "B",
        title = "Sisi Belakang (Back)",
        subtitle = "Pusat Biru",
        centerColor = RubikColor.BLUE,
        instruction = "Putar kubus 90° ke kiri, arahkan sisi BIRU ke kamera.",
        guideHint = "Sisi Kuning tetap berada di bagian atas."
    ),
    LEFT(
        code = "L",
        title = "Sisi Kiri (Left)",
        subtitle = "Pusat Merah",
        centerColor = RubikColor.RED,
        instruction = "Putar kubus 90° ke kanan, arahkan sisi MERAH ke kamera.",
        guideHint = "Sisi Kuning tetap berada di bagian atas."
    ),
    DOWN(
        code = "D",
        title = "Sisi Bawah (Down)",
        subtitle = "Pusat Putih",
        centerColor = RubikColor.WHITE,
        instruction = "Posisikan sisi PUTIH di bawah menghadap kamera.",
        guideHint = "Pastikan sisi Hijau berada di arah atas pandangan Anda."
    );

    companion object {
        fun defaultFaceColors(face: CubeFace): List<RubikColor> {
            return List(9) { face.centerColor }
        }
    }
}
