package gg.aquatic.comet.v2.runtime.virtual

import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.ixume.optimization.*
import gg.aquatic.comet.api.ParticleIDProvider
import gg.aquatic.comet.api.particle.LightData
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.data.EntityData
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*

/**
 * Holds only the information necessary about a particle for it to be displayed
 * Time is time since effect conception
 */
class ParticleStory(
    val startTime: Int,
    val lightData: LightData?,
    val billboard: BillboardConstraints,
    val seeThrough: Boolean,
    val shadow: Boolean,
    val sensitiveCentering: Boolean,
) {
    val id = ParticleIDProvider.id()
    val uuid: UUID = UUID.randomUUID()

    private val positions = mutableListOf<TimestampedPosition>()
    private val transformable = mutableListOf<TimestampedTransformable>()
    private val content = mutableListOf<TimestampedContent>()

    lateinit var optimizedPositions: Array<TimestampedPosition>
    lateinit var optimizedTransformable: Array<TimestampedTransformable>
    lateinit var optimizedContent: Array<TimestampedContent>

    fun update(time: Int, p: V2ParticleData) {
        positions += TimestampedPosition(
            time = time,
            x = p.origin.x + p.relPos.x,
            y = p.origin.y + p.relPos.y,
            z = p.origin.z + p.relPos.z,
        )

        transformable += TimestampedTransformable(
            time = time,
            scaleX = p.scale.x,
            scaleY = p.scale.y,
            scaleZ = p.scale.z,
            rotX = p.rotation.x,
            rotY = p.rotation.y,
            rotZ = p.rotation.z,
            rotW = p.rotation.w,
            opacity = p.color ushr 24,
        )

        content += TimestampedContent(
            time = time,
            data = p.displayData,
            color = p.color,
        )
    }

    fun optimize(
        optimizer: LocalPacketOptimizer,
        settings: OptimizationSettings,
    ) {
//        println("  optimizing")
        val size = positions.size
        check(transformable.size == size)
        check(content.size == size)

        val mPositions = ArrayList<TimestampedPos>(size)
        val mTransformable = ArrayList<TimestampedDisplayData>(size)
        val mContent = ArrayList<TimestampedContentData>(size)

        run {
            var i = 0
            while (i < size) {
                mPositions += positions[i].mengshe()
                mTransformable += transformable[i].mengshe()
                mContent += content[i].mengshe()

                i++
            }
        }

        val p = optimizer.optimizeSegmented(
            positionTolerance = settings.positionTolerance,
            scaleTolerance = settings.scaleTolerance,
            rotTolerance = settings.rotTolerance,
            colorTolerance = settings.colorTolerance,
            opacityTolerance = settings.opacityTolerance,
            posData = mPositions,
            displayData = mTransformable,
            textData = mContent,
            costs = Costs.DEFAULT,
            debugInfo = false,
            interval = settings.interval,
        )

//        println("    $size -> ${p.positions.size} positions updates")
//        println("    $size -> ${p.displayData.size} metadata updates")
//        println("    $size -> ${p.textData.size} content updates")
        optimizedPositions = Array(p.positions.size) { positions[p.positions[it]] }
        optimizedTransformable = Array(p.displayData.size) { transformable[p.displayData[it]] }
        optimizedContent = Array(p.textData.size) { content[p.textData[it]] }
    }

    private lateinit var previousData: EntityData
    private var positionsCursor = 0
    private var transformableCursor = 0
    private var contentCursor = 0

    fun initializeEntity(): List<PacketWrapper<*>> {
        check(positions.isNotEmpty())
        positionsCursor = 1
        transformableCursor = 1
        contentCursor = 1

        val result = mutableListOf<PacketWrapper<*>>()

        val pos = optimizedPositions.first()
        val transform = optimizedTransformable.first()
        val content = optimizedContent.first()

        val nextPos = optimizedPositions.getOrNull(1)
        val nextTransform = optimizedTransformable.getOrNull(1)

        var teleportationDuration = 1
        if (nextPos != null) teleportationDuration = nextPos.time - pos.time + 1
        var transformDuration = 1
        if (nextTransform != null) transformDuration = nextTransform.time - transform.time


        result += WrapperPlayServerSpawnEntity(
            id,
            Optional.of(uuid),
            content.data.entityType,
            Vector3d(pos.x, pos.y, pos.z),
            0f, 0f, 0f,
            0,
            Optional.of(Vector3d())
        )

        val data = EntityData(
            content.data,
            content.color,
            transform.opacity,
            null,
            VECTOR3F_0,
            Quaternionf(
                transform.rotX,
                transform.rotY,
                transform.rotZ,
                transform.rotW,
            ),
            Vector3f(
                transform.scaleX,
                transform.scaleY,
                transform.scaleZ,
            ),
            billboard,
            0,
            transformDuration,
            teleportationDuration,
            lightData,
            seeThrough,
            shadow,
            sensitiveCentering,
        )

        previousData = data

        val metadata = EntityDataBuilder.getDataFor(
            data, UpdateFlags.allTrue(), initial = true, usePUA = false
        )

        if (metadata != null) {
            result += WrapperPlayServerEntityMetadata(id, metadata)
        }

        return result
    }


    fun updateEntity(time: Int): EntityUpdateResult {
        if (positionsCursor >= optimizedPositions.size) {
            return EntityUpdateResult.Dead
        }
        if (transformableCursor >= optimizedTransformable.size) {
            return EntityUpdateResult.Dead
        }
        if (contentCursor > optimizedContent.size) {
            return EntityUpdateResult.Dead
        }

        val result = mutableListOf<PacketWrapper<*>>()

        var tpd = previousData.teleportationDuration
        val currPos = optimizedPositions[positionsCursor]
        if (currPos.time == time) {
            positionsCursor++

            if (positionsCursor + 1 >= optimizedPositions.size) {
                // LAST
                return EntityUpdateResult.Dead
            } else {
                val next = optimizedPositions[positionsCursor]
                tpd = next.time - currPos.time
                result += WrapperPlayServerEntityTeleport(
                    id,
                    Vector3d(next.x, next.y, next.z),
                    0f, 0f, false
                )
            }
        }

        var color = previousData.color
        var displayData = previousData.displayData
        var updateContent = false
        if (contentCursor < optimizedContent.size) { // if larger, then we don't need to update anything anyway. content doesn't require lookahead
            val currContent = optimizedContent[contentCursor]
            if (currContent.time == time) {
                contentCursor++
                updateContent = true
                color = currContent.color
                displayData = currContent.data
            }
        }

        val currTransform = optimizedTransformable[transformableCursor]

        if (currTransform.time == time) {
            transformableCursor++

            if (transformableCursor + 1 >= optimizedTransformable.size) {
                // LAST
                return EntityUpdateResult.Dead
            } else {
                val next = optimizedTransformable[transformableCursor]
                val data = EntityData(
                    displayData,
                    color,
                    next.opacity,
                    null,
                    VECTOR3F_0,
                    Quaternionf(
                        next.rotX,
                        next.rotY,
                        next.rotZ,
                        next.rotW,
                    ),
                    Vector3f(
                        next.scaleX,
                        next.scaleY,
                        next.scaleZ,
                    ),
                    billboard,
                    tpd,
                    next.time - currTransform.time + 1,
                    tpd,
                    lightData,
                    seeThrough,
                    shadow,
                    sensitiveCentering,
                )

                val metadata = EntityDataBuilder.getDataFor(
                    data,
                    flags = UpdateFlags(
                        display = updateContent,
                        transparency = previousData.transparency != data.transparency,
                        translation = false,
                        rotation = previousData.rotation != data.rotation,
                        scale = previousData.scale != data.scale,
                        transformationInterpolation = previousData.transformationInterpolationDuration != data.transformationInterpolationDuration,
                        teleportationDuration = previousData.teleportationDuration != data.teleportationDuration,
                    ),
                    initial = false,
                    usePUA = false,
                )

                previousData = data.copy()

                if (metadata != null) {
                    result += WrapperPlayServerEntityMetadata(id, metadata)
                }
            }
        }

        return EntityUpdateResult.Update(result)
    }

    sealed interface EntityUpdateResult {
        class Update(val packets: List<PacketWrapper<*>>) : EntityUpdateResult
        object Dead : EntityUpdateResult
    }
}

val VECTOR3F_0 = Vector3f()
