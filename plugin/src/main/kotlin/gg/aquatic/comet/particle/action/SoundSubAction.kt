package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.NotMyType
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.optimization.VirtualEmitter
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

//TODO: Particle Data support

class SoundSubAction(
    private val soundIDExpr: Expr<String>,
    private val volumeExpr: Expr<Number>?,
    private val pitchExpr: Expr<Number>?,
    private val myEmitterData: EmitterData,
) : SubAction {
    override fun execute(context: ActionContext) {
        if (context.otherEmitterData.emitter!!.isPregen) {
            val virtual = context.otherEmitterData.emitter!! as VirtualEmitter
            myEmitterData.copyFrom(context.otherEmitterData)
            val soundID = soundIDExpr.eval().getOrPrint(context.otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
            val volume = volumeExpr?.eval()?.getOrPrint(context.otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 1f
            val pitch = pitchExpr?.eval()?.getOrPrint(context.otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 1f

            virtual.emitterActionsBuffer += { em ->
                em.pose.location.world!!.playSound(
                    em.pose.location,
                    soundID,
                    volume, pitch
                )
            }
        } else {
            myEmitterData.copyFrom(context.otherEmitterData)
            val soundID = soundIDExpr.eval().getOrPrint(context.otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
            val volume = volumeExpr?.eval()?.getOrPrint(context.otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 1f
            val pitch = pitchExpr?.eval()?.getOrPrint(context.otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 1f

            context.otherEmitterData.location.world!!.playSound(
                context.otherEmitterData.location,
                soundID,
                volume, pitch
            )

        }
    }

    companion object : ComponentParser<SoundSubAction> {
        override val id: String = "play_sound"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<SoundSubAction> {
            if (!(jsonElement.isJsonObject && jsonElement.asJsonObject.has("sound_id"))) {
                return Result.failure(NotMyType())
            }

            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            val soundIDExpr = (jsonObject.getExpr("sound_id")
                .fold({ it }, { return Result.failure(it) }))
                .constructExpr<String>(engine, macros, true)
                .fold({ it }, { return Result.failure(it) })
            val volumeExpr = jsonObject.getExprOrNull("volume")
                ?.constructExpr<Number>(engine, macros)
                ?.fold({ it }, { return Result.failure(it) })
            val pitchExpr = jsonObject.getExprOrNull("pitch")
                ?.constructExpr<Number>(engine, macros)
                ?.fold({ it }, { return Result.failure(it) })

            return Result.success(SoundSubAction(soundIDExpr, volumeExpr, pitchExpr, emitterData))
        }
    }
}