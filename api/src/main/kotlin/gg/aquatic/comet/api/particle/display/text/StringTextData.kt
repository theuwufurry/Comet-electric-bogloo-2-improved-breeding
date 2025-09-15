package gg.aquatic.comet.api.particle.display.text

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import gg.aquatic.comet.api.particle.display.DisplayData
import net.kyori.adventure.text.format.TextColor

data class StringTextData(
    val string: String,
    override val backgroundColor: Int,
    override val lineWidth: Int,
) : TextData {
    override val entityType: com.github.retrooper.packetevents.protocol.entity.type.EntityType = EntityTypes.TEXT_DISPLAY
    override fun copy(): DisplayData {
        return StringTextData(string, backgroundColor, lineWidth)
    }

    override fun with(color: Int): net.kyori.adventure.text.Component {
        return net.kyori.adventure.text.Component.text(string)
            .color(TextColor.color(color and 0xFFFFFF))
    }
}