package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.emitter.optimization.vec.DisplayDataVector
import gg.aquatic.comet.emitter.optimization.vec.Vec
import gg.aquatic.comet.emitter.optimization.vec.WrappedPos
import java.util.*
import kotlin.math.pow

/*
colors ARE important, use radial check

optimize in between w/ douglas for everything else
 */

class CachedPath(
    val locTol: Double = 0.05,
    val dispTol: Double = 0.05,
    val colTol: Double = 32.0,
) {
    /**
     * particle id -> < loc hashes , display hashes >
     */
    val hashes: MutableMap<UUID, Pair<MutableSet<Int>, MutableSet<Int>>> = mutableMapOf()

    /**
     * particle id -> < timestamped vector3d >, world coords
     */
    var locations: MutableMap<UUID, MutableList<TimestampedPos>> = mutableMapOf()
    var colors: MutableMap<UUID, MutableList<TimestampedColor>> = mutableMapOf()
    var displayData: MutableMap<UUID, MutableList<TimestampedDisplayData>> = mutableMapOf()

    fun optimized(): CachedPath {
        locations.replaceAll { _, v ->
            val b = v.size
            val r = simplfiyLocs(v, locTol)
            if (DEBUG_LOCS >= 1) {
                println(
                    "--L--\n" + "$b -> ${r.size}"
                )
            }

            if (DEBUG_LOCS >= 2) {
                for (a in r) {
                    println(a)
                }
            }

            r
        }
        displayData.replaceAll { _, v ->
            val b = v.size
            val r = simplifyDisplayData(v, dispTol)

            if (DEBUG_DISPLAY_DATA >= 1) {
                print(
                    "--DD--\n" + "$b -> ${r.size}"
                )
            }

            if (DEBUG_DISPLAY_DATA >= 2) {
                for (a in r) {
                    println(a)
                }
            }

            r
        }
        colors.replaceAll { _, v ->
            val b = v.size
            val r = simplifyColors(v, colTol)
            if (DEBUG_COLORS >= 1) {
                println("--CD--\n" + "$b -> ${r.size}")
            }

            if (DEBUG_COLORS >= 2) {
                for (a in r) {
                    println(a)
                }
            }

            r

        }
        return this
    }

    companion object {
        // 0 - off, 1 - size, 2 - list
        val DEBUG_LOCS = 1
        val DEBUG_DISPLAY_DATA = 1
        val DEBUG_COLORS = 1
    }
}

/**
 * time: Ticks
 * vec: location
 */

abstract class TimestampedData(
    val time: Int,
) {
    abstract val vec: Vec
}

class TimestampedPos(
    time: Int,
    override val vec: WrappedPos
) : TimestampedData(time) {
    override fun toString(): String {
        return """
            | Time: $time
            | Pos: ${vec.vec.x} ${vec.vec.y} ${vec.vec.z}
        """.trimIndent()
    }
}

class TimestampedColor(
    val time: Int,
    private val r: Int,
    private val g: Int,
    private val b: Int,
) {
    val color = (r shl 16) or (g shl 8) or b
    fun distanceSquared(other: TimestampedColor): Int {
        val dr = r - other.r
        val dg = g - other.g
        val db = b - other.b
        return dr * dr + dg * dg + db * db
    }

    override fun toString(): String {
        return """
            | Time: $time
            | Color: $r $g $b
        """.trimIndent()
    }
}

class TimestampedDisplayData(
    time: Int,
    override val vec: DisplayDataVector
) : TimestampedData(time) {
    override fun toString(): String {
        return """
            t: $time
            v: $vec
        """.trimIndent()
    }
}

private fun simplifyColors(
    toSimplify: MutableList<TimestampedColor>,
    tolerance: Double
): MutableList<TimestampedColor> {
    if (toSimplify.size < 3) return toSimplify


    val sqTolerance = tolerance * tolerance
    val radialSimplified: MutableList<TimestampedColor> = mutableListOf(toSimplify.first())

    var lastPoint = toSimplify.first()

    for (i in 1 until toSimplify.size - 1) {
        val currNode = toSimplify[i]
        if (lastPoint.distanceSquared(currNode) > sqTolerance) {
            radialSimplified += currNode
            lastPoint = currNode
        }
    }

    radialSimplified += toSimplify.last()

    return radialSimplified
}

private fun simplfiyLocs(
    toSimplify: MutableList<TimestampedPos>,
    tolerance: Double
): MutableList<TimestampedPos> {
    if (toSimplify.size < 3) return toSimplify

    val sqTolerance = tolerance * tolerance
    val radialSimplified: MutableList<TimestampedPos> = mutableListOf(toSimplify.first())

    var lastPoint = toSimplify.first()

    for (i in 1 until toSimplify.size - 1) {
        val currNode = toSimplify[i]
        if (lastPoint.vec.distanceSquared(currNode.vec) > sqTolerance) {
            radialSimplified += currNode
            lastPoint = currNode
        }
    }

    radialSimplified += toSimplify.last()

    return (listOf(toSimplify.first()) + douglas(
        sqTolerance,
        radialSimplified
    ) + listOf(toSimplify.last())).toMutableList()

}

private fun simplifyDisplayData(
    toSimplify: MutableList<TimestampedDisplayData>,
    tolerance: Double
): MutableList<TimestampedDisplayData> {
    if (toSimplify.size < 3) return toSimplify

    val sqTolerance = tolerance * tolerance
    val radialSimplified: MutableList<TimestampedDisplayData> = mutableListOf(toSimplify.first())

    var lastPoint = toSimplify.first()

    for (i in 1 until toSimplify.size - 1) {
        val currNode = toSimplify[i]
        if (lastPoint.vec.distanceSquared(currNode.vec) > sqTolerance) {
            radialSimplified += currNode
            lastPoint = currNode
        }
    }

    radialSimplified += toSimplify.last()

    return (listOf(toSimplify.first()) + douglas(
        sqTolerance,
        radialSimplified
    ) + listOf(toSimplify.last())).toMutableList()
}

private fun <T : TimestampedData> douglas(sqTolerance: Double, nodes: List<T>): List<T> {
    if (nodes.size < 3) return emptyList()

    val start = nodes.first().vec
    val end = nodes.last().vec

    val delta = end.clone().sub(start)
    val sqDeltaLength = delta.lengthSquared()
    if (CachedPath.DEBUG_DISPLAY_DATA >= 2 && start is DisplayDataVector) {
        println(
            "|||||||||||||||||||||||||||||||||\n" +
                    "| SIZE: ${nodes.size}\n" +
                    "-> START: \n" +
                    start + "\n" +
                    "-> END: \n" +
                    end + "\n" +
                    "-> DELTA: \n" +
                    delta + "\n" +
                    "| LENGTH: $sqDeltaLength\n"
        )
    }

    var max = 0.0
    var index = -1

    for (i in 1 until nodes.size - 1) {
        //guaranteed to be no division by 0 bcs of radial distance preprocessing
        val offset = start.clone().sub(nodes[i].vec)

        val sqDistance = if (sqDeltaLength <= 0.0)
            offset.lengthSquared()
        else ((offset.lengthSquared() * sqDeltaLength) - offset.dot(delta).pow(2)) / sqDeltaLength

        if (CachedPath.DEBUG_DISPLAY_DATA >= 2 && start is DisplayDataVector) {
            println(
                "-------------------------------\n" +
                        "| INDEX: $i\n" +
                        "-> OFFSET:\n" +
                        offset + "\n" +
                        "| SQ: $sqDeltaLength\n" +
                        "| DOT: ${offset.dot(delta)}\n" +
                        "| SQDISTANCE:${sqDistance}\n"
            )
        }

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