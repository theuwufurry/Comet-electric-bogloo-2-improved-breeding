package gg.aquatic.comet.v2.runtime.particle

import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import gg.aquatic.comet.api.ParticleIDProvider
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.EntityData
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.comet.v2.parsing.api.V2ParticleData
import java.util.*

class V2Particle(val data: V2ParticleData) {
    val id = ParticleIDProvider.id()
    private val uuid = UUID.randomUUID()

    private lateinit var previousEntityData: EntityData

    fun init() {
        previousEntityData = EntityData(
            data.displayData,
            data.color,
            data.color ushr 24,
            null,
            data.translation,
            data.rotation,
            data.scale,
            data.billboardConstraints,
            data.interpolationDelay,
            data.transformationInterpolationDuration,
            data.teleportationDuration,
            data.light,
            data.seeThrough,
            data.shadow,
            data.sensitiveCentering,
        )
    }


    fun getAddPacket(): List<PacketWrapper<*>> {
        val result = mutableListOf<PacketWrapper<*>>()

        val spawnPos = Vector3d(
            data.origin.x + data.relativePosition.x,
            data.origin.y + data.relativePosition.y,
            data.origin.z + data.relativePosition.z
        )


        result += WrapperPlayServerSpawnEntity(
            id,
            Optional.of(uuid),
            data.displayData.entityType,
            spawnPos,
            0f, 0f, 0f,
            0,
            Optional.of(Vector3d())
        )

        val nd = EntityDataBuilder.getDataFor(
            EntityData(
                data.displayData,
                data.color,
                data.color ushr 24,
                null,
                data.translation,
                data.rotation,
                data.scale,
                data.billboardConstraints,
                data.interpolationDelay,
                data.transformationInterpolationDuration,
                data.teleportationDuration,
                data.light,
                data.seeThrough,
                data.shadow,
                data.sensitiveCentering,
            ), UpdateFlags(
                display = true,
                transparency = true,
                translation = true,
                rotation = true,
                scale = true,
                transformationInterpolation = true,
                teleportationDuration = true
            ), initial = true, usePUA = false
        )

        if (nd != null) {
            result += WrapperPlayServerEntityMetadata(id, nd)
        }

        return result
    }

    fun update(): List<PacketWrapper<*>> {
        val transparency = data.color ushr 24
        val interpolationDuration = data.transformationInterpolationDuration

        val flags = UpdateFlags()

        flags.display = ((previousEntityData.displayData != data.displayData)
                         || ((previousEntityData.color and 0xFFFFFF) != (data.color and 0xFFFFFF)))
        flags.transparency = (previousEntityData.transparency != transparency)
        flags.translation = (previousEntityData.translation != data.translation)
        flags.rotation = (previousEntityData.rotation != data.rotation)
        flags.scale = (previousEntityData.scale != data.scale)
        flags.transformationInterpolation =
            (previousEntityData.transformationInterpolationDuration != data.transformationInterpolationDuration)
        flags.teleportationDuration = (previousEntityData.teleportationDuration != data.teleportationDuration)

        if (!flags.anyRelevantTrue()) {
            return emptyList()
        }

        val newData =
            EntityData(
                data.displayData,
                data.color,
                transparency, null,
                data.translation,
                data.rotation,
                data.scale,
                data.billboardConstraints, data.interpolationDelay,
                interpolationDuration, data.teleportationDuration,
                data.light,
                data.seeThrough,
                data.shadow,
                data.sensitiveCentering,
            )

        previousEntityData = newData.copy()

        return listOf(
            EntityDataBuilder.getDataFor(
                newData, flags, initial = false, usePUA = false
            ).let { WrapperPlayServerEntityMetadata(id, it ?: emptyList()) })
    }

    fun getPositionPacket(): PacketWrapper<*> {
        return WrapperPlayServerEntityTeleport(
            id, Vector3d(
                data.origin.x + data.relativePosition.x,
                data.origin.y + data.relativePosition.y,
                data.origin.z + data.relativePosition.z,
            ), 0f, 0f, false
        )
    }
}