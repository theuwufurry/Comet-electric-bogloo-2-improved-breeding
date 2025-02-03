package gg.aquatic.comet.api.particle.display.sprite

import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.api.particle.display.DisplayData

interface SpriteComponent : DisplayComponent

data class SpriteData(val id: String) : DisplayData {
    override fun copy(): DisplayData {
        return SpriteData(id)
    }
}