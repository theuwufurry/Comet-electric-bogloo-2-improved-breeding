package gg.aquatic.comet.particle

import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import gg.aquatic.comet.api.ParticleIDProvider
import gg.aquatic.comet.api.emitter.YawPitch
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.pitch
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.yaw
import gg.aquatic.comet.api.packet.PassengerManager
import gg.aquatic.comet.api.particle.AbstractParticle
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.AbstractEntityDataBuilder
import gg.aquatic.comet.api.particle.data.EntityData
import gg.aquatic.comet.particle.data.EntityDataBuilder
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

open class Particle(override var data: ParticleData) : AbstractParticle() {
    override val id = ParticleIDProvider.id()
    private val uuid = UUID.randomUUID()

    private val invertedIDs: Pair<Int, UUID> by lazy {
        val dr = DeterministicRandom(id + uuid.hashCode())
        return@lazy dr.kotlinRandom.nextInt() to dr.uuid()
    }

    override val entityIDs: List<Int>
        get() {
            return if (data.emitter != null && data.emitter!!.unrealizedEmitter.isDoubleSided) {
                listOf(id, invertedIDs.first)
            } else {
                listOf(id)
            }
        }

    private lateinit var previousEntityData: EntityData

    fun init() {
        previousEntityData = EntityData(
            data.displayData,
            data.color,
            data.color ushr 24,
            null,
            data.translation,
            data.rotation,
            data.scale,
            data.billboardConstraints,
            data.interpolationDelay,
            data.transformationInterpolationDuration,
            data.teleportationDuration,
            data.light,
            data.seeThrough,
            data.shadow,
        )
    }

    override fun tick() {
        data.age++
    }

    override fun getAddPacket(data: ParticleData): List<PacketWrapper<*>> {
    val result = mutableListOf<PacketWrapper<*>>()

        val mount = data.emitter?.mount

        val spawnPos = mount?.let {
            com.github.retrooper.packetevents.util.Vector3d(
                mount.pos.x,
                mount.pos.y,
                mount.pos.z,
            )
        } ?: com.github.retrooper.packetevents.util.Vector3d(
            data.origin.x + data.relativePosition.x,
            data.origin.y + data.relativePosition.y,
            data.origin.z + data.relativePosition.z
        )

        val yawpitch = data.emitter?.yawpitchSupplier?.get() ?: YawPitch(0f, 0f)

        result += WrapperPlayServerSpawnEntity(
            id,
            Optional.of(uuid),
            data.displayData.entityType,
            spawnPos,
            yawpitch.pitch, yawpitch.yaw, 0f,
            0,
            Optional.of(com.github.retrooper.packetevents.util.Vector3d ())
        )

        val nd = EntityDataBuilder.getDataFor(
            EntityData(
                data.displayData,
                data.color,
                data.color ushr 24,
                null,
                data.translation,
                data.rotation,
                data.scale,
                data.billboardConstraints,
                data.interpolationDelay,
                data.transformationInterpolationDuration,
                data.teleportationDuration,
                data.light,
                data.seeThrough,
                data.shadow
            ), UpdateFlags(
                display = true,
                transparency = true,
                translation = true,
                rotation = true,
                scale = true,
                transformationInterpolation = true,
                teleportationDuration = true
            ), true
        )

        if (nd != null) {
            result += WrapperPlayServerEntityMetadata(id, nd)
        }

        if (mount != null) {
            val ls = PassengerManager.passengerMap.getOrPut(data.emitter!!.mount!!.entityID) { mutableListOf() }
            ls += id
            result += WrapperPlayServerSetPassengers(data.emitter!!.mount!!.entityID, ls.toIntArray())
        }

        if (data.emitter != null && data.emitter!!.unrealizedEmitter.isDoubleSided) {
            result += WrapperPlayServerSpawnEntity(
                invertedIDs.first,
                Optional.of(invertedIDs.second),
                data.displayData.entityType,
                spawnPos,
                yawpitch.pitch, yawpitch.yaw, 0f,
                0,
                Optional.of(com.github.retrooper.packetevents.util.Vector3d())
            )

            val inverted = EntityDataBuilder.getDataFor(
                EntityData(
                    data.displayData,
                    data.color,
                    data.color ushr 24,
                    null,
                    data.translation,
                    data.rotation.flipped(),
                    data.scale,
                    data.billboardConstraints,
                    data.interpolationDelay,
                    data.transformationInterpolationDuration,
                    data.teleportationDuration,
                    data.light,
                    data.seeThrough,
                    data.shadow,
                ), UpdateFlags(
                    display = true,
                    transparency = true,
                    translation = true,
                    rotation = true,
                    scale = true,
                    transformationInterpolation = true,
                    teleportationDuration = true
                ), true
            )

            if (inverted != null) {
                result += WrapperPlayServerEntityMetadata(invertedIDs.first, inverted)
            }

            return result
        } else {
            return result
        }
    }

    //TODO: use correct data for non full update
    override fun updatePackets(
        entityDataBuilder: AbstractEntityDataBuilder,
        shouldUpdate: Boolean,
        data: ParticleData,
        flagOverride: UpdateFlags?,
    ): List<PacketWrapper<*>> {
        return if (shouldUpdate) {
            val result = mutableListOf<PacketWrapper<*>>()
            handleFullUpdate(entityDataBuilder, data, flagOverride)?.let { result += it }

            if (data.emitter != null && data.emitter!!.unrealizedEmitter.isDoubleSided) {
                val invertedData = data.clone()
                invertedData.rotation = invertedData.rotation.flipped()
                handleFullUpdate(entityDataBuilder, invertedData, flagOverride, invertedIDs.first)?.let { result += it }
            }

            result
        } else if (data.transformationInterpolationDuration > 1 && previousEntityData.reserveTransparency != null) {
            val transformationInterpolationDuration = data.transformationInterpolationDuration - 1
            val flags = UpdateFlags(
                display = false,
                transparency = false,
                translation = false,
                rotation = false,
                scale = false,
                transformationInterpolation = true,
                teleportationDuration = false
            )

            val newData =
                EntityData(
                    data.displayData,
                    data.color,
                    previousEntityData.reserveTransparency!!, null,
                    data.translation,
                    data.rotation,
                    data.scale,
                    data.billboardConstraints,
                    data.interpolationDelay,
                    transformationInterpolationDuration,
                    data.teleportationDuration,
                    data.light,
                    data.seeThrough,
                    data.shadow,
                )

            previousEntityData = newData.copy()

            val result = mutableListOf<PacketWrapper<*>>()

            if (data.emitter != null && data.emitter!!.unrealizedEmitter.isDoubleSided) {
                val invertedData = EntityData(
                    data.displayData,
                    data.color,
                    previousEntityData.reserveTransparency!!, null,
                    data.translation,
                    data.rotation.flipped(),
                    data.scale,
                    data.billboardConstraints,
                    data.interpolationDelay,
                    transformationInterpolationDuration,
                    data.teleportationDuration,
                    data.light,
                    data.seeThrough,
                    data.shadow,
                )


                entityDataBuilder.getDataFor(
                    invertedData, flags, false
                )?.let { WrapperPlayServerEntityMetadata(invertedIDs.first, it) }?.let { result += it }
            }

            entityDataBuilder.getDataFor(
                newData, flags, false
            )?.let { WrapperPlayServerEntityMetadata(id, it) }?.let { result += it }

            result
        } else emptyList()
    }

    private fun handleFullUpdate(
        entityDataBuilder: AbstractEntityDataBuilder,
        data: ParticleData,
        flagOverride: UpdateFlags?,
        entityID: Int = id
    ): PacketWrapper<*>? {
        val (flags, newData) = flagOverride?.let {
            flagOverride to EntityData(
                data.displayData,
                data.color,
                data.color ushr 24,
                null,
                data.translation,
                data.rotation,
                data.scale,
                data.billboardConstraints,
                data.interpolationDelay,
                data.transformationInterpolationDuration,
                data.teleportationDuration,
                data.light,
                data.seeThrough,
                data.shadow,
            )
        } ?: let {
            var transparency = data.color ushr 24
            var interpolationDuration = data.transformationInterpolationDuration

            val flags = UpdateFlags()

            flags.display = ((previousEntityData.displayData != data.displayData)
                    || ((previousEntityData.color and 0xFFFFFF) != (data.color and 0xFFFFFF)))
            flags.transparency = (previousEntityData.transparency != transparency)
            flags.translation = (previousEntityData.translation != data.translation)
            flags.rotation = (previousEntityData.rotation != data.rotation)
            flags.scale = (previousEntityData.scale != data.scale)
            flags.transformationInterpolation =
                (previousEntityData.transformationInterpolationDuration != data.transformationInterpolationDuration)
            flags.teleportationDuration = (previousEntityData.teleportationDuration != data.teleportationDuration)

            var reserveTransparency: Int? = null
            if (interpolationDuration > 1) {
                if (previousEntityData.reserveTransparency == null) {
                    if (previousEntityData.transparency > 127 && transparency <= 127) {
                        flags.transparency = true
                        reserveTransparency = transparency
                        transparency = 127
                        interpolationDuration = 0
                        flags.transformationInterpolation = true
                    }
                } else {
                    interpolationDuration--
                    transparency = previousEntityData.reserveTransparency!!
                    flags.transformationInterpolation = true
                }
            }

            if (!flags.anyRelevantTrue()) return@let null

            val newData =
                EntityData(
                    data.displayData,
                    data.color,
                    transparency, reserveTransparency,
                    data.translation,
                    data.rotation,
                    data.scale,
                    data.billboardConstraints, data.interpolationDelay,
                    interpolationDuration, data.teleportationDuration,
                    data.light,
                    data.seeThrough,
                    data.shadow,
                )

            flags to newData
        } ?: return null

        if (!flags.anyRelevantTrue()) return null

        previousEntityData = newData.copy()

        return entityDataBuilder.getDataFor(
            newData, flags, false
        ).let { WrapperPlayServerEntityMetadata(entityID, it ?: emptyList()) }
    }

    private fun Quaternionf.flipped(): Quaternionf {
        if (data.emitter == null) return this
        val inverse = Quaternionf(data.emitter!!.pose.rot).invert()
        val newThis = Quaternionf(inverse)
            .mul(this)
            .rotateLocalY(Math.PI.toFloat())

        return Quaternionf(data.emitter!!.pose.rot).mul(newThis)
    }

    override fun getMovementPacket(): PacketWrapper<*> {
        val yawpitch = data.emitter?.yawpitchSupplier?.get() ?: YawPitch(0f, 0f)
        return WrapperPlayServerEntityTeleport(
            id, com.github.retrooper.packetevents.util.Vector3d(
               data.origin.x + data.relativePosition.x, 
               data.origin.y + data.relativePosition.y,
               data.origin.z + data.relativePosition.z,
            ), yawpitch.yaw, yawpitch.pitch, false
        )
    }

    override val pose: Pose
        get() {
            return Pose(
                data.emitter!!.pose.world,
                Vector3d(
                    data.origin.x + data.relativePosition.x,
                    data.origin.y + data.relativePosition.y,
                    data.origin.z + data.relativePosition.z
                ),
                Quaterniond()
            )
        }

    override val dead: AtomicBoolean = AtomicBoolean(data.dead)
        get() {
            field.set(data.dead)
            return field
        }
}