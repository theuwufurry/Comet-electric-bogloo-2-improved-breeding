package gg.aquatic.comet.emitter.optimization.Mengshe

import gg.aquatic.comet.emitter.optimization.Mengshe.math.Quaternion
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.LongArrayList
import java.util.*
import kotlin.math.sqrt


/*
Context
- Teleportation updates can be sent periodically with no need for changing teleportation duration
- Teleportation duration change requires entity metadata update
- Entity metadata updates have to be sent regularly anyway so changing interpolation duration is not as big of a deal

Theory
- In order to allow maximum flexibility, have the optimizer take in data iteratively, not requiring the entire life to be known at start time
- Do this by feeding it timestamped data, and optimizer will return packets when it deems fit
- Max tolerance for every data type is provided
- Match up metadata updates with tp duration updates

an entity metadata update of any kind requires sending new data
enforce rules with cost function

entity metadata and location independently can have regions
    location region boundaries must align with entity metadata updates
    entity metadata region boundaries don't have special requirements

A region R with tp duration T has data that can be well approximated by a discrete point every T ticks

given metadata regions and loc regions... cost is calculated as the following:
// simplify bounds cost to update cost, so that gets merged into total_updates

metadata_regions.total_updates * UPDATE_COST +
loc_regions.total_tps * TP_COST +
loc_regions.bounds * (UPDATE_COST + TD_UPDATE_COST)

the text itself can also be changed in 2 ways for particle purposes...
    color: can be optimized
    content: cannot be optimized

for content updates, just detect when it changes and insert that as a mandatory display data update point

output:
    metadata region bounds
    loc region bounds
 */

/**
 * SINGLE THREADED!
 */
class LocalPacketOptimizer() {
    private var totalPosCycles = 0
    private var skippedCycles = 0

    fun optimizeSegmented(
        positionTolerance: Double,
        scaleTolerance: Double,
        rotTolerance: Double,
        colorTolerance: Double,
        opacityTolerance: Double,
        posData: List<TimestampedPos>,
        displayData: List<TimestampedDisplayData>,
        textData: List<TimestampedContentData>,
        costs: Costs,
        debugInfo: Boolean,
        interval: Int,
    ): OptimizedPath {
        check(posData.size == displayData.size)
        check(displayData.size == textData.size)
        val pathSize = posData.size

        if (pathSize <= interval) {
            return optimize(
                positionTolerance,
                scaleTolerance,
                rotTolerance,
                colorTolerance,
                opacityTolerance,
                posData,
                displayData,
                textData,
                costs,
                debugInfo,
            )
        }

        val chunkedPosData = posData.chunked(interval)
        val chunkedDisplayData = displayData.chunked(interval)
        val chunkedTextData = textData.chunked(interval)

        val numChunks = (pathSize + interval - 1) / interval
        check(chunkedPosData.size == numChunks)

        val full = OptimizedPath(mutableListOf(), mutableListOf(), mutableListOf())

        for (i in 0..<(numChunks - 1)) {
            val start = i * interval
            val path = optimize(
                positionTolerance = positionTolerance,
                scaleTolerance = scaleTolerance,
                rotTolerance = rotTolerance,
                colorTolerance = colorTolerance,
                opacityTolerance = opacityTolerance,
                posData = chunkedPosData[i],
                displayData = chunkedDisplayData[i],
                textData = chunkedTextData[i],
                costs = costs,
                debugInfo = debugInfo,
            )

            path.positions.removeLast()
            path.displayData.removeLast()
            path.textData.removeLast()

            full.positions += path.positions.map { it + start }
            full.displayData += path.displayData.map { it + start }
            full.textData += path.textData.map { it + start }
        }

        val lastPath = optimize(
            positionTolerance = positionTolerance,
            scaleTolerance = scaleTolerance,
            rotTolerance = rotTolerance,
            colorTolerance = colorTolerance,
            opacityTolerance = opacityTolerance,
            posData = chunkedPosData.last(),
            displayData = chunkedDisplayData.last(),
            textData = chunkedTextData.last(),
            costs = costs,
            debugInfo = debugInfo,
        )

        full.positions += lastPath.positions.map { it + (numChunks - 1) * interval }
        full.displayData += lastPath.displayData.map { it + (numChunks - 1) * interval }
        full.textData += lastPath.textData.map { it + (numChunks - 1) * interval }

        return full
    }

    private val _posData = PosArrayList()

    private val _minPosCosts = DoubleArrayList()
    private val _posPath = IntArrayList()

    private val _posRegionIndices = IntArrayList()

    private val _minDisplayCosts = DoubleArrayList()
    private val _displayPath = IntArrayList()

    private val _displayIndices = IntArrayList()

    fun optimize(
        positionTolerance: Double,
        scaleTolerance: Double,
        rotTolerance: Double,
        colorTolerance: Double,
        opacityTolerance: Double,
        posData: List<TimestampedPos>,
        displayData: List<TimestampedDisplayData>,
        textData: List<TimestampedContentData>,
        costs: Costs,
        debugInfo: Boolean,
    ): OptimizedPath {
        totalPosCycles = 0
        skippedCycles = 0

        _posData.from(posData)

        _minPosCosts.size(posData.size)
        Arrays.fill(_minPosCosts.elements(), Double.MAX_VALUE)
        _minPosCosts.set(0, 0.0)

        _posPath.size(posData.size)
        Arrays.fill(_posPath.elements(), -1)

        var i = 0
        while (i < _posData.size) {
            var j = 0
            while (j < i) {
                val divideBest = _minPosCosts.getDouble(j)
                val currBest = _minPosCosts.getDouble(i)
                val currentCost = divideBest + _posData.posRegionCost(
                    positionTolerance,
                    j,
                    i,
                    costs,
                    bestCost = currBest,
                    divideCost = divideBest
                )

                if (currentCost < currBest) {
                    _minPosCosts.set(j, currentCost)
                    _posPath.set(i, j)
                }

                j++
            }

            i++
        }

        _posRegionIndices.clear()
        _posRegionIndices.add(_posPath.size - 1)

        var k = _posPath.getInt(_posPath.size - 1)
        do {
            _posRegionIndices.add(0, k)
            k = _posPath.getInt(k)
        } while (k != -1)

        if (debugInfo) {
            println("posRegionIndices: $_posRegionIndices")
        }

        val actualizedPositions = IntArrayList()
        _posRegionIndices.actualizePositions(positionTolerance, _posData, costs, actualizedPositions)

        val updateAreas = constructPosTPUpdateAreas(
            regions = _posRegionIndices,
            actual = actualizedPositions,
        )

        val optimizedTextData = textData.deltas(colorTolerance)
        val anchors = optimizedTextData

        if (debugInfo) {
            println("anchors: $anchors")
        }

        _minDisplayCosts.size(displayData.size)
        Arrays.fill(_minDisplayCosts.elements(), Double.MAX_VALUE)
        _minDisplayCosts.set(0, 0.0)

        _displayPath.size(displayData.size)
        Arrays.fill(_displayPath.elements(), -1)

        var l = 0
        while (l < displayData.size) {
            var m = 0
            while (m < l) {
                val currBest = _minDisplayCosts.getDouble(l)
                val divideBest = _minDisplayCosts.getDouble(m)
                val currentCost = divideBest + displayData.displayRegionCost(
                    scaleTolerance, rotTolerance, opacityTolerance,
                    anchors,
                    updateAreas,
                    m,
                    l,
                    costs,
                    bestCost = currBest, divideCost = divideBest
                )

                if (currentCost < currBest) {
                    _minDisplayCosts.set(l, currentCost)
                    _displayPath.set(l, m)
                }

                m++
            }

            l++
        }

        _displayIndices.clear()
        if (displayData.isNotEmpty()) {
            _displayIndices += _displayPath.size - 1

            var n = _displayPath.getInt(_displayPath.size - 1)
            do {
                _displayIndices.addFirst(n)
                n = _displayPath.getInt(n)
            } while (n != -1)
        }

        if (debugInfo) {
            println("displayIndices: $_displayIndices")
        }

        return OptimizedPath(
            actualizedPositions,
            _displayIndices.actualizeDisplayData(
                scaleTolerance,
                rotTolerance,
                opacityTolerance,
                displayData,
                anchors,
                updateAreas,
                costs
            ),
            optimizedTextData,
        )
    }

    fun List<TimestampedContentData>.deltas(ctol: Double): IntArrayList {
        if (this.isEmpty()) return IntArrayList()
        if (this.size == 1) return IntArrayList()

        val out = IntArrayList()

        var currContent = first().content
        var currColor = first().color
        var i = 1
        while (i < size) {
            if (this[i].content != currContent || currColor.scaleDistance(this[i].color) > ctol) {
                out += i
                currContent = this[i].content
                currColor = this[i].color
            }

            i++
        }

        out.add(size - 1)

        return out
    }

    fun IntArrayList.actualizePositions(
        tolerance: Double,
        positions: PosArrayList,
        costs: Costs,
        out: IntArrayList,
    ) {
        var i = 0
        while (i < size - 1) {
            val start = this.getInt(i)
            val end = this.getInt(i + 1)

            val duration = end - start
            var t = duration

            var minCost = duration * costs.tpCost
            var bestPeriod = 1

            while (t > 1) {
                if (duration % t != 0) {
                    t--
                    continue
                }

                var valid = true
                // test if this period fits the data within tolerance
                run check@{
                    var j = start
                    while (j < end) {
                        // test all points (j + 1)..<(j + t)
                        val sx = positions.x(j)
                        val sy = positions.y(j)
                        val sz = positions.z(j)

                        val ex = positions.x(j + t)
                        val ey = positions.y(j + t)
                        val ez = positions.z(j + t)

                        var k = j + 1
                        while (k < j + t) {
                            val f = (k - j) / t.toDouble()
                            val px = positions.x(k)
                            val py = positions.y(k)
                            val pz = positions.z(k)

                            val d = distance(
                                px, py, pz,
                                (ex - sx) * f + sx,
                                (ey - sy) * f + sy,
                                (ez - sz) * f + sz,
                            )

                            if (d > tolerance) {
                                valid = false
                                return@check
                            }

                            k++
                        }

                        j += t
                    }
                }

                if (valid) {
                    val c = duration / t * costs.tpCost
                    if (c < minCost) {
                        minCost = c
                        bestPeriod = t
                    }
                }

                t--
            }

            var q = start
            while (q < end) {
                out += q

                q += bestPeriod
            }

            i++
        }

        out += last()
    }

    fun IntArrayList.actualizeDisplayData(
        scaleTolerance: Double, rotTolerance: Double, opacityTolerance: Double,
        displayData: List<TimestampedDisplayData>,
        anchors: IntArrayList,
        tpUpdateAreas: List<Pair<Int, Int>>,
        costs: Costs,
    ): IntArrayList {
        if (isEmpty()) return IntArrayList()

        val out = IntArrayList()
        var i = 0
        while (i < size - 1) {
            val start = this.getInt(i)
            val end = this.getInt(i + 1)

            val bestPeriod = bestDisplayPeriod(
                displayData = displayData,
                scaleTolerance = scaleTolerance,
                rotTolerance = rotTolerance,
                opacityTolerance = opacityTolerance,
                anchors = anchors,
                tpUpdateAreas = tpUpdateAreas,
                start = start, end = end,
                costs = costs,
                quitEarly = false, bestCost = 0.0, divideCost = 0.0
            )

            var q = start
            while (q < end) {
                out += q

                q += bestPeriod
            }

            i++
        }

        out += last()

        return out
    }

    /**
     * @param start inclusive
     * @param end exclusive, but must be in array
     * @return Double.MAX_VALUE if cost cannot be smaller than besTcost
     */
    fun PosArrayList.posRegionCost(
        tolerance: Double,
        start: Int, end: Int,
        costs: Costs,
        bestCost: Double, divideCost: Double,
    ): Double {
        require(end > start)
        require(start >= 0)
        require(end <= size)

        val duration = end - start
        var t = duration

        val minCost = duration * costs.tpCost + costs.updateCost.toDouble()

        while (t > 1) {
            if (duration % t != 0) {
                t--
                continue
            }

            var valid = true
            // test if this period fits the data within tolerance
            run check@{
                var j = start
                while (j < end) {
                    val exIndex = (j + t).coerceAtMost(end - 1) // clamp to last valid index
                    val sx = x(j)
                    val sy = y(j)
                    val sz = z(j)

                    val ex = x(exIndex)
                    val ey = y(exIndex)
                    val ez = z(exIndex)

                    var k = j + 1
                    while (k < exIndex) {
                        val f = (k - j) / t.toDouble()
                        val px = x(k)
                        val py = y(k)
                        val pz = z(k)

                        val d = distance(
                            px, py, pz,
                            (ex - sx) * f + sx,
                            (ey - sy) * f + sy,
                            (ez - sz) * f + sz,
                        )

                        if (d > tolerance) {
                            valid = false
                            return@check
                        }

                        k++
                    }

                    j += t
                }
            }

            if (valid) {
                val c = duration / t * costs.tpCost
                return c + costs.updateCost.toDouble()
            }

            t--
        }

        return minCost
    }

    /**
     * Constructs the areas where teleportation duration can be updated
     */
    fun constructPosTPUpdateAreas(
        regions: List<Int>,
        actual: List<Int>,
    ): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var i = 1
        var j = -1
        while (i < regions.size - 1) { // don't create an update region for the last one
            val r = regions[i]
            while (j + 1 < actual.size && actual[j + 1] < r) {
                j++
            }

            if (j != -1) {
                // for first region we need to give extra 1 tick buffer
                check(actual[j] < r)
                if (i == 1) {
                    if (actual[j] + 1 >= r) {
//                    println("| FAILURE! ${actual[j] + 1} -> r")
                    } else {
//                    println("|0 updateArea: ${actual[j] + 1} -> $r")
                        out += (actual[j] + 1) to r
                    }
                } else {
//                println("| updateArea: ${actual[j]} -> $r")
                    out += actual[j] to r
                }
            }

            i++
        }

        return out
    }

    fun List<TimestampedDisplayData>.displayRegionCost(
        scaleTolerance: Double, rotTolerance: Double, opacityTolerance: Double,
        anchors: IntArrayList,
        tpUpdateAreas: List<Pair<Int, Int>>,
        start: Int, end: Int,
        costs: Costs,
        bestCost: Double, divideCost: Double,
    ): Double {
        require(end > start)
        require(start >= 0)
        require(end <= size)

        /*
        we must ensure that we send an update somewhere between the the last tp before a region update (inclusive) and the region update (exclusive)
        so we have a list of ranges which we need to fill
            min list
            max list
        a region will never try to cover an entry in its list which goes outside of its max bounds (avoid duplicates)
         */

        val duration = end - start

        val bestPeriod = bestDisplayPeriod(
            displayData = this,
            scaleTolerance = scaleTolerance,
            rotTolerance = rotTolerance,
            opacityTolerance = opacityTolerance,
            anchors = anchors,
            tpUpdateAreas = tpUpdateAreas,
            start = start, end = end,
            costs = costs,
            quitEarly = true, bestCost = bestCost, divideCost = divideCost,
        )

        if (bestPeriod == -1) {
            return Double.MAX_VALUE
        }

        val cost = duration / bestPeriod * costs.updateCost

        return cost.toDouble()
    }

    private val _relevantAnchors = IntArrayList()
    private val _relevantUpdateAreas = LongArrayList()

    private val _quat = Quaternion(0.0, 0.0, 0.0, 0.0)

    fun bestDisplayPeriod(
        displayData: List<TimestampedDisplayData>,
        scaleTolerance: Double, rotTolerance: Double, opacityTolerance: Double,
        anchors: IntArrayList,
        tpUpdateAreas: List<Pair<Int, Int>>,
        start: Int, end: Int,
        costs: Costs,
        quitEarly: Boolean,
        bestCost: Double, divideCost: Double,
    ): Int {
        _relevantAnchors.clear()
        for (m in 0 until anchors.size) {
            val a = anchors.getInt(m)
            if (a in start until end) {
                _relevantAnchors.add(a)
            }
        }

        _relevantUpdateAreas.clear()
        for (m in 0 until tpUpdateAreas.size) {
            val s = tpUpdateAreas[m].first
            val e = tpUpdateAreas[m].second
            if (s < end && e > start) {
                _relevantUpdateAreas.add((s.toLong() shl 32) or e.toLong())
            }
        }

        val maxDuration = end - start
        var t = maxDuration

        while (t > 1) {
            if (quitEarly && maxDuration / t * costs.updateCost + divideCost >= bestCost) {
                return -1
            }

            if (maxDuration % t != 0) {
                t--
                continue
            }

            // Ensure all anchors in range are covered
            var anchorsValid = true
            for (m in 0 until _relevantAnchors.size) {
                if ((_relevantAnchors.getInt(m) - start) % t != 0) {
                    anchorsValid = false
                    break
                }
            }
            if (!anchorsValid) {
                t--
                continue
            }

            // Ensure all tpUpdateAreas in range are compatible
            var areasValid = true
            for (m in 0 until _relevantUpdateAreas.size) {
                val l = _relevantUpdateAreas.getLong(m)
                val s = (l ushr 32).toInt()
                val e = l.toInt()
                if (!((s - start) % t == 0 || s + (t - (s - start) % t) < e)) {
                    areasValid = false
                    break
                }
            }
            if (!areasValid) {
                t--
                continue
            }

            // Validate display data within tolerance
            var valid = true
            run check@{
                var j = start
                while (j < end) {
                    val eIndex = (j + t).coerceAtMost(end - 1)
                    val sData = displayData[j]
                    val eData = displayData[eIndex]

                    var k = j + 1
                    while (k < eIndex) {
                        val f = (k - j) / t.toDouble()
                        val p = displayData[k]

                        val opacityDist = p.opacityDistance(interpolateOpacity(sData.opacity, eData.opacity, f))
                        if (opacityDist > opacityTolerance) {
                            valid = false
                            return@check
                        }

                        val d = p.scaleDistance(
                            (eData.scaleX - sData.scaleX) * f + sData.scaleX,
                            (eData.scaleY - sData.scaleY) * f + sData.scaleY,
                            (eData.scaleZ - sData.scaleZ) * f + sData.scaleZ,
                        )
                        if (d > scaleTolerance) {
                            valid = false
                            return@check
                        }

                        k++
                    }

                    j += t
                }

                // rotation checks after scale/opacity
                var i = start
                while (i < end) {
                    val eIndex = (i + t).coerceAtMost(end - 1)
                    val sData = displayData[i]
                    val eData = displayData[eIndex]

                    var k = i + 1
                    while (k < eIndex) {
                        val f = (k - i) / t.toDouble()
                        val p = displayData[k]

                        val q = _quat.set(sData.rot.x, sData.rot.y, sData.rot.z, sData.rot.w).nlerp(eData.rot, f)
                        if (p.rotDistance(q) > rotTolerance) {
                            valid = false
                            return@check
                        }

                        k++
                    }

                    i += t
                }
            }

            if (valid) return t

            t--
        }

        return 1
    }
}

fun distance(
    a: Double, b: Double, c: Double,
    x: Double, y: Double, z: Double,
): Double {
    return sqrt((a - x) * (a - x) + (b - y) * (b - y) + (c - z) * (c - z))
}