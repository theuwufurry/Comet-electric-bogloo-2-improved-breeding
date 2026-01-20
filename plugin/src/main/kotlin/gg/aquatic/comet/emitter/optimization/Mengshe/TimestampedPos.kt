package gg.aquatic.comet.emitter.optimization.Mengshe

import kotlin.math.sqrt

data class TimestampedPos(
    val t: Int,
    val x: Double,
    val y: Double,
    val z: Double,
) {
    fun distance(x: Double, y: Double, z: Double): Double {
        return sqrt(Math.fma(this.x - x, this.x - x, Math.fma(this.y - y, this.y - y, (this.z - z) * (this.z - z))))
    }
}