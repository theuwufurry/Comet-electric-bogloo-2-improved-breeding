package gg.aquatic.comet.particle.display.sprite

import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.display.DisplayData

interface SpriteComponent : DisplayComponent

data class SpriteData(val id: String) : DisplayData {
    override fun copy(): DisplayData {
        return SpriteData(id)
    }
}