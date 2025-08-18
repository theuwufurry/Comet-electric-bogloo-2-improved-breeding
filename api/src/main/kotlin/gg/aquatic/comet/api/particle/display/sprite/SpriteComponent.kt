package gg.aquatic.comet.api.particle.display.sprite

import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes

interface SpriteComponent : DisplayComponent

data class SpriteData(val id: String) : DisplayData {
    override val entityType: EntityType = EntityTypes.TEXT_DISPLAY
    override fun copy(): DisplayData {
        return SpriteData(id)
    }
}