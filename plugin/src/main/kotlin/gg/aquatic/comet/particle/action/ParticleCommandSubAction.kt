package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
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

        Bukkit.getScheduler().runTask(AbstractParticleEmitter.INSTANCE, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandString)
        })
    }

    companion object : ComponentParser<ParticleCommandSubAction> {
        override val id: String = "command"

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