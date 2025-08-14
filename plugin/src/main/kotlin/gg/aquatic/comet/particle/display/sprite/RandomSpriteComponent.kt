package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.*
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class RandomSpriteComponent(
    private val weightedSprites: List<Pair<Expr<Number>, Expr<String>>>,
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
                val weight = weightScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: return
                val sprite = spriteScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
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

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<RandomSpriteComponent> {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            val weightedSprites: MutableList<Pair<Expr<Number>, Expr<String>>> = mutableListOf()

            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                if (jsonObject.has("sprite") && jsonObject.has("min") && jsonObject.has("max")) {
                    val sprite = jsonObject.getExpr("sprite").fold({ it }, { return Result.failure(it) })
                    val min = jsonObject["min"]
                        ?.asNumberOrNull()?.toInt()
                        ?: return Result.failure(InvalidJsonException("Malformed 'min' field!"))
                    val max = jsonObject["max"]
                        ?.asNumberOrNull()?.toInt()
                        ?: return Result.failure(InvalidJsonException("Malformed 'min' field!"))
                    if (max < min) return Result.failure(InvalidJsonException("Max is greater than min!"))

                    for (i in min..max) {
                        weightedSprites += ("1".constructExpr<Number>(engine, macros).getOrThrow()) to "\"$sprite.$i\""
                            .constructExpr<String>(engine, macros, tryAsSimpleString = true)
                            .fold({ it }, { return Result.failure(it) })
                    }

                    return Result.success(
                        RandomSpriteComponent(
                            weightedSprites,
                            particleData, emitterData
                        )
                    )
                } else {
                    for ((weight, sprite) in jsonObject.entrySet()) {
                        val spriteString = sprite.asStringOrNull() ?: continue
                        weightedSprites += (
                                weight.constructExpr<Number>(engine, macros)
                                    .fold({ it }, { return Result.failure(it) })
                                ) to (
                                spriteString.constructExpr<String>(engine, macros, tryAsSimpleString = true)
                                    .fold({ it }, { return Result.failure(it) })
                                )
                    }

                    return Result.success(
                        RandomSpriteComponent(
                            weightedSprites,
                            particleData, emitterData
                        )
                    )
                }
            } else {
                val jsonArr = jsonElement.asJsonArray

                for (jsonElem in jsonArr) {
                    val obj = jsonElem.asJsonObjectOrNull() ?: continue
                    val weightStr = obj["weight"]?.asStringOrNull() ?: continue
                    val spriteString = obj["sprite"]?.asStringOrNull() ?: continue
                    weightedSprites += (
                            weightStr.constructExpr<Number>(engine, macros)
                                .fold({ it }, { return Result.failure(it) })
                            ) to (
                            spriteString.constructExpr<String>(engine, macros, tryAsSimpleString = true)
                                .fold({ it }, { return Result.failure(it) })
                            )
                }

                return Result.success(RandomSpriteComponent(
                    weightedSprites,
                    particleData, emitterData
                ))
            }
        }
    }
}