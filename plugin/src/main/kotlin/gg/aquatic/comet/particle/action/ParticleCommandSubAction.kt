package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.ComponentParser
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.parsing.particleEngine
import gg.aquatic.comet.particle.ParticleData
import org.bukkit.Bukkit
import javax.script.CompiledScript

class ParticleCommandSubAction(
    private val commandScript: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : SubAction {
    override fun execute(context: ActionContext) {
        myEmitterData.copyFrom(context.otherEmitterData)
        myParticleData.copyFrom(context.otherParticleData ?: return)

        val commandString = commandScript.eval() as? String ?: return

        Bukkit.getScheduler().runTask(ParticleEmitter.INSTANCE, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandString)
        })
    }

    companion object : ComponentParser<ParticleCommandSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ParticleCommandSubAction? {
            if (!jsonElement.isJsonObject) return null
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val commandScript = engine.compile(jsonObject.expression("command") ?: return null, macros)

            return ParticleCommandSubAction(commandScript, particleData, emitterData)
        }
    }
}