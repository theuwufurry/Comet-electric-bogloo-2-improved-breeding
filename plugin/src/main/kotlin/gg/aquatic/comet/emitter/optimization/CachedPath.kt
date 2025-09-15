package gg.aquatic.comet.emitter.optimization

import com.ixume.optimization.TimestampedDisplayData
import com.ixume.optimization.TimestampedTextData
import com.ixume.optimization.math.Quaternion
import com.ixume.optimization.optimize
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.emitter.optimization.vec.DisplayDataVector
import java.awt.Color
import java.util.*
import kotlin.math.pow
import kotlin.system.measureNanoTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/*
colors ARE important, use radial check

optimize in between w/ douglas for everything else
 */

/**
 * @param considerColorTex Whether to consider color and textures in optimizations. False is faster.
 */
data class CachedPath(
    private val locTol: Double,
    private val dispTol: Double,
    private val rotTol: Double,
    private val colTol: Double,
    private val considerColorTex: Boolean,
) {
    val emitterData: MutableList<TimestampedEmitterData> = mutableListOf()
    val emitterActions: MutableList<TimestampedEmitterActions> = mutableListOf()
    val particleActions: MutableMap<UUID, MutableList<TimestampedParticleActions>> = mutableMapOf()

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

    val finishedParticles = mutableListOf<UUID>()

    fun optimizeFinished(): CachedPath {
        for (finishedParticle in finishedParticles) {
            val t = measureNanoTime {
                mengsheOptimizeFinishedParticle(
                    path = this,
                    finishedParticle = finishedParticle,
                    positionTolerance = locTol,
                    scaleTolerance = dispTol,
                    colorTolerance = colTol,
                    rotTolerance = rotTol,
                )
            }

            val d = t.toDuration(DurationUnit.NANOSECONDS)
            println("Optimization took $d !")
        }

        finishedParticles.clear()

        return this
    }

    companion object {
        // 0 - off, 1 - size, 2 - list
        val DEBUG_LOCS = 0.0
        val DEBUG_DISPLAY_DATA = 0.0
        val DEBUG_COL_TEX = 0.0
    }
}

private fun simplifyColors(
    toSimplify: MutableList<TimestampedColoredTexture>,
    tolerance: Double,
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
    tolerance: Double,
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
    tolerance: Double,
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

private fun mengsheOptimizeFinishedParticle(
    path: CachedPath,
    finishedParticle: UUID,
    positionTolerance: Double,
    scaleTolerance: Double,
    colorTolerance: Double,
    rotTolerance: Double,
) {
    val ip = path.internalLocations[finishedParticle]!!
    val it = path.internalTransformableData[finishedParticle]!!
    val ic = path.coloredTextureData[finishedParticle]!!

    val mengshePositions = ip.map { it.mengshe() }
    val mengsheDisplay = it.map { it.mengshe() }
    val mengsheText = ic.map { it.mengshe() }

    val p = optimize(
        positionTolerance = positionTolerance,
        scaleTolerance = scaleTolerance,
        rotTolerance = rotTolerance,
        colorTolerance = colorTolerance,
        posData = mengshePositions,
        displayData = mengsheDisplay,
        textData = mengsheText,
    )

    println("positions: ${p.positions}")
    println("display: ${p.displayData}")
    println("text: ${p.textData}")

    val optimizedPositions = p.positions.map { ip[it] }.toMutableList()
    val optimizedDisplay = p.displayData.map { idx -> it[idx] }.toMutableList()
    val optimizedText = p.textData.map { ic[it] }.toMutableList()

    path.locations[finishedParticle] = optimizedPositions
    path.transformableData[finishedParticle] = optimizedDisplay
    path.coloredTextureData[finishedParticle] = optimizedText
}

fun TimestampedPos.mengshe(): com.ixume.optimization.TimestampedPos {
    return com.ixume.optimization.TimestampedPos(
        t = vec.time.toInt(),
        x = vec.vec.x,
        y = vec.vec.y,
        z = vec.vec.z,
    )
}

fun TimestampedTransformableData.mengshe(): TimestampedDisplayData {
    return TimestampedDisplayData(
        t = vec.time.toInt(),
        scaleX = vec.scale.x.toDouble(),
        scaleY = vec.scale.y.toDouble(),
        scaleZ = vec.scale.z.toDouble(),

        rot = Quaternion(vec.rot.x.toDouble(), vec.rot.y.toDouble(), vec.rot.z.toDouble(), vec.rot.w.toDouble()),
    )
}

fun TimestampedColoredTexture.mengshe(): TimestampedTextData {
    return TimestampedTextData(
        t = time,
        content = (this.displayData as SpriteData).id,
        color = Color(color),
    )
}