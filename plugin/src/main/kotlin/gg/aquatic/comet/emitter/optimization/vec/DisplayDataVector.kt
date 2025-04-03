package gg.aquatic.comet.emitter.optimization.vec

import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.emitter.optimization.alpha
import gg.aquatic.comet.particle.Particle
import org.joml.Quaternionf
import org.joml.Vector3f

/*
pitch, roll, scale are not simple
    0 -> 2pi, get closest distance
 */
class DisplayDataVector private constructor(
    private var alpha: Double,
    var rot: Quaternionf,
    var scale: Vector3f,
    var time: Double,
    private val coefficients: DisplayDataVectorCoefficients,
    val displayData: DisplayData,
) : Vec {
    /**
     * INTERNAL variables must be modified with EXTERNAL variables to prevent imprecision errors
     */
    var internalAlpha = alpha * coefficients.alpha
        set(value) {
            field = value * coefficients.alpha
            alpha = value
        }

    var internalRot: Quaternionf = Quaternionf(rot).mul(coefficients.rot)
        set(value) {
            field = Quaternionf(value).mul(coefficients.rot)
            rot = value
        }

    var internalScale: Vector3f = Vector3f(scale).mul(coefficients.scale)
        set(value) {
            field = Vector3f(value).mul(coefficients.scale)
            scale = value
        }

    var internalTime = time * coefficients.time
        set(value) {
            field = value * coefficients.time
            time = value
        }

    override fun clone(): Vec {
        return DisplayDataVector(alpha, Quaternionf(rot), Vector3f(scale), time, coefficients, displayData)
    }

    override fun lengthSquared(): Double {
        return Math.fma(
            internalAlpha,
            internalAlpha,
            internalTime * internalTime
        ) + internalRot.lengthSquared() + internalScale.lengthSquared()
    }

    override fun distanceSquared(other: Vec): Double {
        other as DisplayDataVector
        val dAlpha = other.internalAlpha - internalAlpha
        val dt = other.internalTime - time
        return Math.fma(
            dAlpha,
            dAlpha,
            Math.fma(
                other.internalRot.x - internalRot.x.toDouble(),
                other.internalRot.x - internalRot.x.toDouble(),
                Math.fma(
                    other.internalRot.y - internalRot.y.toDouble(),
                    other.internalRot.y - internalRot.y.toDouble(),
                    Math.fma(
                        other.internalRot.z - internalRot.z.toDouble(),
                        other.internalRot.z - internalRot.z.toDouble(),
                        Math.fma(
                            other.internalRot.w - internalRot.w.toDouble(),
                            other.internalRot.w - internalRot.w.toDouble(),
                            Math.fma(
                                dt,
                                dt,
                                other.internalScale.distanceSquared(internalScale).toDouble()
                            )
                        )
                    )
                )
            )
        )
    }

    override fun sub(other: Vec): Vec {
        other as DisplayDataVector

        internalAlpha = alpha - other.alpha
        internalRot.x = rot.x - other.rot.x
        internalRot.y = rot.y - other.rot.y
        internalRot.z = rot.z - other.rot.z
        internalRot.w = rot.w - other.rot.w
        internalScale = Vector3f(scale).sub(other.scale)
        internalTime = time - other.time

        return this
    }

    override fun dot(other: Vec): Double {
        other as DisplayDataVector
        return Math.fma(
            internalTime,
            other.internalTime,
            internalAlpha * other.internalAlpha + other.internalRot.dot(internalRot) + other.internalScale.dot(internalScale)
        )
    }

    companion object {
        fun create(
            alpha: Double,
            rot: Quaternionf,
            scale: Vector3f,
            time: Double,
            coefficients: DisplayDataVectorCoefficients,
            displayData: DisplayData
        ): DisplayDataVector {
            return DisplayDataVector(
                alpha,
                rot,
                scale,
                time,
                coefficients,
                displayData
            )
        }

        fun create(
            particle: Particle,
            coefficients: DisplayDataVectorCoefficients,
        ): DisplayDataVector {
            return create(
                particle.data.color.alpha(),
                particle.data.rotation,
                particle.data.scale,
                particle.data.age,
                coefficients,
                particle.data.displayData
            )
        }
    }

    override fun toString(): String {
        return """
            | Rot: $rot
            | Scale: $scale
            | Time: $time
        """.trimIndent()
    }
}

class DisplayDataVectorCoefficients(
    val alpha: Double = 1.0,
    val rot: Float = 1f,
    val scale: Float = 1f,
    val time: Double = 1.0,
)