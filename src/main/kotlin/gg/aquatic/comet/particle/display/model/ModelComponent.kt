package gg.aquatic.comet.particle.display.model

import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.display.DisplayData

interface ModelComponent : DisplayComponent

data class ModelData(val item: String, val id: Int) : DisplayData {
    override fun copy(): DisplayData {
        return ModelData(item, id)
    }
}