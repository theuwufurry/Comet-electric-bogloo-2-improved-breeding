package gg.aquatic.comet.emitter.optimization

import kotlin.math.ceil

/**
 * Utility to solve the minimum packet problem, currently can analyze costs of updating teleportation duration on input teleport times, as well as taking in display data update times and attempting to use those (as their cost is lower) to find optimal packet schedule.
 * Lacking ability to evaluate costs of placing update times at points other than given teleport times, and naively trying to have many display data points to emulate this leads to extremely slow runtimes
 * Need better heuristics needed to quicker converge on solution.
 * Need a method to ensure some frequency of updates.
 * @author !xume
 */

/*
TODO: update costs to match real packets
 */
const val UPDATE_COST = 4.0
const val TP_COST = 2.0
const val ADDON_COST = 0.5
const val MAX_INTERVAL = 20

/**
 * Optimized result
 * @param updates The times at which teleportation duration updates should happen
 * @param ddUpdates The times at which teleportation duration updates should happen, but all ddUpdates are also addons to existing display data updates
 * @param tps Times at which teleports happen
 * @param cost Cost of path
 */
data class OptimizationResult(
    val updates: List<Int>,
    val ddUpdates: List<Int>,
    val tps: List<Int>,
    val cost: Double
)


fun optimize(
    tps: List<Int>,
    ddTimes: List<Int>,
): OptimizationResult {
    if (tps.size < MAX_INTERVAL) return optimizeSplit(tps, ddTimes)

    val numSplits = ceil(tps.size.toDouble() / MAX_INTERVAL.toDouble()).toInt() - 1
    val step = tps.size / (numSplits + 1) + 1

    // 50 / 3 = 16, 48,

    val aggregateUpdates = mutableSetOf<Int>()
    val aggregateDDUpdates = mutableSetOf<Int>()
    val aggregateTps = mutableSetOf<Int>()

    println("SIZE: ${tps.size}")

    for (i in tps.indices step step) {
        val end = (i + step).coerceAtMost(tps.size - 1)
        println("  | INDEX: $i -> $end")
        val r = optimizeSplit(tps.subList(i, end), ddTimes)
        aggregateUpdates += r.updates
        aggregateDDUpdates += r.ddUpdates
        aggregateTps += r.tps
    }

    return OptimizationResult(
        aggregateUpdates.toList(),
        aggregateDDUpdates.toList(),
        aggregateTps.toList(),
        cost(
            updates = aggregateUpdates.toList(),
            tps = aggregateTps.toList(),
            debug = false,
            ddTimes = aggregateDDUpdates.toList(),
            punishing = false
        )
    )
}

/**
 * Optimized teleportation duration updates
 * @param tps Teleport times
 * @param ddTimes Times at which display data updates are already scheduled to happen (teleportation duration updates are cheaper here)
 */
private fun optimizeSplit(
    tps: List<Int>,
    ddTimes: List<Int>,
): OptimizationResult {
//    tpTimes += ddTimes

    if (tps.isEmpty()) return OptimizationResult(emptyList(), emptyList(), emptyList(), 0.0)
    var bestCost = UPDATE_COST + TP_COST * ((tps.last() - tps.first()) + 1)
    var bestVariation: Pair<List<Int>, List<Int>>? = null
    if (tps.size < 3) return OptimizationResult(listOf(tps.first()), emptyList(), tps, bestCost)

    //(splits + 1) * (update_cost * tp_cost)
//    var i = 0
    //(ddTimes.size).coerceAtMost(updates) * ADDON_COST + ((updates - ddTimes.size).coerceAtLeast(0) + 1) * UPDATE_COST
    for (updates in 0..(tps.size - 2)) {
        //complete upper bound
        if ((ddTimes.size).coerceAtMost(updates) * ADDON_COST + ((updates - ddTimes.size).coerceAtLeast(0) + 1) * UPDATE_COST + TP_COST * tps.size > bestCost) break
//        println("updates: $updates")
        val variations = orderedSplits(tps, updates)!!
//        println("$updates: ${variations.size}")

        for (updatesVariation in variations) {
//            println("updatesvariation: $updatesVariation")
            val ddVariations = ddSwaps(tps, updatesVariation, ddTimes)
            for (ddVariation in ddVariations) {
//                println("  | diddVariation: $ddVariation")
                val newTps = mutableListOf<Int>()
                newTps.addAll(tps)
                newTps.addAll(ddVariation.second)
                newTps.sort()

                val c =
                    cost(ddVariation.first, newTps, false, bestCost, ddVariation.second, true)
                if (c < bestCost) {
                    bestCost = c
                    bestVariation = ddVariation
                }
            }
//            println(updatesVariation)
//            i++
        }
    }

//    println("i: $i")

    return if (bestVariation == null) {
        return OptimizationResult(listOf(tps.first()), emptyList(), tps, bestCost)
    } else {
        val newTps = tps.toSortedSet()
        newTps.addAll(bestVariation.second)

        OptimizationResult(
            bestVariation.first,
            bestVariation.second,
            newTps.toList(),
            bestCost
        )
    }
}

/**
 * @return Returns update TIMES of all permutations with given number of splits
 */
fun orderedSplits(
    tps: List<Int>,
    splits: Int
): List<List<Int>>? {
    if (tps.size < splits + 1) return null
    if (splits == 0) return listOf(listOf(tps.first()))

    val result: MutableList<List<Int>> = mutableListOf()

    for (p in 1 until tps.size - 1) {
        val times = tps.subList(1, p + 2)
        val duration = times.last() - times.first()
        var f = false

        for (period in (duration).downTo(2)) {
            if (duration % period != 0) continue
            var found = true
            for (t in times) {
                if ((t - times.first()) % period != 0) {
                    found = false
                    break
                }
            }

            if (found) {
                f = true
                break
            }
        }

        if (!f && duration * TP_COST > (UPDATE_COST + TP_COST) * (p - 1)) {
            continue
        }

        val right = _orderedSplits(tps.subList(p, tps.size), splits - 1) ?: continue
        for (variation in right) {
            val r = mutableListOf<Int>()
            r += tps.first()
            r += tps[p]
            r += variation

            result += r
        }
    }

    return result
}

fun _orderedSplits(
    tps: List<Int>,
    updates: Int,
): List<List<Int>>? {
    if (tps.size < updates + 1) return null
    if (updates == 0) return listOf(listOf())

    val result: MutableList<List<Int>> = mutableListOf()

    for (p in 1 until tps.size - 1) {
        val times = tps.subList(1, p + 2)
        val duration = times.last() - times.first()
        var f = false

        for (period in (duration).downTo(2)) {
            if (duration % period != 0) continue
            var found = true
            for (t in times) {
                if ((t - times.first()) % period != 0) {
                    found = false
                    break
                }
            }

            if (found) {
                f = true
                break
            }
        }

        if (!f && duration * TP_COST > (UPDATE_COST + TP_COST) * (p - 1)) {
            continue
        }

        val right = _orderedSplits(tps.subList(p, tps.size), updates - 1) ?: continue
        for (variation in right) {
            val r = mutableListOf<Int>()
            r += tps[p]
            r += variation

            result += r
        }
    }

    return result
}



//TODO: Properly adjust for all possible ddtimes in the same update time teleport time interval
/**
 * @param tps Teleport times
 * @param updateTimes TP update times
 * @param ddTimes Display data update times
 * @return Variations of updates, accounting for collisions with ddTimes; first value represents updates, second represents ddTimes. ALL AS TIMES
 */
private fun ddSwaps(
    tps: List<Int>,
    updateTimes: List<Int>,
    ddTimes: List<Int>,
): List<Pair<List<Int>, List<Int>>> {
    if (ddTimes.isEmpty()) return listOf(updateTimes to emptyList())
    val results = mutableListOf<Pair<List<Int>, List<Int>>>()

    //collision is a dd before/on an update and after a teleport
    //if collision has the same ultimate update, choose dd closest to update
    //this ensures all cases covered over permutations
    //forget about the one before that, it has already been dealt with at an earlier permutation
    var collisionTime: Int? = null
    var collisionTimeIndex: Int? = null
    var collisionUpdate: Int? = null
    var collisionUpdateIndex: Int? = null
    for ((i, ddTime) in ddTimes.withIndex()) {
//        println("  | i: $i, ddTime: $ddTime")
        //find update time right after ddtime
        val ci = updateTimes.indexOfFirst { it >= ddTime }
        if (ci == -1) {
            continue
        }

        val cv = updateTimes[ci]

//        println("  | ci: $ci, cv: $cv")
        //if the collision update isn't the same as the found collision update, then this is a new interval and we dno't need to search any longer
        if (collisionUpdate != null && collisionUpdate != cv) {
            break
        } else if (collisionUpdate != null) {
            //we have the same update found, this means it MUST be after a found one already
            collisionTime = ddTime
            collisionTimeIndex = i
            collisionUpdate = cv
            collisionUpdateIndex = ci
            continue
        }

        //find teleport right before that update time
        val ctpi = tps.indexOfLast { it < cv }
        if (ctpi == -1) {
            continue
        }

//        println("  | ctpi: $ctpi")

        //check if ddtime is after penultimate teleport
        if (tps[ctpi] >= ddTime) {
            continue
        }

        collisionTime = ddTime
        collisionTimeIndex = i
        collisionUpdate = cv
        collisionUpdateIndex = ci
    }

    if (collisionTime == null) return listOf(updateTimes to emptyList())
    collisionTimeIndex!!
    collisionUpdate!!
    collisionUpdateIndex!!

//    println("""
//        collisionTime: $collisionTime
//        collisionTimeIndex: $collisionTimeIndex
//        collisionUpdate: $collisionUpdate
//        collisionUpdateIndex: $collisionTimeIndex
//    """.trimIndent())

    val fa = mutableListOf<Int>()
    val tr = mutableListOf<Int>()

    //add all updates BEFORE collision update, as that's the modulated one
    for (ui in 0 until collisionUpdateIndex) {
        fa += updateTimes[ui]
        tr += updateTimes[ui]
    }

    if (collisionUpdateIndex == updateTimes.size - 1) {
        //last update, so no right
        results += (updateTimes to emptyList())
        results += (tr to listOf(collisionTime))

        return results
    }

    if (collisionTimeIndex == ddTimes.size - 1) {
        //last collision possible, so the "right" from here would just be the remaining updates
        results += (updateTimes to emptyList())

        for (j in (collisionUpdateIndex + 1) until updateTimes.size) {
            tr += updateTimes[j]
        }

        val tr2 = listOf(collisionTime)
        results += (tr to tr2)

        return results
    }


    //now fa and tr have all the update times up to the collision one
    //for fa, we don't do anything with the collision and then add right
    //for tr, we add the collision and don't add the update, then add right
    //right needs to hold tps >= collision update time (can hold all tps tho)
    //right needs to hold update times > collision update time
    //right needs to hold dd's > collision time
//    val right = ddSwaps(updateIndices.subList(ui + 1, updateIndices.size), ddIndices.subList(1, ddIndices.size))
    val right = ddSwaps(
        tps,
        updateTimes.subList(collisionUpdateIndex + 1, updateTimes.size),
        ddTimes.subList(collisionTimeIndex + 1, ddTimes.size)
    )
    for (variation in right) {
        //keep update
        run {
            val newFirst = mutableListOf<Int>()
            newFirst.addAll(fa)
            newFirst.add(collisionUpdate)
            newFirst.addAll(variation.first)

            val newSecond = mutableListOf<Int>()
            newSecond.addAll(variation.second)

            results += newFirst to newSecond
        }

        //keep dd
        run {
            val newFirst = mutableListOf<Int>()
            newFirst.addAll(fa)
            newFirst.addAll(variation.first)

            val newSecond = mutableListOf<Int>()
            newSecond.add(collisionTime)
            newSecond.addAll(variation.second)

            results += newFirst to newSecond
        }
    }

    return results
}

fun cost(
    updates: List<Int>,
    tps: List<Int>,
    debug: Boolean = false,
    best: Double = Double.MAX_VALUE,
    ddTimes: List<Int>,
    punishing: Boolean
): Double {
    if (debug) println("updates: $updates")
    if (debug) println("tps: $tps")
    if (tps.isEmpty()) return 0.0
    if (tps.size == 1) return TP_COST + updates.size * UPDATE_COST
    if (updates.isEmpty()) throw IllegalStateException("Updates empty for $tps")

    val updateTimes = updates.toSortedSet()
    updateTimes.addAll(ddTimes)
    updateTimes.add(tps.last())
    if (debug) println("updates times: $updateTimes")

    var cost = updates.size * UPDATE_COST/* + ddTimes.filter { it !in tps }.size * TP_COST*/ + ddTimes.size * ADDON_COST
    if (debug) println("  | cost: $cost")
    if (punishing && cost > best) return Double.MAX_VALUE
    var updateStart = tps.first() - 1

    for (updateTime in updateTimes) {
        if (debug) println("------- update: $updateTime")
        val timeStartIndex = tps.indexOfFirst { it > updateStart }
        val timeStart = tps[timeStartIndex]
        val timeEndIndex =  ((tps.indexOfFirstOrNull { it > updateTime } ?: tps.size)).coerceAtMost(tps.size - 1)
        val timeEnd = tps[timeEndIndex]
        val duration = timeEnd - timeStart
        if (debug) println("start: $timeStart")
        if (debug) println("duration: $duration")
        val times = tps.subList(timeStartIndex, timeEndIndex + 1)
        if (debug) println("times: $times")
        if (times.isEmpty()) {
            updateStart = updateTime
            continue
        }

        var f = false
        for (period in (duration).downTo(2)) {
            if (duration % period != 0) continue
            var found = true
            for (t in times) {
                if (debug) println("t: $t")
                if ((t - timeStart) % period != 0) {
                    found = false
                    break
                }
            }

            if (found) {
                if (debug) println("found period: $period")
                cost += (duration / period * TP_COST)
                f = true
                break
            }
        }

        if (!f) {
            cost += duration * TP_COST
            if (punishing && cost > best) return Double.MAX_VALUE
        }

        if (updateTime >= tps.last()) break
        updateStart = updateTime
    }

    return cost + TP_COST
}

inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    val r = indexOfFirst { predicate(it) }
    return if (r == -1) null else r
}

/**
 * Actualizes an optimization result to have teleports everywhere they're needed.
 * @return the result with the added teleport times
 */
fun actualize(
    template: OptimizationResult,
    debug: Boolean = false
): OptimizationResult {
    if (debug) println("template: $template")
    if (template.tps.isEmpty()) return template
    if (template.tps.size < 3) return template
    if (template.updates.isEmpty()) throw IllegalStateException("Updates empty for template $template")

    val actualizedTps = sortedSetOf<Int>()
    actualizedTps += template.tps

    val actualizedUpdates = sortedSetOf<Int>()

    val updateTimes = template.updates.toSortedSet()
    updateTimes.addAll(template.ddUpdates)
    updateTimes.add(template.tps.last())
    if (debug) println("updates times: $updateTimes")

    var updateStart = template.tps.first() - 1

    for (updateTime in updateTimes) {
        if (debug) println("------- update: $updateTime")
        val timeStartIndex = template.tps.indexOfFirst { it > updateStart }
        val timeStart = template.tps[timeStartIndex]
        val timeEndIndex =  ((template.tps.indexOfFirstOrNull { it > updateTime } ?: template.tps.size)).coerceAtMost(template.tps.size - 1)
        val timeEnd = template.tps[timeEndIndex]
        val duration = timeEnd - timeStart
        if (debug) println("  | start: $timeStart")
        if (debug) println("  | duration: $duration")
        val times = template.tps.subList(timeStartIndex, timeEndIndex + 1)
        if (debug) println("  | times: $times")
        if (times.isEmpty()) {
            updateStart = updateTime
            continue
        }

        var f = false
        for (period in (duration).downTo(2)) {
            if (duration % period != 0) continue
            var found = true
            for (t in times) {
                if (debug) println("  | t: $t")
                if ((t - timeStart) % period != 0) {
                    found = false
                    break
                }
            }

            if (found) {
                if (debug) println("  | found period: $period")
                for (k in timeStart..timeEnd step period) {
                    actualizedTps += k
                }

                if (timeEndIndex < template.tps.size - 1) {
                    actualizedUpdates += (timeEnd - period)
                }
//                cost += (duration / period * TP_COST)
                f = true
                break
            }
        }

        if (!f) {
            if (debug) println("  | failed to find period")
//            cost += duration * TP_COST
//            if (cost > best) return Double.MAX_VALUE

            if (timeEndIndex < template.tps.size - 1) {
                actualizedUpdates += (timeEnd - 1)
            }
        }

        if (updateTime >= template.tps.last()) break
        updateStart = updateTime
    }

    return OptimizationResult(
        updates = actualizedUpdates.toList(),
        ddUpdates = template.ddUpdates,
        tps = actualizedTps.toList(),
        cost = template.cost
    )
}
