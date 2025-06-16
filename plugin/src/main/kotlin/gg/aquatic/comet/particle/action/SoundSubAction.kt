package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.optimization.VirtualEmitter
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

//TODO: Particle Data support

class SoundSubAction(
    private val soundIDExpr: CompiledScript,
    private val volumeExpr: CompiledScript,
    private val pitchExpr: CompiledScript,
    private val myEmitterData: EmitterData,
) : SubAction {
    override fun execute(context: ActionContext) {
        if (context.otherEmitterData.emitter!!.isPregen) {
            val virtual = context.otherEmitterData.emitter!! as VirtualEmitter
            myEmitterData.copyFrom(context.otherEmitterData)
            val soundID = soundIDExpr.eval() as String
            val volume = (volumeExpr.eval() as Number).toFloat()
            val pitch = (pitchExpr.eval() as Number).toFloat()

            virtual.emitterActionsBuffer += { em ->
                em.pose.location.world!!.playSound(
                    em.pose.location,
                    soundID,
                    volume, pitch
                )
            }
        } else {
            myEmitterData.copyFrom(context.otherEmitterData)
            val soundID = soundIDExpr.eval() as String
            val volume = (volumeExpr.eval() as Number).toFloat()
            val pitch = (pitchExpr.eval() as Number).toFloat()

            context.otherEmitterData.location.world!!.playSound(
                context.otherEmitterData.location,
                soundID,
                volume, pitch
            )

        }
    }

    companion object : ComponentParser<SoundSubAction> {
        override val id: String = "play_sound"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SoundSubAction? {
            if (!(jsonElement.isJsonObject && jsonElement.asJsonObject.has("sound_id"))) return null
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            val soundIDExpr = engine.compile(jsonObject.expression("sound_id")!!, macros, true)
            val volumeExpr = engine.compile(jsonObject.expression("volume") ?: "1", macros)
            val pitchExpr = engine.compile(jsonObject.expression("pitch") ?: "1", macros)

            return SoundSubAction(soundIDExpr, volumeExpr, pitchExpr, emitterData)
        }
    }
}