package gg.aquatic.comet.emitter.optimization

import gg.aquatic.comet.emitter.optimization.Mengshe.Costs
import gg.aquatic.comet.emitter.optimization.Mengshe.LocalPacketOptimizer
import gg.aquatic.comet.emitter.optimization.Mengshe.TimestampedContentData
import gg.aquatic.comet.emitter.optimization.Mengshe.TimestampedDisplayData
import gg.aquatic.comet.emitter.optimization.Mengshe.math.Quaternion
import java.awt.Color
import java.util.*
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
    private val opacityTol: Double,
    private val considerColorTex: Boolean,
) {
    private val optimizer = LocalPacketOptimizer()
    
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

    fun optimizeFinished(interval: Int): CachedPath {
        for (finishedParticle in finishedParticles) {
            val t = measureNanoTime {
                mengsheOptimizeFinishedParticle(
                    optimizer = optimizer,
                    path = this,
                    finishedParticle = finishedParticle,
                    positionTolerance = locTol,
                    scaleTolerance = dispTol,
                    colorTolerance = colTol,
                    rotTolerance = rotTol,
                    opacityTolerance = opacityTol,
                    interval = interval,
                )
            }

            val d = t.toDuration(DurationUnit.NANOSECONDS)
//            
//            println("Optimization took $d!")
//            println("| Path had ${internalLocations[finishedParticle]?.size} nodes!")
        }

        finishedParticles.clear()

        return this
    }
}

private fun mengsheOptimizeFinishedParticle(
    optimizer: LocalPacketOptimizer,
    path: CachedPath,
    finishedParticle: UUID,
    positionTolerance: Double,
    scaleTolerance: Double,
    colorTolerance: Double,
    rotTolerance: Double,
    opacityTolerance: Double,
    interval: Int,
) {
    val ip = path.internalLocations[finishedParticle]!!
    val it = path.internalTransformableData[finishedParticle]!!
    val ic = path.coloredTextureData[finishedParticle]!!

    val mengshePositions = ip.map { it.mengshe() }
    val mengsheDisplay = it.map { it.mengshe() }
    val mengsheText = ic.map { it.mengshe() }

    val p = optimizer.optimizeSegmented(
        positionTolerance = positionTolerance,
        scaleTolerance = scaleTolerance,
        rotTolerance = rotTolerance,
        colorTolerance = colorTolerance,
        opacityTolerance = opacityTolerance,
        posData = mengshePositions,
        displayData = mengsheDisplay,
        textData = mengsheText,
        costs = Costs.DEFAULT,
        debugInfo = false,
        interval = interval,
    )

//    println("positions: ${p.positions}")
//    println("display: ${p.displayData}")
//    println("text: ${p.textData}")

    val optimizedPositions = p.positions.map { ip[it] }.toMutableList()
    val optimizedDisplay = p.displayData.map { idx -> it[idx] }.toMutableList()
    val optimizedText = p.textData.map { ic[it] }.toMutableList()

    path.locations[finishedParticle] = optimizedPositions
    path.transformableData[finishedParticle] = optimizedDisplay
    path.coloredTextureData[finishedParticle] = optimizedText
}

fun TimestampedPos.mengshe(): gg.aquatic.comet.emitter.optimization.Mengshe.TimestampedPos {
    return gg.aquatic.comet.emitter.optimization.Mengshe.TimestampedPos(
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

        opacity = (vec.alpha * 256.0).toInt().coerceIn(25..255)
    )
}

fun TimestampedColoredTexture.mengshe(): TimestampedContentData {
    return TimestampedContentData(
        t = time,
        content = this.displayData.content,
        color = Color(color),
    )
}