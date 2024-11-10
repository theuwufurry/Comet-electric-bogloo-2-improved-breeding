package gg.aquatic.particleemitter.particle

import gg.aquatic.particleemitter.ParticleEmitter.Companion.unsafe
import gg.aquatic.particleemitter.ParticleIDProvider
import gg.aquatic.particleemitter.particle.data.ComponentData
import gg.aquatic.particleemitter.particle.data.EntityDataBuilder
import gg.aquatic.particleemitter.particle.data.PacketEntity
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import org.joml.Vector3d
import java.util.*

class Particle(var origin: Vector3d, var data: ParticleData) {

    val id = ParticleIDProvider.id
    private val uuid = UUID.randomUUID()
    private val packetEntity: PacketEntity = unsafe.allocateInstance(PacketEntity::class.java) as PacketEntity

    init {
        packetEntity.particle = this
    }

    fun tick() {
        data.age++
    }

    fun getAddPacket(): Pair<PacketWrapper<*>, PacketWrapper<*>> {
        val packet = WrapperPlayServerSpawnEntity(
            packetEntity.id,
            Optional.of(uuid),
            EntityTypes.TEXT_DISPLAY,
            gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d(
                origin.x + data.relativePosition.x,
                origin.y + data.relativePosition.y,
                origin.z + data.relativePosition.z
            ),
            0F,
            0F,
            0F,
            0,
            Optional.empty()
        )
        return Pair(packet, updatePacket())
    }

    fun updatePacket(): PacketWrapper<*> {
        val data = EntityDataBuilder.getDataFor(
            ComponentData(
                data.sprite,
                data.color,
                data.matrix
            )
        )
        val dataPacket = WrapperPlayServerEntityMetadata(
            packetEntity.id, data
        )
        return dataPacket
    }

    fun getMovementPacket(): PacketWrapper<*> {
        return WrapperPlayServerEntityTeleport(packetEntity.id, packetEntity.trackingPosition(),0f,0f,false)
    }
}