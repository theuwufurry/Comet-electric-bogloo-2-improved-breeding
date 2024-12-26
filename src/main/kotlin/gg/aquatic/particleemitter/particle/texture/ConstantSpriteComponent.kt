package gg.aquatic.particleemitter.particle.texture

import com.google.gson.JsonElement
import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.parsing.ComponentParser
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import gg.aquatic.particleemitter.parsing.expression
import gg.aquatic.particleemitter.particle.ParticleData
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class ConstantSpriteComponent(private val sprite: CompiledScript, private val myEmitterData: EmitterData) : SpriteComponent {
    companion object : ComponentParser<SpriteComponent> {
        init {
            ParticleJsonParser.spriteComponentParsers += "constant_sprite" to this
        }

        override fun parse(jsonElement: JsonElement): ConstantSpriteComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData(0.0)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            val jsonObject = jsonElement.asJsonObject
            return ConstantSpriteComponent(
                jsonObject.expression("sprite")?.let {
                    (engine as Compilable).compile(it) } ?: return null,
                emitterData
            )
        }
    }

    override fun sprite(otherEmitterData: EmitterData, otherParticleData: ParticleData): String {
        return if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            sprite.eval() as String
        } else {
            otherParticleData.sprite
        }
    }
}