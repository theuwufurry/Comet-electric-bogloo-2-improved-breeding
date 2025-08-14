package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class FlipbookSpriteComponent : ParticleComponent, SpriteComponent {
    private val inputScript: Expr<Number>
    private val spriteScripts: List<Pair<Double, Expr<String>>>
    private val myParticleData: ParticleData
    private val myEmitterData: EmitterData

    constructor(
        inputScript: Expr<Number>,
        spriteScripts: List<Pair<Double, Expr<String>>>,
        myParticleData: ParticleData,
        myEmitterData: EmitterData
    ) {
        this.inputScript = inputScript
        this.spriteScripts = spriteScripts
        this.myParticleData = myParticleData
        this.myEmitterData = myEmitterData
    }

    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "flipbook_sprite"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<FlipbookSpriteComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val sprites: MutableList<Pair<Double, Expr<String>>> = mutableListOf()
            if (jsonObject["sprites"].isJsonArray) {
                for (element in jsonObject.getAsJsonArray("sprites")) {
                    sprites += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (
                            element.getExpr("sprite")
                                .fold({ it }, { return Result.failure(it) })
                                .constructExpr<String>(engine, macros, tryAsSimpleString = true)
                                .fold({ it }, { return Result.failure(it) })
                            )
                }
            } else if (jsonObject["sprites"].isJsonObject) {
                for ((index, sprite) in jsonObject["sprites"].asJsonObject.entrySet()) {
                    sprites += index.toDouble() to (
                            sprite.asStringOrNull()
                                ?.constructExpr<String>(engine, macros, tryAsSimpleString = true)
                                ?.fold({ it }, { return Result.failure(it) })
                                ?: return Result.failure(InvalidJsonException("Missing model!")))
                }
            } else {
                return Result.failure(InvalidJsonException("Malformed component!"))
            }

            return Result.success(
                FlipbookSpriteComponent(
                    jsonObject.getExpr("input")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    sprites,
                    particleData, emitterData
                )
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val inputResult = inputScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: return

        if (inputResult >= spriteScripts.last().first) {
            otherParticleData.displayData = SpriteData(spriteScripts.last().second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return)
            return
        }

        if (inputResult <= spriteScripts.first().first) {
            otherParticleData.displayData = SpriteData(spriteScripts.first().second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return)
            return
        }

        var i = 0
        while (i + 1 < spriteScripts.size && inputResult >= spriteScripts[i + 1].first) i++
        val chosenSprite = spriteScripts[i].second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
        otherParticleData.displayData = SpriteData(chosenSprite)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}