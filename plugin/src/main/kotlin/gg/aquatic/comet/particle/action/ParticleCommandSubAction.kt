package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.NotMyType
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import org.bukkit.Bukkit

class ParticleCommandSubAction(
    private val commandScript: Expr<String>,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : SubAction {
    override fun execute(context: ActionContext) {
        myEmitterData.copyFrom(context.otherEmitterData)
        myParticleData.copyFrom(context.otherParticleData ?: return)

        val commandString = commandScript.eval().getOrPrint(context.otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return

        Bukkit.getScheduler().runTask(AbstractParticleEmitter.INSTANCE, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandString)
        })
    }

    companion object : ComponentParser<ParticleCommandSubAction> {
        override val id: String = "command"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ParticleCommandSubAction> {
            if (!jsonElement.isJsonObject) return Result.failure(NotMyType())
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val commandScript = jsonObject.getExpr("max_particles")
                .fold({ it }, { return Result.failure(NotMyType()) })
                .constructExpr<String>(engine, macros)
                .fold({ it }, { return Result.failure(it) })

            return Result.success(ParticleCommandSubAction(commandScript, particleData, emitterData))
        }
    }
}