package gg.aquatic.comet.api.particle.display.text

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.comet.api.particle.display.DisplayData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

class ComponentTextData(
    override val content: Component,
    override val backgroundColor: Int,
    override val lineWidth: Int,
    val colored: Boolean,
) : TextData<Component> {
    override val entityType: com.github.retrooper.packetevents.protocol.entity.type.EntityType = EntityTypes.TEXT_DISPLAY

    override fun with(color: Int): Component {
        return if (colored) content.color(TextColor.color(color)) else content
    }

    override fun copy(): DisplayData<Component> {
        return ComponentTextData(
            content, backgroundColor, lineWidth, colored
        )
    }
}