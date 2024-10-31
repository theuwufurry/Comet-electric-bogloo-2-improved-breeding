package com.ixume.particleemitter.particle

import com.ixume.particleemitter.ParticleEmitter.Companion.unsafe
import com.ixume.particleemitter.ParticleIDProvider
import com.ixume.particleemitter.particle.data.EntityData
import com.ixume.particleemitter.particle.data.EntityDataBuilder
import com.ixume.particleemitter.particle.data.PacketEntity
import com.ixume.particleemitter.particle.display.TextDisplayComponent
import com.ixume.particleemitter.particle.display.sprite.SpriteData
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import java.util.*

open class Particle(var data: ParticleData) {

    val id = ParticleIDProvider.id
    private val uuid = UUID.randomUUID()
    private val packetEntity: PacketEntity = unsafe.allocateInstance(PacketEntity::class.java) as PacketEntity

    init {
        packetEntity.particle = this
    }

    open fun tick() {
        data.age++
    }

    fun getAddPacket(): Pair<Packet<in ClientGamePacketListener>, Packet<in ClientGamePacketListener>?> {
        val packet = ClientboundAddEntityPacket(
            id,
            uuid,
            data.origin.x + data.relativePosition.x,
            data.origin.y + data.relativePosition.y,
            data.origin.z + data.relativePosition.z,
            0F,
            0F,
            if (data.displayData is SpriteData || data.displayData is TextDisplayComponent) EntityType.TEXT_DISPLAY else EntityType.ITEM_DISPLAY,
            0,
            Vec3(0.0, 0.0, 0.0),
            0.0
        )
        val data = EntityDataBuilder.getDataFor(
            EntityData(
                data.displayData,
                data.color,
                data.matrix,
                data.billboardConstraints
            )
        )
        val entityDataPacket: Packet<in ClientGamePacketListener>? =
            data!!.nonDefaultValues?.let { ClientboundSetEntityDataPacket(id, it) }
        return Pair(packet, entityDataPacket)
    }

    fun updatePacket(): ClientboundSetEntityDataPacket? {
        return EntityDataBuilder.getDataFor(
            EntityData(
                data.displayData,
                data.color,
                data.matrix,
                data.billboardConstraints
            )
        )!!.nonDefaultValues?.let { ClientboundSetEntityDataPacket(id, it) }
    }

    fun getMovementPacket(): ClientboundTeleportEntityPacket {
        return ClientboundTeleportEntityPacket(packetEntity)
    }
}