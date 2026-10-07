package com.example.engine

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)

    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0.0001f) Vector3D(x / len, y / len, z / len) else this
    }

    fun rotateX(radians: Float): Vector3D {
        val cosA = cos(radians)
        val sinA = sin(radians)
        return Vector3D(
            x,
            y * cosA - z * sinA,
            y * sinA + z * cosA
        )
    }

    fun rotateY(radians: Float): Vector3D {
        val cosA = cos(radians)
        val sinA = sin(radians)
        return Vector3D(
            x * cosA + z * sinA,
            y,
            -x * sinA + z * cosA
        )
    }

    fun rotateZ(radians: Float): Vector3D {
        val cosA = cos(radians)
        val sinA = sin(radians)
        return Vector3D(
            x * cosA - y * sinA,
            x * sinA + y * cosA,
            z
        )
    }

    fun snapToGrid(): Vector3D {
        fun snap(v: Float): Float {
            val rounded2 = round(v * 2f) / 2f
            return if (kotlin.math.abs(rounded2) < 0.001f) 0f else rounded2
        }
        return Vector3D(snap(x), snap(y), snap(z))
    }

    fun snapNormal(): Vector3D {
        fun snap(v: Float): Float {
            val r = round(v)
            return if (kotlin.math.abs(r) < 0.001f) 0f else r
        }
        return Vector3D(snap(x), snap(y), snap(z)).normalized()
    }
}

enum class RubikColor(val displayName: String, val color: Color, val hex: String) {
    YELLOW("Kuning (U)", Color(0xFFFACC15), "#FACC15"),
    WHITE("Putih (D)", Color(0xFFF8FAFC), "#F8FAFC"),
    GREEN("Hijau (F)", Color(0xFF22C55E), "#22C55E"),
    BLUE("Biru (B)", Color(0xFF2563EB), "#2563EB"),
    ORANGE("Oranye (R)", Color(0xFFF97316), "#F97316"),
    RED("Merah (L)", Color(0xFFEF4444), "#EF4444"),
    CORE("Core", Color(0xFF1E293B), "#1E293B");

    companion object {
        fun fromNormal(normal: Vector3D): RubikColor {
            val nx = normal.x
            val ny = normal.y
            val nz = normal.z
            return when {
                ny > 0.6f -> YELLOW
                ny < -0.6f -> WHITE
                nz > 0.6f -> GREEN
                nz < -0.6f -> BLUE
                nx > 0.6f -> ORANGE
                nx < -0.6f -> RED
                else -> CORE
            }
        }
    }
}
