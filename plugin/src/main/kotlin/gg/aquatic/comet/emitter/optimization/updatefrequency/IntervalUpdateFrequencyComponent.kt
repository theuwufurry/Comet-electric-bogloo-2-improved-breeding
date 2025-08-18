package gg.aquatic.comet.emitter.optimization.updatefrequency

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyResult
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleData
import kotlin.math.floor

class IntervalUpdateFrequencyComponent(private val interval: Int, offset: Int) : UpdateFrequencyComponent {
    override val interpolationDelay: Int = -1
    override val initialInterpolationDuration: Int = interval + offset

    companion object : ComponentParser<IntervalUpdateFrequencyComponent> {
        override val id: String = "interval_update_frequency"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<IntervalUpdateFrequencyComponent> {
            val jsonObject = jsonElement.asJsonObject
            if (!(jsonObject.has("interval") && jsonObject.get("interval").isJsonPrimitive)) return Result.failure(
                InvalidJsonException("Missing 'interval' field!"))
            val interval = jsonObject.get("interval").asJsonPrimitive.asNumber.toInt()
            val offset =
                if (jsonObject.has("offset") && jsonObject.get("offset").isJsonPrimitive) jsonObject.get("offset").asJsonPrimitive.asInt else 0
            return Result.success(IntervalUpdateFrequencyComponent(interval, offset))
        }

        fun default(): UpdateFrequencyComponent {
            return IntervalUpdateFrequencyComponent(1, 0)
        }
    }

    override fun shouldSendUpdate(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData
    ): UpdateFrequencyResult {
        return UpdateFrequencyResult(
            floor(otherParticleData.age).toInt() == 1 || (floor(otherParticleData.age).toInt() % interval == 0),
            initialInterpolationDuration
        )
    }
}