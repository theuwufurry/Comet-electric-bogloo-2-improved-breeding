package gg.aquatic.comet.particle.variable

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterComponent
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.emitterEngine
import gg.aquatic.comet.parsing.macro.Macro
import javax.script.CompiledScript

class EmitterVariableInitializationComponent(
    private val scripts: List<CompiledScript>,
    private val myEmitterData: EmitterData
) : EmitterComponent {
    override fun init(otherEmitterData: EmitterData) {
        myEmitterData.copyFrom(otherEmitterData)
        for (script in scripts) {
            script.eval()
            otherEmitterData.copyFrom(myEmitterData)
        }
    }

    override fun execute(otherEmitterData: EmitterData) {
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "emitter_variable_init" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            val jsonArray = jsonElement.asJsonArray
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            val scripts: MutableList<CompiledScript> = mutableListOf()
            for (element in jsonArray) {
                scripts += engine.compile(element.asJsonPrimitive.asString, macros)
            }

            return EmitterVariableInitializationComponent(scripts, emitterData)
        }
    }
}