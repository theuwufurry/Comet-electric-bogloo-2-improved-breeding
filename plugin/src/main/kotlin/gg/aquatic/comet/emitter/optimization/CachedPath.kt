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

//        locations.replaceAll { _, v ->
//            val b = v.size
//            val r = simplfiyLocs(v, locTol)
//            if (DEBUG_LOCS >= 1) {
//                println(
//                    "--L--\n" + "$b -> ${r.size}"
//                )
//            }
//
//            if (DEBUG_LOCS >= 2) {
//                for (a in r) {
//                    println(a)
//                }
//            }
//
//            r
//        }

        internalTransformableData.forEach { (k, v) ->
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

            transformableData[k] = r
        }

//        transformableData.replaceAll { _, v ->
//            val b = v.size
//            val r = simplifyDisplayData(v, dispTol)
//
//            if (DEBUG_DISPLAY_DATA >= 1) {
//                print(
//                    "--DD--\n" + "$b -> ${r.size}"
//                )
//            }
//
//            if (DEBUG_DISPLAY_DATA >= 2) {
//                for (a in r) {
//                    println(a)
//                }
//            }
//
//            r
//        }
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
            val ddTimes = sortedSetOf<Int>()
            ddTimes += transformableTimes
            ddTimes += colorTexTimes
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
            val internalTransformables = internalTransformableData[id]!!
            //assemble tp locations
            val mappedLocs = r.tps.map { tpTime -> internalLocs.first { iLoc -> iLoc.time == tpTime } }.toMutableList()
            locations[id] = mappedLocs

            val allUpdates = r.updates.toSortedSet()
            allUpdates.addAll(r.ddUpdates)
            allUpdates.addAll(transformableTimes)
            allUpdates.addAll(colorTexTimes)

            if (DEBUG_LOCS >= 1) println("  | ALL UPDATES: $allUpdates")

            val mappedTransformables = allUpdates.map { u -> internalTransformables.first { iTransformable -> iTransformable.time == u } }.toMutableList()
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

class Interval(
    val startTime: Int, //inclusive
    val endTime: Int, //exclusive
    val transformables: List<TimestampedTransformableData>,
    val locs: List<TimestampedPos>
) {
    fun optimize(): Result {
        println("<<< OPTIMIZE >>>")
        val duration = endTime - startTime
        println(" | S, E, D: $startTime, $endTime, $duration")

        if (duration == 0) throw IllegalStateException("$startTime == $endTime !!")
        val myLocs = locs.during(startTime until endTime)
        if (myLocs.isEmpty()) {
            println(" | EMPTY LOCS")
            return Result(
                cost = 0.0,
                timedTeleportDurations = mapOf(),
                locUpdateTimes = listOf(),
            )
        }

        if (myLocs.size == 1) {
            println(" | 1 LOC at ${myLocs.first().time}")
            if (myLocs.first().time == startTime) {
                return Result(
                    cost = 1.0,
                    timedTeleportDurations = mapOf(startTime to duration),
                    locUpdateTimes = listOf(startTime),
                )
            }
        }

        println(" | LOCS:")
        for (loc in myLocs) {
            println("   | T: ${loc.time}, P: ${loc.vec.vec}")
        }

        var bestHarmonicCost = duration

        for (period in 2..(duration / 2)) {
            if (duration % period != 0) continue //now we have harmonic, check if it lines up with key points

            var valid = true
            for (loc in myLocs) {
                if ((loc.time - startTime) % period != 0) {
                    valid = false
                    break
                }
            }

            if (valid) {
                bestHarmonicCost = duration / period
            }
        }

        fun harmonicResult(): Result {
            val locUpdateTimes = mutableListOf<Int>()
            var t = startTime
            repeat(bestHarmonicCost) {
                locUpdateTimes += t
                t += duration / bestHarmonicCost
            }

            return Result(
                cost = bestHarmonicCost.toDouble(),
                timedTeleportDurations = mapOf(startTime to duration / bestHarmonicCost),
                locUpdateTimes = locUpdateTimes,
            )
        }

        println(" | BEST HARMONIC COST: $bestHarmonicCost")

        val myTransformables = transformables.during(startTime until endTime)

        println(" | TRANSFORMABLES:")
        for (t in myTransformables) {
            println("   | T: ${t.time}, S: ${t.vec.scale}")
        }

        if (myTransformables.isEmpty()) {
            return harmonicResult()
        }

        if (myTransformables.size == 1) {
            val first = myTransformables.first()
            if (first.time == startTime) {
                //only 1 display data, at the start, so this would mean further calls would be identical to this one. this means no display data optimizations possible, default to harmonic case
                return harmonicResult()
            }
        }

        var bestComplexCost = Double.MAX_VALUE
        var bestLeft: Result? = null
        var bestRight: Result? = null
        for (pivot in myTransformables) {
            println(" | PIVOT:")
            println("    | T: ${pivot.time}, S: ${pivot.vec.scale}")
            //pivot
            val left = Interval(
                startTime = startTime,
                endTime = pivot.time,
                transformables = transformables,
                locs = locs,
            ).let { optimize() }

            val right = Interval(
                startTime = pivot.time,
                endTime = endTime,
                transformables = transformables,
                locs = locs,
            ).let { optimize() }

            val cost = left.cost + right.cost
            if (cost < bestComplexCost) {
                bestComplexCost = cost
                bestLeft = left
                bestRight = right
            }
        }

        println(" | BEST COMPLEX COST: $bestComplexCost")

        if (bestComplexCost < bestHarmonicCost) {
            bestLeft!!; bestRight!!

            val ttd = bestLeft.timedTeleportDurations.toMutableMap()

            for ((t, d) in bestRight.timedTeleportDurations) {
                ttd[t] = d
            }

            return Result(
                cost = bestComplexCost,
                timedTeleportDurations = ttd,
                locUpdateTimes = bestLeft.locUpdateTimes.toMutableList().apply { addAll(bestRight.locUpdateTimes) },
            )
        } else {
            return harmonicResult()
        }
    }

    private fun <T : TimestampedData> List<T>.during(range: IntRange): List<T> {
        if (range.isEmpty()) return emptyList()

        return this.filter { it.time in range }
    }

    /*
    report back on when to set TPID and to what, and when loc updates should happen
     */
    class Result(
        val cost: Double,
        val timedTeleportDurations: Map<Int, Int>,
        val locUpdateTimes: List<Int>,
    )
}