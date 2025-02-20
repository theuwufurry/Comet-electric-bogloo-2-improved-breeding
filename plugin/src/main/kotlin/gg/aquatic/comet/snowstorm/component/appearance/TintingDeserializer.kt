package gg.aquatic.comet.snowstorm.component.appearance

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.emitter.environment.toRGBA
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.color.Gradient
import java.io.File
import kotlin.math.max

object TintingDeserializer : Deserializer {
    override val id = "minecraft:particle_appearance_tinting"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val colorObj = jsonObject["color"]?.asJsonObjectOrNull() ?: return
        val interpolant = colorObj["interpolant"]?.asStringOrNull() ?: return
        val gradientObj = colorObj["gradient"]?.asJsonObjectOrNull() ?: return

        val gradient: MutableList<Pair<Double, String>> = mutableListOf()

        for ((timeStr, colorElem) in gradientObj.entrySet()) {
            val time = timeStr.toDoubleOrNull() ?: continue
            val colorStr = colorElem.asStringOrNull() ?: continue
            val color = colorStr.toRGBA() ?: continue

            gradient += time to "new Color(${color.red}, ${color.green}, ${color.blue}, ${max(color.alpha, 25)})"
        }

        deserializedEffect.components += Gradient(
            deserializedEffect.parseExpr(interpolant),
            gradient
        )
    }
}