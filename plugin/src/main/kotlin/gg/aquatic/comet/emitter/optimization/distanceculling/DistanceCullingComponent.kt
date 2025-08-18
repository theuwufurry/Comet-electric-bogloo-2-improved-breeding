package gg.aquatic.comet.emitter.optimization.distanceculling

import com.google.gson.JsonElement
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.macro.Macro

class DistanceCullingComponent(val viewDistance: Float) {
    companion object : ComponentParser<DistanceCullingComponent> {
        override val id: String = "distance_culling"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<DistanceCullingComponent> {
            val jsonObject = jsonElement.asJsonObject
            if (!(jsonObject.has("distance") && jsonObject.get("distance").isJsonPrimitive)) return Result.failure(
                InvalidJsonException("'distance' field is nonexistent or malformed!"))
            val distance = jsonObject.get("distance").asJsonPrimitive.asNumber
            return Result.success(DistanceCullingComponent(distance.toFloat() * distance.toFloat()))
        }

        fun default(): DistanceCullingComponent {
            return DistanceCullingComponent(64f * 64f)
        }
    }
}