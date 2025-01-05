package gg.aquatic.comet.emitter.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import javax.script.CompiledScript

class InstantRateComponent(
    private val amount: CompiledScript,
    private val offset: CompiledScript?,
    private val myEmitterData: EmitterData
) :
    RateComponent {
    companion object : ComponentParser<InstantRateComponent> {
        init {
            ParticleJsonParser.rateComponentParsers += "emitter_rate_instant" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): InstantRateComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return InstantRateComponent(
                engine.compile(jsonObject.expression("amount") ?: return null, macros),
                engine.compile(jsonObject.expression("offset") ?: "0", macros),
                emitterData
            )
        }
    }

    override fun toEmit(otherEmitterData: EmitterData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        val offsetTicks = (offset?.eval() as? Number)?.toInt() ?: 0
        return if (otherEmitterData.age == 1.0 + offsetTicks) {
            (amount.eval() as Number).toInt()
        } else 0
    }
}