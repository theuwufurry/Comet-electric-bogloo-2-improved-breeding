package gg.aquatic.comet.api.particle

import com.github.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.particle.data.AbstractEntityDataBuilder

abstract class AbstractParticle : Parent {
    abstract var data: ParticleData
    abstract val id: Int
    abstract val entityIDs: List<Int>
    abstract fun tick()
    abstract fun getAddPacket(data: ParticleData = this.data): List<PacketWrapper<*>>

    abstract fun updatePackets(entityDataBuilder: AbstractEntityDataBuilder, shouldUpdate: Boolean, data: ParticleData = this.data, flagOverride: UpdateFlags?): List<PacketWrapper<*>>

    abstract fun getMovementPacket(): PacketWrapper<*>
}

data class UpdateFlags(
    var display: Boolean = false,
    var transparency: Boolean = false,
    var translation: Boolean = false,
    var rotation: Boolean = false,
    var scale: Boolean = false,
    var transformationInterpolation: Boolean = false,
    var teleportationDuration: Boolean = false,
) {
    /**
     * Does not check transformation duration, as sending them alone is unwanted in all known cases, if this changes, add handling for it
     */
    fun anyRelevantTrue() = display || transparency || translation || rotation || scale || teleportationDuration
    fun anyTrue() = display || transparency || translation || rotation || scale || teleportationDuration || transformationInterpolation

    companion object {
        fun allTrue() = UpdateFlags(
            display = true,
            transparency = true,
            translation = true,
            rotation = true,
            scale = true,
            transformationInterpolation = true,
            teleportationDuration = true,
        )

        fun delta(curr: ParticleData, prev: ParticleData): UpdateFlags {
            val flags = UpdateFlags()
            val transparency = curr.color ushr 24
            val prevTransparency = prev.color ushr 24
            flags.display = ((prev.displayData != curr.displayData)
                    || ((prev.color and 0xFFFFFF) != (curr.color and 0xFFFFFF)))
            flags.transparency = (prevTransparency != transparency)
            flags.translation = (prev.translation != curr.translation)
            flags.rotation = (prev.rotation != curr.rotation)
            flags.scale = (prev.scale != curr.scale)
            flags.transformationInterpolation =
                (prev.transformationInterpolationDuration != curr.transformationInterpolationDuration)
            flags.teleportationDuration = (prev.teleportationDuration != curr.teleportationDuration)

            return flags
        }
    }
}