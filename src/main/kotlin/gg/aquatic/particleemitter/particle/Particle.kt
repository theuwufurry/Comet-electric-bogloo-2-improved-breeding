package gg.aquatic.particleemitter.particle

import com.ixume.particleemitter.particle.data.PacketEntity
import com.ixume.particleemitter.ParticleEmitter.Companion.unsafe
import com.ixume.particleemitter.ParticleIDProvider
import gg.aquatic.particleemitter.ParticleEmitter.Companion.unsafe
import gg.aquatic.particleemitter.ParticleIDProvider
import gg.aquatic.particleemitter.particle.data.PacketEntity
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.bukkit.World
import org.joml.Vector3d
import java.util.UUID

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

    fun getAddPacket(): Pair<Packet<in ClientGamePacketListener>, Packet<in ClientGamePacketListener>?> {
        val packet = ClientboundAddEntityPacket(id, uuid, origin.x + data.relativePosition.x, origin.y + data.relativePosition.y, origin.z + data.relativePosition.z, 0F, 0F, EntityType.TEXT_DISPLAY, 0, Vec3(0.0, 0.0, 0.0), 0.0)
        val data = gg.aquatic.particleemitter.particle.data.EntityDataBuilder.getDataFor(
            gg.aquatic.particleemitter.particle.data.ComponentData(
                data.sprite,
                data.color,
                data.matrix
            )
        )
        val entityDataPacket: Packet<in ClientGamePacketListener>? =
            data.nonDefaultValues?.let { ClientboundSetEntityDataPacket(id, it) }
        return Pair(packet, entityDataPacket)
    }

    fun updatePacket(): ClientboundSetEntityDataPacket? {
        return gg.aquatic.particleemitter.particle.data.EntityDataBuilder.getDataFor(
            gg.aquatic.particleemitter.particle.data.ComponentData(
                data.sprite,
                data.color,
                data.matrix
            )
        ).nonDefaultValues?.let { ClientboundSetEntityDataPacket(id, it) }
    }

    fun getMovementPacket(): ClientboundTeleportEntityPacket {
        return ClientboundTeleportEntityPacket(packetEntity)
    }
}