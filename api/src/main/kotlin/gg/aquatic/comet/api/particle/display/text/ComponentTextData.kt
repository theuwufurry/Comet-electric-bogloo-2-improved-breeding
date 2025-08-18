package gg.aquatic.comet.api.particle.display.text

import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

class ComponentTextData(
    val component: Component,
    override val backgroundColor: Int,
    override val lineWidth: Int,
    val colored: Boolean,
) : TextData {
    override val entityType: EntityType = EntityTypes.TEXT_DISPLAY

    override fun with(color: Int): Component {
        return if (colored) component.color(TextColor.color(color)) else component
    }

    override fun copy(): DisplayData {
        return ComponentTextData(
            component, backgroundColor, lineWidth, colored
        )
    }
}