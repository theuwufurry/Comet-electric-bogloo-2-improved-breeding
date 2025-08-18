package gg.aquatic.comet.api.particle.display.sprite

import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData
import org.bukkit.entity.EntityType

interface SpriteComponent : DisplayComponent

data class SpriteData(val id: String) : DisplayData {
    override val entityType: EntityType = EntityType.TEXT_DISPLAY
    override fun copy(): DisplayData {
        return SpriteData(id)
    }
}