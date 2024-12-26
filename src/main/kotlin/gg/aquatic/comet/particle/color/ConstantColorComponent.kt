package gg.aquatic.comet.particle.color

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import java.awt.Color
import javax.script.CompiledScript

class ConstantColorComponent(
    private val colorScript: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) :
    ColorComponent {
    companion object : ComponentParser<ConstantColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "constant_color" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantColorComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ConstantColorComponent(
                engine.compile(jsonObject.expression("color")?.addDependency() ?: return null, macros),
                emitterData, particleData
            )
        }
    }

    override fun color(otherEmitterData: EmitterData, otherParticleData: ParticleData): Int {
        return if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            return (colorScript.eval() as Color).argb()
        } else {
            otherParticleData.color
        }
    }
}