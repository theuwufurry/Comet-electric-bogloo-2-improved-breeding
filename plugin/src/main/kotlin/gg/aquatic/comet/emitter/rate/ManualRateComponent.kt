package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
import java.util.*
import javax.script.CompiledScript

class ManualRateComponent(private val spawningMap: Map<Int, CompiledScript>, private val myEmitterData: EmitterData) :
    RateComponent {
    companion object : ComponentParser<ManualRateComponent> {
        private val timesInput = Regex("^\\d+\\.\\.\\d+$")

        init {
            ParticleJsonParser.rateComponentParsers += "emitter_rate_manual" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ManualRateComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val spawningMap: MutableMap<Int, CompiledScript> = TreeMap()
            for ((timeString, _) in jsonObject.entrySet()) {
                timeString.toIntOrNull()?.let l@{
                    val amountScript = engine.compile(jsonObject.expression(timeString) ?: return@l, macros)
                    spawningMap[it] = amountScript
                } ?: run l@{
                    if (!timesInput.matches(timeString)) return@l

                    val parts = timeString.split("")

                    val start = parts[0].toInt()
                    val finish = parts[1].toInt()

                    val amountScript = engine.compile(jsonObject.expression(timeString) ?: return@l, macros)
                    for (time in start..finish) {
                        spawningMap[time] = amountScript
                    }
                }
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