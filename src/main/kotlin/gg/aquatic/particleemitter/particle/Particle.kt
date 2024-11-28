package gg.aquatic.particleemitter.particle

import gg.aquatic.aquaticseries.lib.util.mapPair
import gg.aquatic.particleemitter.emitter.Emitter
import gg.aquatic.particleemitter.particle.data.ComponentData
import gg.aquatic.particleemitter.particle.data.EntityDataBuilder
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import org.bukkit.Location
import org.joml.Vector3d

class Particle(
    val emitter: Emitter,
    val location: Location,
    var origin: Vector3d,
    var data: ParticleData
) {
    fun tick() {
        data.age++
    }

    /*
    val fakeEntity = FakeEntity(
        EntityTypes.TEXT_DISPLAY,
        location,
        50,
        audience
    ) {
        val data = EntityDataBuilder.getDataFor(
            ComponentData(
                data.sprite,
                data.color,
                data.matrix
            )
        ).mapPair { it.index to it }
        this.entityData += data
    }.apply {
        register()
    }
     */

    val particleEntity = ParticleEntity(
        this,
        location
    )

    /*
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
     */

    internal fun spawnPackets(): List<PacketWrapper<*>> {
        return listOf(
            particleEntity.spawnPacket,
            particleEntity.allMetadataPacket
        )
    }

    fun updateParticle(): List<PacketWrapper<*>> {
        val data = EntityDataBuilder.getDataFor(
            ComponentData(
                data.sprite,
                data.color,
                data.matrix
            )
        ).mapPair { it.index to it }

        val packet = particleEntity.setData(data) ?: return listOf()

        /*
        fakeEntity.updateEntity {
            entityData.clear()
            entityData += data
        }
         */
        return listOf(packet)
    }

    fun updateLocation(): PacketWrapper<*>? {
        this.location.x = origin.x + data.relativePosition.x
        this.location.y = origin.y + data.relativePosition.y
        this.location.z = origin.z + data.relativePosition.z
        return particleEntity.teleport(location)
        //fakeEntity.teleport(location)
    }

    /*
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
        return WrapperPlayServerEntityTeleport(packetEntity.id, packetEntity.trackingPosition(), 0f, 0f, false)
    }

     */
}