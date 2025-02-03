package gg.aquatic.comet.api.particle

import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.particle.data.AbstractEntityDataBuilder
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport


abstract class AbstractParticle() : Parent {
    abstract var data: ParticleData
    abstract val id: Int
    abstract fun tick()
    abstract fun getAddPacket(): List<PacketWrapper<*>>

    abstract fun updatePacket(entityDataBuilder: AbstractEntityDataBuilder, shouldUpdate: Boolean): WrapperPlayServerEntityMetadata?

    abstract fun getMovementPacket(): WrapperPlayServerEntityTeleport
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