package gg.aquatic.comet.emitter.optimization

import org.joml.Vector3d
import java.util.*
import kotlin.math.pow

class CachedPath {
    /**
     * particle id -> < loc hashes , display hashes >
     */
    val hashes: MutableMap<UUID, Pair<MutableSet<Int>, MutableSet<Int>>> = mutableMapOf()
    /**
     * particle id -> < timestamped vector3d >, world coords
     */
    var locations: MutableMap<UUID, MutableList<TimestampedVector3d>> = mutableMapOf()

    fun optimized(): CachedPath {
        locations.replaceAll { _, v -> simplify(v, TOLERANCE) }
        return this
    }

    companion object {
        const val TOLERANCE = 0.05
    }
}

/**
 * time: Ticks
 */
data class TimestampedVector3d(
    val time: Int,
    val vec: Vector3d
)

private fun simplify(toSimplify: MutableList<TimestampedVector3d>, tolerance: Double): MutableList<TimestampedVector3d> {
    if (toSimplify.size < 3) return toSimplify

    val sqTolerance = tolerance * tolerance
    val radialSimplified: MutableList<TimestampedVector3d> = mutableListOf(toSimplify.first())

    var lastPoint = toSimplify.first()

    for (i in 1 until toSimplify.size - 1) {
        val currNode = toSimplify[i]
        if (lastPoint.vec.distanceSquared(currNode.vec) > sqTolerance) {
            radialSimplified += currNode
            lastPoint = currNode
        }
    }

    radialSimplified += toSimplify.last()

    return (listOf(toSimplify.first()) + douglas(sqTolerance, radialSimplified) + listOf(toSimplify.last())).toMutableList()
}

private fun douglas(sqTolerance: Double, nodes: List<TimestampedVector3d>): List<TimestampedVector3d> {
    if (nodes.size < 3) return emptyList()

    val start = nodes.first().vec
    val end = nodes.last().vec

    val delta = Vector3d(end).sub(start)
    val sqDeltaLength = delta.lengthSquared()

    var max = 0.0
    var index = -1

    for (i in 1 until nodes.size - 1) {
        //guaranteed to be no division by 0 bcs of radial distance preprocessing
        val offset = Vector3d(start).sub(nodes[i].vec)
        val sqDistance = ((offset.lengthSquared() * sqDeltaLength) - offset.dot(delta).pow(2)) / sqDeltaLength
        if (sqDistance > sqTolerance && sqDistance > max) {
            max = sqDistance
            index = i
        }
    }

    if (index == -1) return emptyList()
    else {
        val pivot = nodes[index]
        return douglas(sqTolerance, nodes.subList(0, index + 1)) + pivot + douglas(
            sqTolerance,
            nodes.subList(index + 1, nodes.size)
        )
    }
}
