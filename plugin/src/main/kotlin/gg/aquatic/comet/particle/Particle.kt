package gg.aquatic.comet.particle

import gg.aquatic.comet.api.ParticleIDProvider
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.particle.AbstractParticle
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.AbstractEntityDataBuilder
import gg.aquatic.comet.api.particle.data.EntityData
import gg.aquatic.comet.api.particle.display.TextDisplayComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.world.Location
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import java.util.*

open class Particle(override var data: ParticleData) : AbstractParticle() {
    override val id = ParticleIDProvider.id()
    private val uuid = UUID.randomUUID()

    private lateinit var previousEntityData: EntityData

    fun init() {
        previousEntityData = EntityData(
            data.displayData,
            data.color, data.color ushr 24, null,
            data.translation, data.rotation, data.scale,
            data.billboardConstraints, data.interpolationDelay, data.transformationInterpolationDuration, data.teleportationDuration
        )
    }

    override fun tick() {
        data.age++
    }

    override fun getAddPacket(): List<PacketWrapper<*>> {
        val packet = WrapperPlayServerSpawnEntity(
            id,
            Optional.of(uuid),
            if (data.displayData is SpriteData || data.displayData is TextDisplayComponent) EntityTypes.TEXT_DISPLAY else EntityTypes.ITEM_DISPLAY,
            Vector3d(
                data.origin.x + data.relativePosition.x,
                data.origin.y + data.relativePosition.y,
                data.origin.z + data.relativePosition.z
            ),
            0f, 0f, 0f,
            0,
            Optional.of(Vector3d())
        )

        val data = EntityDataBuilder.getDataFor(
            EntityData(
                data.displayData,
                data.color, data.color ushr 24, null,
                data.translation, data.rotation, data.scale,
                data.billboardConstraints, data.interpolationDelay, data.transformationInterpolationDuration, data.teleportationDuration
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

        val entityDataPacket: PacketWrapper<*> = WrapperPlayServerEntityMetadata(id, data)
        return listOf(packet, entityDataPacket)
    }

    override fun updatePacket(
        entityDataBuilder: AbstractEntityDataBuilder,
        shouldUpdate: Boolean
    ): WrapperPlayServerEntityMetadata? {
        return if (shouldUpdate) handleFullUpdate(entityDataBuilder)
        else if (data.transformationInterpolationDuration > 1 && previousEntityData.reserveTransparency != null) {
            val transformationInterpolationDuration = data.transformationInterpolationDuration - 1
            val flags = UpdateFlags(false, false, false, false, false, true, false)

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
                    data.teleportationDuration
                )

            previousEntityData = newData.copy()

            entityDataBuilder.getDataFor(
                newData, flags, false
            ).let { WrapperPlayServerEntityMetadata(id, it) }
        } else null
    }

    private fun handleFullUpdate(entityDataBuilder: AbstractEntityDataBuilder): WrapperPlayServerEntityMetadata? {
        val flags = UpdateFlags()
        var transparency = data.color ushr 24
        var interpolationDuration = data.transformationInterpolationDuration
        flags.display = (previousEntityData.displayData != data.displayData)
                || ((previousEntityData.color and 0xFFFFFF) != (data.color and 0xFFFFFF))
        flags.transparency = previousEntityData.transparency != transparency
        flags.translation = previousEntityData.translation != data.translation
        flags.rotation = previousEntityData.rotation != data.rotation
        flags.scale = previousEntityData.scale != data.scale
        flags.transformationInterpolation = previousEntityData.transformationInterpolationDuration != data.transformationInterpolationDuration
        flags.teleportationDuration = previousEntityData.teleportationDuration != data.teleportationDuration

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

        if (!flags.anyTrue()) return null

        val newData =
            EntityData(
                data.displayData,
                data.color,
                transparency, reserveTransparency,
                data.translation,
                data.rotation,
                data.scale,
                data.billboardConstraints, data.interpolationDelay,
                interpolationDuration, data.teleportationDuration
            )

        previousEntityData = newData.copy()

        return entityDataBuilder.getDataFor(
            newData, flags, false
        ).let { WrapperPlayServerEntityMetadata(id, it) }
    }

    override fun getMovementPacket(): WrapperPlayServerEntityTeleport {
        return WrapperPlayServerEntityTeleport(
            id, Location(
                Vector3d(
                    data.origin.x + data.relativePosition.x,
                    data.origin.y + data.relativePosition.y,
                    data.origin.z + data.relativePosition.z
                ), 0f, 0f
            ), true
        )
    }

    override val pose: Pose
        get() {
            return Pose(
                org.joml.Vector3d(
                    data.origin.x + data.relativePosition.x,
                    data.origin.y + data.relativePosition.y,
                    data.origin.z + data.relativePosition.z
                ),
                org.joml.Vector3d()
            )
        }

    override val dead: Boolean
        get() = data.dead
}

