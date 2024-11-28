package gg.aquatic.particleemitter.particle

import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import gg.aquatic.waves.shadow.io.retrooper.packetevents.util.SpigotConversionUtil
import gg.aquatic.waves.shadow.io.retrooper.packetevents.util.SpigotReflectionUtil
import org.bukkit.Location
import java.util.UUID

class ParticleEntity(
    val particle: Particle,
    location: Location
) {

    var location = location
        private set

    val entityData = mutableMapOf<Int, EntityData>()
    private var entityDataUpdated = false

    val entityId: Int = SpigotReflectionUtil.generateEntityId()
    val entityUUID = UUID.randomUUID()

    var spawnPacket = WrapperPlayServerSpawnEntity(
        entityId,
        entityUUID,
        EntityTypes.TEXT_DISPLAY,
        SpigotConversionUtil.fromBukkitLocation(location),
        location.yaw,
        0,
        null
    )
        private set

    private var metadataPacket = WrapperPlayServerEntityMetadata(entityId, entityData.values.toList())
    val allMetadataPacket: WrapperPlayServerEntityMetadata
        get() {
            if (entityDataUpdated) {
                entityDataUpdated = false
                return metadataPacket
            }
            val packet = WrapperPlayServerEntityMetadata(entityId, entityData.values.toList())
            metadataPacket = packet
            return packet
        }

    fun teleport(location: Location): WrapperPlayServerEntityTeleport? {
        if (location == this.location) {
            this.location = location
            return null
        }
        this.location = location
        spawnPacket = WrapperPlayServerSpawnEntity(
            entityId,
            entityUUID,
            EntityTypes.TEXT_DISPLAY,
            SpigotConversionUtil.fromBukkitLocation(location),
            location.yaw,
            0,
            null
        )
        return WrapperPlayServerEntityTeleport(
            entityId, SpigotConversionUtil.fromBukkitLocation(location), false
        )
    }

    private fun setData(index: Int, data: EntityData): Boolean {
        val previousValue = entityData[index]
        if (previousValue != null) {
            if (previousValue.value == data.value) {
                return false
            }
        }
        entityData[index] = data
        return true
    }

    fun setData(map: Map<Int, EntityData>): WrapperPlayServerEntityMetadata? {
        val entityData = mutableListOf<EntityData>()
        map.forEach {
            if (setData(it.key, it.value)) {
                entityData += it.value
            }
        }
        if (entityData.isEmpty()) {
            return null
        }
        entityDataUpdated = true
        return WrapperPlayServerEntityMetadata(entityId, entityData)
    }

}