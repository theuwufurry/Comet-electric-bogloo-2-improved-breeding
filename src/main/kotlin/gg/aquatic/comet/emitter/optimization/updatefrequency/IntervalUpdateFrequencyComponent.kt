package gg.aquatic.comet.emitter.optimization.updatefrequency

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.ComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import kotlin.math.floor

class IntervalUpdateFrequencyComponent(private val interval: Int) : UpdateFrequencyComponent {
    override val interpolationDelay: Int = -1
    override val interpolationDuration: Int = interval

    companion object : ComponentParser<IntervalUpdateFrequencyComponent> {
        init {
            ParticleJsonParser.updateFrequencyParsers += "interval_update_frequency" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): IntervalUpdateFrequencyComponent? {
            val jsonObject = jsonElement.asJsonObject
            if (!(jsonObject.has("interval") && jsonObject.get("interval").isJsonPrimitive)) return null
            val interval = jsonObject.get("interval").asJsonPrimitive.asNumber.toInt()
            return IntervalUpdateFrequencyComponent(interval)
        }
    }

    override fun shouldSendUpdate(otherEmitterData: EmitterData, otherParticleData: ParticleData): Boolean {
        return floor(otherParticleData.age).toInt() == 1 || (floor(otherParticleData.age).toInt() % interval == 0)
    }
}