package gg.aquatic.comet.emitter.optimization.Mengshe.math

import kotlin.math.sqrt

class Quaternion(var x: Double, var y: Double, var z: Double, var w: Double) {
    fun conjugate(): Quaternion {
        x *= -1
        y *= -1
        z *= -1
        
        return this
    }

    fun mul(other: Quaternion): Quaternion {
        x = Math.fma(w, other.x, Math.fma(x, other.w, Math.fma(y, other.z, -z * other.y)))
        y = Math.fma(w, other.y, Math.fma(-x, other.z, Math.fma(y, other.w, z * other.x)))
        z = Math.fma(w, other.z, Math.fma(x, other.y, Math.fma(-y, other.x, z * other.w)))
        w = Math.fma(w, other.w, Math.fma(-x, other.x, Math.fma(-y, other.y, -z * other.z)))

        return this
    }
    
    fun mul(scalar: Double): Quaternion {
        x *= scalar
        y *= scalar
        z *= scalar
        w *= scalar
        
        return this
    }
    
    fun lengthSquared(): Double {
        return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)))

    }
    
    fun normalize(): Quaternion {
        val invNorm: Double = invsqrt(lengthSquared())
        x *= invNorm
        y *= invNorm
        z *= invNorm
        w *= invNorm

        return this
    }

    fun nlerp(other: Quaternion, factor: Double): Quaternion {
        val cosom = Math.fma(this.x, other.x, Math.fma(this.y, other.y, Math.fma(this.z, other.z, this.w * other.w)))
        val scale0 = 1.0 - factor
        val scale1 = if (cosom >= 0.0) factor else -factor
        x = Math.fma(scale0, this.x, scale1 * other.x)
        y = Math.fma(scale0, this.y, scale1 * other.y)
        z = Math.fma(scale0, this.z, scale1 * other.z)
        w = Math.fma(scale0, this.w, scale1 * other.w)
        normalize()
        return this
    }
    
    fun set(x: Double, y: Double, z: Double, w: Double): Quaternion {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        
        return this
    }
    
    fun set(other: Quaternion): Quaternion {
        return set(other.x, other.y, other.z, other.w)
    }
    
    operator fun times(other: Quaternion): Quaternion {
        return Quaternion(x, y, z, w).mul(other)
    }
    
    operator fun times(scalar: Double): Quaternion {
        return Quaternion(x, y, z, w).mul(scalar)
    }
    
    operator fun timesAssign(scalar: Double) {
        mul(scalar)
    }
    
    operator fun unaryMinus(): Quaternion {
        return Quaternion(x, y, z, w).conjugate()
    }
}

fun invsqrt(d: Double): Double {
    return 1.0 / sqrt(d)
}