package gg.aquatic.comet.particle.color

import com.google.gson.JsonElement
import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import java.awt.Color
import javax.script.CompiledScript

class ConstantColorComponent(
    private val colorScript: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, ColorComponent {
    override val priority = 0

    companion object : BaseComponentParser {

        override val id: String = "constant_color"

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

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        otherParticleData.color = if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            (colorScript.eval() as? Color)?.rgb ?: run {
                AbstractParticleEmitter.INSTANCE.logger.warning("Issue evaluating 'color' expressions in 'constant_color' component in ${otherEmitterData.emitter?.unrealizedEmitter?.id}")
                otherParticleData.color
            }
        } else {
            otherParticleData.color
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}