package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class ExpressionSpriteComponent(
    private val sprite: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, SpriteComponent {
    companion object : BaseComponentParser {
        override val id: String = "expression_sprite"

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