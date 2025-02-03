package gg.aquatic.comet.emitter.optimization.updatefrequency

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyResult
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleData

class ManualUpdateFrequencyComponent(private val updateTimes: List<Int>, private val durationOffset: Int) :
    UpdateFrequencyComponent {
    override val interpolationDelay: Int = -1
    override val initialInterpolationDuration: Int =
        if (updateTimes.size > 1) updateTimes[1] - updateTimes[0] + durationOffset else 1

    companion object : ComponentParser<ManualUpdateFrequencyComponent> {
        override val id: String = "manual_update_frequency"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ManualUpdateFrequencyComponent? {
            val jsonObject = jsonElement.asJsonObject
            if (!(jsonObject.has("times") && jsonObject.get("times").isJsonArray)) return null
            val timesObject = jsonObject.getAsJsonArray("times")
            val offset =
                if (jsonObject.has("offset") && jsonObject.get("offset").isJsonPrimitive) jsonObject.get("offset").asJsonPrimitive.asInt else 0
            return ManualUpdateFrequencyComponent(timesObject.filter { it.isJsonPrimitive }.map { it.asInt }, offset)
        }
    }

    override fun shouldSendUpdate(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData
    ): UpdateFrequencyResult {
        val age = otherParticleData.age.toInt()
        val index = updateTimes.indexOf(age)
        if (index == -1) return UpdateFrequencyResult(false, null)
        return UpdateFrequencyResult(
            true,
            if (index >= updateTimes.size - 1) 1 + durationOffset else updateTimes[index + 1] - updateTimes[index] + durationOffset
        )
    }
}