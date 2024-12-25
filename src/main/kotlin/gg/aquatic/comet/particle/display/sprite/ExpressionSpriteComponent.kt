package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import javax.script.CompiledScript

class ExpressionSpriteComponent(
    private val sprite: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : SpriteComponent {
    companion object : ComponentParser<ExpressionSpriteComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "expression_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionSpriteComponent(
                engine.compile(jsonObject.expression("sprite") ?: return null, macros),
                particleData, emitterData
            )
        }
    }

    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): SpriteData {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return SpriteData(sprite.eval() as String)
    }
}