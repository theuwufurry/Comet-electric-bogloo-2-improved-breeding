package gg.aquatic.comet.api.particle.display.sprite

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData

interface SpriteComponent : DisplayComponent

data class SpriteData(val id: String) : DisplayData {
    override val entityType: com.github.retrooper.packetevents.protocol.entity.type.EntityType = EntityTypes.TEXT_DISPLAY
    override fun copy(): DisplayData {
        return SpriteData(id)
    }
}