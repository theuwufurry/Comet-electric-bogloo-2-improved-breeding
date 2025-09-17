package gg.aquatic.comet.api.particle.display.model

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData

interface ModelComponent : DisplayComponent

data class ModelData(override val content: String) : DisplayData<String> {
    override val entityType: com.github.retrooper.packetevents.protocol.entity.type.EntityType = EntityTypes.ITEM_DISPLAY
    override fun copy(): DisplayData<String> {
        return ModelData(content)
    }
}