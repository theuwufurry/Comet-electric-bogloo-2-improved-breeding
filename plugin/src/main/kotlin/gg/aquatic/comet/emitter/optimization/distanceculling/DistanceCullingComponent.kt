package gg.aquatic.comet.emitter.optimization.distanceculling

import com.google.gson.JsonElement
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.ParticleJsonParser

class DistanceCullingComponent(val viewDistance: Float) {
    companion object : ComponentParser<DistanceCullingComponent> {
        init {
            ParticleJsonParser.distanceCullingParser = "distance_culling" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): DistanceCullingComponent? {
            val jsonObject = jsonElement.asJsonObject
            if (!(jsonObject.has("distance") && jsonObject.get("distance").isJsonPrimitive)) return null
            val distance = jsonObject.get("distance").asJsonPrimitive.asNumber
            return DistanceCullingComponent(distance.toFloat() * distance.toFloat())
        }

        fun default(): DistanceCullingComponent {
            return DistanceCullingComponent(64f * 64f)
        }
    }
}