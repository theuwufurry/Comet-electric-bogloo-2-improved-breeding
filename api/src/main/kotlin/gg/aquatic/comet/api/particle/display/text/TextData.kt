package gg.aquatic.comet.api.particle.display.text

import gg.aquatic.comet.api.particle.display.DisplayData

interface TextData : DisplayData {
    val backgroundColor: Int
    val lineWidth: Int
    fun with(color: Int): net.kyori.adventure.text.Component
}
