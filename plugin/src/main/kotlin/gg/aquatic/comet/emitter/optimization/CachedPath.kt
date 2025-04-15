package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.emitter.optimization.vec.DisplayDataVector
import gg.aquatic.comet.emitter.optimization.vec.Vec
import gg.aquatic.comet.emitter.optimization.vec.WrappedPos
import java.util.*
import kotlin.math.pow
import kotlin.system.measureNanoTime

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
    var internalLocations: MutableMap<UUID, MutableList<TimestampedPos>> = mutableMapOf()
    var internalTransformableData: MutableMap<UUID, MutableList<TimestampedTransformableData>> = mutableMapOf()

    var locations: MutableMap<UUID, MutableList<TimestampedPos>> = mutableMapOf()
    var transformableData: MutableMap<UUID, MutableList<TimestampedTransformableData>> = mutableMapOf()

    var coloredTextureData: MutableMap<UUID, MutableList<TimestampedColoredTexture>> = mutableMapOf()

    fun optimize(): CachedPath {
        internalLocations.forEach { (k, v) ->
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

            locations[k] = r
        }

        internalTransformableData.forEach { (k, v) ->
            val b = v.size
            val r = simplifyDisplayData(v, dispTol)

            if (DEBUG_DISPLAY_DATA >= 1) {
                println(
                    "--DD--\n" + "$b -> ${r.size}"
                )
            }

            if (DEBUG_DISPLAY_DATA >= 2) {
                for (a in r) {
                    println(a)
                }
            }

            transformableData[k] = r
        }

        coloredTextureData.replaceAll { _, v ->
            val b = v.size
            val r = simplifyColors(v, colTol)
            if (DEBUG_COL_TEX >= 1) {
                println("--CD--\n" + "$b -> ${r.size}")
            }

            if (DEBUG_COL_TEX >= 2) {
                for (a in r) {
                    println(a)
                }
            }

            r

        }

        for (id in hashes.keys) {
            val locs = locations[id]!!
            val transformables = transformableData[id]!!
            val colorTex = coloredTextureData[id]!!

            val locTimes = locs.map { it.time }
            val transformableTimes = transformables.map { it.time }
            val colorTexTimes = colorTex.map { it.time }
            val ddTimes = mutableListOf<Int>()

            ddTimes += transformableTimes
            ddTimes += colorTexTimes
            val internalTransformables = internalTransformableData[id]!!
            val filledTimes = fillTransparency(internalTransformables)
            if (DEBUG_DISPLAY_DATA >= 1) println("  | FILLED: $filledTimes")
            ddTimes += filledTimes
            ddTimes.sort()
            val r: OptimizationResult
            if (DEBUG_LOCS >= 1) println("===============")
            if (DEBUG_LOCS >= 1) println("  | LOC TIMES: $locTimes")
            if (DEBUG_LOCS >= 1) println("  | T TIMES: $transformableTimes")
            if (DEBUG_LOCS >= 1) println("  | TEX TIMES: $colorTexTimes")
            if (DEBUG_LOCS >= 1) println("  | DDTIMES: $ddTimes")
            val t = measureNanoTime { r = actualize(optimize(locTimes, ddTimes.toList())) }

//            val interval = Interval(
//                startTime = 0,
//                endTime = locs.last().time,
//                locs = locs,
//                transformables = transformables,
//            )
//
//            val result = interval.optimize()

            if (DEBUG_LOCS >= 1) println("[[[[[[ OPTIMIZED ]]]]]]")
            if (DEBUG_LOCS >= 1) println(" | TOOK: ${t / 1_000_000.0} ms")
            if (DEBUG_LOCS >= 1) println(" | TPS: ${r.tps}")
            if (DEBUG_LOCS >= 1) println(" | UPDATES: ${r.updates}")
            if (DEBUG_LOCS >= 1) println(" | DD: ${r.ddUpdates}")
            if (DEBUG_LOCS >= 1) println(" | COST: ${r.cost}")

            val internalLocs = internalLocations[id]!!
            //assemble tp locations
            val mappedLocs = r.tps.map { tpTime -> internalLocs.first { iLoc -> iLoc.time == tpTime } }.toMutableList()
            locations[id] = mappedLocs

            val allUpdates = r.updates.toSortedSet()
            allUpdates.addAll(r.ddUpdates)
            allUpdates.addAll(transformableTimes)
            allUpdates.addAll(filledTimes)
            allUpdates.addAll(colorTexTimes)

            if (DEBUG_LOCS >= 1) println("  | ALL UPDATES: $allUpdates")

            val mappedTransformables =
                allUpdates.map { u -> internalTransformables.first { iTransformable -> iTransformable.time == u } }
                    .toMutableList()
            transformableData[id] = mappedTransformables
        }

        return this
    }

    companion object {
        // 0 - off, 1 - size, 2 - list
        val DEBUG_LOCS = 0
        val DEBUG_DISPLAY_DATA = 0
        val DEBUG_COL_TEX = 0
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

class TimestampedColoredTexture(
    val time: Int,
    private val r: Int,
    private val g: Int,
    private val b: Int,
    val displayData: DisplayData,
) {
    val color = (r shl 16) or (g shl 8) or b
    fun distanceSquared(other: TimestampedColoredTexture): Int {
        val dr = r - other.r
        val dg = g - other.g
        val db = b - other.b
        return dr * dr + dg * dg + db * db
    }

    override fun toString(): String {
        return """
            | Time: $time
            | Color: $r $g $b
            | DisplayData: $displayData
        """.trimIndent()
    }
}

class TimestampedTransformableData(
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
    toSimplify: MutableList<TimestampedColoredTexture>,
    tolerance: Double
): MutableList<TimestampedColoredTexture> {
    if (toSimplify.size < 3) return toSimplify

    val sqTolerance = tolerance * tolerance
    val radialSimplified: MutableList<TimestampedColoredTexture> = mutableListOf(toSimplify.first())

    var lastPoint = toSimplify.first()

    for (i in 1 until toSimplify.size - 1) {
        val currNode = toSimplify[i]
        if (lastPoint.displayData != currNode.displayData || lastPoint.distanceSquared(currNode) > sqTolerance) {
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

/**
 * Fills in 127 boundary crosses
 * @return Times of filled transparencies.
 */
private fun fillTransparency(
    full: MutableList<TimestampedTransformableData>,
): List<Int> {
    val times = mutableListOf<Int>()
    var prev = full.first()
    for (i in 1..<full.size) {
        val curr = full[i]

        if (prev.vec.alpha * 255.0 > 127.0 && curr.vec.alpha * 255.0 <= 127.0) {
            times += prev.time
            times += curr.time
        }

        prev = curr
    }

    return times
}

private fun simplifyDisplayData(
    toSimplify: MutableList<TimestampedTransformableData>,
    tolerance: Double
): MutableList<TimestampedTransformableData> {
    if (toSimplify.size < 3) return toSimplify

    val sqTolerance = tolerance * tolerance
    val radialSimplified: MutableList<TimestampedTransformableData> = mutableListOf(toSimplify.first())

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