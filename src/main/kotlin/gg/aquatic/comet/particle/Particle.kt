package gg.aquatic.comet.particle

import gg.aquatic.comet.ParticleIDProvider
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

open class Particle(var data: ParticleData) {

    val id = ParticleIDProvider.id
    private val uuid = UUID.randomUUID()

    open fun tick() {
        data.age++
    }

    fun getAddPacket(entityDataBuilder: EntityDataBuilder): List<PacketWrapper<*>> {
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

        val data = entityDataBuilder.getDataFor(
            EntityData(
                data.displayData,
                data.color,
                data.translation, data.rotation, data.scale,
                data.billboardConstraints, data.interpolationDelay, data.interpolationDuration
            ), true
        )

        val entityDataPacket: PacketWrapper<*> = WrapperPlayServerEntityMetadata(id, data)
        return listOf(packet, entityDataPacket)
    }

    fun updatePacket(entityDataBuilder: EntityDataBuilder): WrapperPlayServerEntityMetadata {
        return entityDataBuilder.getDataFor(
            EntityData(
                data.displayData,
                data.color,
                data.translation, data.rotation, data.scale,
                data.billboardConstraints, data.interpolationDelay, data.interpolationDuration
            ), false
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
}