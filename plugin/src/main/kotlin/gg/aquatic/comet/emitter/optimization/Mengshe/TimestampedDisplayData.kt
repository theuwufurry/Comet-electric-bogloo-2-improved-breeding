package gg.aquatic.comet.emitter.optimization.Mengshe

import gg.aquatic.comet.emitter.optimization.Mengshe.math.Quaternion
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

data class TimestampedDisplayData(
    val t: Int,

    val scaleX: Double,
    val scaleY: Double,
    val scaleZ: Double,

    val rot: Quaternion,

    val opacity: Int,
) {
    constructor(t: Int, s: Double, rx: Double, ry: Double, rz: Double, rw: Double, opacity: Int) : this(
        t,
        s,
        s,
        s,
        Quaternion(rx, ry, rz, rw),
        255
    )

    fun scaleDistance(scaleX: Double, scaleY: Double, scaleZ: Double): Double {
        return sqrt(
            Math.fma(
                this.scaleX - scaleX,
                this.scaleX - scaleX,
                Math.fma(
                    this.scaleY - scaleY,
                    this.scaleY - scaleY,
                    (this.scaleZ - scaleZ) * (this.scaleZ - scaleZ)
                )
            )
        )
    }

    private val _q = Quaternion(0.0, 0.0, 0.0, 0.0)
    private val _q2 = Quaternion(0.0, 0.0, 0.0, 0.0)

    fun rotDistance(other: Quaternion): Double {
        val dQ = _q.set(rot).mul(_q2.set(other).conjugate())
        if (dQ.w < 0.0) dQ *= -1.0
        dQ.normalize()
        val angularDelta = 2.0 * acos(dQ.w.coerceIn(-1.0, 1.0))
       
        return angularDelta
    }

    fun opacityDistance(other: Int): Int {
        return abs(other - opacity)
    }
}

fun interpolateOpacity(s: Int, e: Int, a: Double): Int {
    check(a >= 0.0 && a < 1.0)
    /*
    internally, the opacity is represented as an i8, just interpreted as unsigned
    this means that interpolating between 128ub -> 127ub actually means -128b -> 127b, which passes through -1b == 255ub, 0b == 0ub
    
    however this is the only discontinuity
     */
    val start = s.toByte().toInt()
    val end = e.toByte().toInt()

    val interpolated = ((end - start) * a).toInt() + start

    return interpolated and 0xFF
}