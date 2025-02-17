package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class ExpressionEmitterLifetimeComponent(
    private val expirationScript: CompiledScript?,
    private val activationScript: CompiledScript?,
    private val myEmitterData: EmitterData
) : EmitterComponent, EmitterLifetimeComponent {
    override val priority = -1
    override fun init(otherEmitterData: EmitterData) {}

    override fun execute(otherEmitterData: EmitterData) {
        otherEmitterData.age++
        myEmitterData.copyFrom(otherEmitterData)

        otherEmitterData.dead = (expirationScript?.run {
            (eval() as Number).toDouble() <= 0.0
        } ?: false)

        otherEmitterData.isActive = (activationScript?.run {
            (eval() as Number).toDouble() > 0.0
        } ?: true)
    }

    override fun die(otherEmitterData: EmitterData) {}

    companion object : BaseComponentParser {
        override val id: String = "expression_emitter_lifetime"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionEmitterLifetimeComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return ExpressionEmitterLifetimeComponent(
                jsonObject.expression("expiration_expression")?.let { engine.compile(it, macros) },
                jsonObject.expression("activation_expression")?.let { engine.compile(it, macros) },
                emitterData
            )
        }
    }
}