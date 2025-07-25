package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.*
import gg.aquatic.comet.api.parsing.macro.Macro
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

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): RandomSpriteComponent? {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            val weightedSprites: MutableList<Pair<CompiledScript, CompiledScript>> = mutableListOf()

            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                if (jsonObject.has("sprite") && jsonObject.has("min") && jsonObject.has("max")) {
                    val sprite = jsonObject["sprite"]!!.asStringOrNull() ?: return null
                    val min = jsonObject["min"]!!.asNumberOrNull()?.toInt() ?: return null
                    val max = jsonObject["max"]!!.asNumberOrNull()?.toInt() ?: return null
                    if (max < min) return null

                    for (i in min..max) {
                        weightedSprites += engine.compile("1") to engine.compile("\"$sprite.$i\"")
                    }

                    return RandomSpriteComponent(
                        weightedSprites,
                        particleData, emitterData
                    )
                } else {
                    for ((weight, sprite) in jsonObject.entrySet()) {
                        val spriteString = sprite.asStringOrNull() ?: continue
                        weightedSprites += (engine.compile(weight, macros, true) ?: continue) to (engine.compile(spriteString, macros, true) ?: continue)
                    }

                    return RandomSpriteComponent(
                        weightedSprites,
                        particleData, emitterData
                    )
                }
            } else {
                val jsonArr = jsonElement.asJsonArray

                for (jsonElem in jsonArr) {
                    val obj = jsonElem.asJsonObjectOrNull() ?: continue
                    val weightStr = obj["weight"]?.asStringOrNull() ?: continue
                    val spriteString = obj["sprite"]?.asStringOrNull() ?: continue
                    weightedSprites += ((engine.compile(weightStr, macros, true) ?: continue) to (engine.compile(spriteString, macros, true) ?: continue))
                }

                return RandomSpriteComponent(
                    weightedSprites,
                    particleData, emitterData
                )
            }
        }
    }
}