package gg.aquatic.particleemitter.particle.data

import gg.aquatic.particleemitter.particle.Particle
import gg.aquatic.waves.fake.block.FakeEntity
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d
import org.bukkit.World

class PacketEntity(type: EntityType, val world: World) {

    var particle: Particle? = null

    val id: Int = particle?.id ?: 0

    fun trackingPosition(): Vector3d {
        return Vector3d(particle!!.origin.x + particle!!.data.relativePosition.x, particle!!.origin.y + particle!!.data.relativePosition.y, particle!!.origin.z + particle!!.data.relativePosition.z)
    }
}