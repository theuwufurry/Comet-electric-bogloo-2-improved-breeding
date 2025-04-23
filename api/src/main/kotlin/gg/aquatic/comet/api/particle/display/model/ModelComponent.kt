package gg.aquatic.comet.api.particle.display.model

import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData

interface ModelComponent : DisplayComponent

data class ModelData(val id: String) : DisplayData {
    override fun copy(): DisplayData {
        return ModelData(id)
    }
}