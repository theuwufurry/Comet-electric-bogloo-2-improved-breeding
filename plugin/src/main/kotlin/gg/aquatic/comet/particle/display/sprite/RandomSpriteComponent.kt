package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import javax.script.CompiledScript

class RandomSpriteComponent(
    private val weightedSprites: List<Pair<CompiledScript, CompiledScript>>,
    private val myParticleData: ParticleData, private val myEmitterData: EmitterData
) : ParticleComponent, SpriteComponent {
    override val priority = 0
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)

            var totalWeight = 0.0
            val evaluatedWeightedSprites: MutableList<Pair<Double, String>> = mutableListOf()
            for ((weightScript, spriteScript) in weightedSprites) {
                val weight = (weightScript.eval() as Number).toDouble()
                val sprite = spriteScript.eval() as String
                evaluatedWeightedSprites += (weight to sprite)
                totalWeight += weight
            }

            val normalizedWeightedSprites = evaluatedWeightedSprites.map { (k, v) -> (k / totalWeight to v) }
            val random = otherEmitterData.emitter!!.random.kotlinRandom.nextDouble()

            var runningWeight = 0.0
            for ((weight, sprite) in normalizedWeightedSprites) {
                if (random <= weight + runningWeight) {
                    otherParticleData.displayData = SpriteData(sprite)
                    return
                }

                runningWeight += weight
            }

            otherParticleData.displayData = SpriteData(normalizedWeightedSprites.last().second)
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
    }

    companion object : BaseComponentParser {
        override val id: String = "random_sprite"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): RandomSpriteComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val weightedSprites: MutableList<Pair<CompiledScript, CompiledScript>> = mutableListOf()
            for ((weight, sprite) in jsonObject.entrySet()) {
                val spriteString = sprite.asStringOrNull() ?: continue
                weightedSprites += (engine.compile(weight, macros, true) to engine.compile(spriteString, macros, true))
            }

            return RandomSpriteComponent(
                weightedSprites,
                particleData, emitterData
            )
        }
    }
}