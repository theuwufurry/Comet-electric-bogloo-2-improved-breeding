package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class TimedEmitterLifetimeComponent(
    private val lifetimeScript: CompiledScript?,
    private val maxLifeScript: CompiledScript?,
    private val myEmitterData: EmitterData
) : EmitterComponent, EmitterLifetimeComponent {
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {
        myEmitterData.copyFrom(otherEmitterData)
//        otherEmitterData.dead = !((lifetimeScript.eval() as Number).toDouble() <= 0.0)
        otherEmitterData.dead = !(lifetimeScript?.run {
            (eval() as Number).toDouble() <= 0.0
        } ?: maxLifeScript?.run {
            otherEmitterData.age <= (eval() as Number).toDouble()
        } ?: false)
    }

    override fun die(otherEmitterData: EmitterData) {}

    companion object : BaseComponentParser {
        override val id: String = "timed_emitter_lifetime"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): TimedEmitterLifetimeComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return TimedEmitterLifetimeComponent(
                jsonObject.expression("expiration_expression")?.let { engine.compile(it, macros) },
                jsonObject.expression("max_lifetime")?.let { engine.compile(it, macros) },
                emitterData
            )
        }
    }
}