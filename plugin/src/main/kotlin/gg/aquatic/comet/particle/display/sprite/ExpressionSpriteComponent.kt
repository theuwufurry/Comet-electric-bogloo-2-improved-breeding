package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import javax.script.CompiledScript

class ExpressionSpriteComponent(
    private val sprite: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, SpriteComponent {
    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "expression_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionSpriteComponent(
                engine.compile(jsonObject.expression("sprite") ?: return null, macros, true),
                particleData, emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        otherParticleData.displayData = SpriteData(sprite.eval() as String)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}