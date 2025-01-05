package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import java.util.*
import javax.script.CompiledScript

class ManualRateComponent(private val spawningMap: Map<Int, CompiledScript>, private val myEmitterData: EmitterData) :
    RateComponent {
    companion object : ComponentParser<ManualRateComponent> {
        init {
            ParticleJsonParser.rateComponentParsers += "emitter_rate_manual" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ManualRateComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val spawningMap: MutableMap<Int, CompiledScript> = TreeMap()
            for ((timeString, _) in jsonObject.entrySet()) {
                val time = timeString.toIntOrNull() ?: continue
                val amountScript = engine.compile(jsonObject.expression(timeString) ?: continue, macros)
                spawningMap[time] = amountScript
            }

            return ManualRateComponent(
                spawningMap,
                emitterData
            )
        }
    }

    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val entry = spawningMap[otherEmitterData.age.toInt()]
        return if (entry != null) {
            (entry.eval() as Number).toInt()
        } else 0
    }
}