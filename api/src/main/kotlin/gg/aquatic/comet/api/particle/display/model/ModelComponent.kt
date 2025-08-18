package gg.aquatic.comet.api.particle.display.model

import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData
import org.bukkit.entity.EntityType

interface ModelComponent : DisplayComponent

data class ModelData(val id: String) : DisplayData {
    override val entityType: EntityType = EntityType.ITEM_DISPLAY
    override fun copy(): DisplayData {
        return ModelData(id)
    }
}