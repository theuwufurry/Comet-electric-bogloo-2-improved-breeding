package gg.aquatic.comet.particle

import gg.aquatic.comet.ParticleIDProvider
import gg.aquatic.comet.emitter.parent.Parent
import gg.aquatic.comet.particle.data.EntityData
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.comet.particle.display.TextDisplayComponent
import gg.aquatic.comet.particle.display.sprite.SpriteData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.world.Location
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import java.util.*

open class Particle(var data: ParticleData) : Parent {
    val id = ParticleIDProvider.id()
    private val uuid = UUID.randomUUID()

    private lateinit var previousEntityData: EntityData

    fun init() {
        previousEntityData = EntityData(
            data.displayData,
            data.color, data.color ushr 24, null,
            data.translation, data.rotation, data.scale,
            data.billboardConstraints, data.interpolationDelay, data.interpolationDuration
        )
    }

    fun tick() {
        data.age++
    }

    fun getAddPacket(): List<PacketWrapper<*>> {
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
                data.billboardConstraints, data.interpolationDelay, data.interpolationDuration
            ), UpdateFlags(true, true, true, true, true, true), true
        )

        val entityDataPacket: PacketWrapper<*> = WrapperPlayServerEntityMetadata(id, data)
        return listOf(packet, entityDataPacket)
    }

    fun updatePacket(entityDataBuilder: EntityDataBuilder, shouldUpdate: Boolean): WrapperPlayServerEntityMetadata? {
        return if (shouldUpdate) handleFullUpdate(entityDataBuilder)
        else if (data.interpolationDuration > 1 && previousEntityData.reserveTransparency != null) {
            val interpolationDuration = data.interpolationDuration - 1
            val flags = UpdateFlags(false, false, false, false, false, true)

            val newData =
                EntityData(
                    data.displayData,
                    data.color,
                    previousEntityData.reserveTransparency!!, null,
                    data.translation,
                    data.rotation,
                    data.scale,
                    data.billboardConstraints, data.interpolationDelay, interpolationDuration
                )

            previousEntityData = newData.copy()

            entityDataBuilder.getDataFor(
                newData, flags, false
            ).let { WrapperPlayServerEntityMetadata(id, it) }
        } else null
    }

    private fun handleFullUpdate(entityDataBuilder: EntityDataBuilder): WrapperPlayServerEntityMetadata? {
        val flags = UpdateFlags()
        var transparency = data.color ushr 24
        var interpolationDuration = data.interpolationDuration
        flags.display = (previousEntityData.displayData != data.displayData)
                || ((previousEntityData.color and 0xFFFFFF) != (data.color and 0xFFFFFF))
        flags.transparency = previousEntityData.transparency != transparency
        flags.translation = previousEntityData.translation != data.translation
        flags.rotation = previousEntityData.rotation != data.rotation
        flags.scale = previousEntityData.scale != data.scale
        flags.interpolation = previousEntityData.interpolationDuration != data.interpolationDuration

        var reserveTransparency: Int? = null
        if (interpolationDuration > 1) {
            if (previousEntityData.reserveTransparency == null) {
                if (previousEntityData.transparency > 127 && transparency <= 127) {
                    flags.transparency = true
                    reserveTransparency = transparency
                    transparency = 127
                    interpolationDuration = 0
                    flags.interpolation = true
                }
            } else {
                interpolationDuration--
                transparency = previousEntityData.reserveTransparency!!
                flags.interpolation = true
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
                data.billboardConstraints, data.interpolationDelay, interpolationDuration
            )

        previousEntityData = newData.copy()

        return entityDataBuilder.getDataFor(
            newData, flags, false
        ).let { WrapperPlayServerEntityMetadata(id, it) }
    }

    fun getMovementPacket(): WrapperPlayServerEntityTeleport {
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

    override fun location(): org.joml.Vector3d {
        return org.joml.Vector3d(
            data.origin.x + data.relativePosition.x,
            data.origin.y + data.relativePosition.y,
            data.origin.z + data.relativePosition.z
        )
    }
}

class UpdateFlags(
    var display: Boolean = false,
    var transparency: Boolean = false,
    var translation: Boolean = false,
    var rotation: Boolean = false,
    var scale: Boolean = false,
    var interpolation: Boolean = false
) {
    fun anyTrue() = display || transparency || translation || rotation || scale || interpolation
}