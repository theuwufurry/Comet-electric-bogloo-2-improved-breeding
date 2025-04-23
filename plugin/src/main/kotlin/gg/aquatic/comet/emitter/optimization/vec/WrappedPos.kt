package gg.aquatic.comet.emitter.optimization.vec

import org.joml.Vector3d

class WrappedPos(
    val vec: Vector3d,
    var time: Double,
    private val timeCoefficient: Double,
) : Vec {
    var internalTime = time * timeCoefficient
        set(value) {
            field = value * timeCoefficient
            time = value
        }

    override fun distanceSquared(other: Vec): Double {
        other as WrappedPos
        return other.vec.distanceSquared(vec) + internalTime * internalTime
    }

    override fun clone(): Vec {
        return WrappedPos(Vector3d(vec), time, timeCoefficient)
    }

    override fun sub(other: Vec): Vec {
        other as WrappedPos
        vec.sub(other.vec)
        internalTime = time - other.time
        return this
    }

    override fun lengthSquared(): Double {
        return vec.lengthSquared() + internalTime * internalTime
    }

    override fun dot(other: Vec): Double {
        other as WrappedPos
        return vec.dot(other.vec) + internalTime * other.internalTime
    }
}