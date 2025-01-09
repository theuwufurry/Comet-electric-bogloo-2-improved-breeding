package gg.aquatic.comet.particle.variable

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import javax.script.CompiledScript

class ParticleVariableInitializationComponent(
    private val scripts: List<CompiledScript>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData,
) : ParticleComponent {
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        if (otherParticleData.age == 0.0) {
            for (script in scripts) {
                script.eval()
                otherEmitterData.copyFrom(myEmitterData)
                otherParticleData.copyFrom(myParticleData)
            }
        }
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "particle_variable_init" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            val jsonArray = jsonElement.asJsonArray
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val scripts: MutableList<CompiledScript> = mutableListOf()
            for (element in jsonArray) {
                scripts += engine.compile(element.asJsonPrimitive.asString, macros)
            }

            return ParticleVariableInitializationComponent(scripts, emitterData, particleData)
        }
    }
}