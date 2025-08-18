package gg.aquatic.comet.api.particle.display.model

import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes

interface ModelComponent : DisplayComponent

data class ModelData(val id: String) : DisplayData {
    override val entityType: EntityType = EntityTypes.ITEM_DISPLAY
    override fun copy(): DisplayData {
        return ModelData(id)
    }
}