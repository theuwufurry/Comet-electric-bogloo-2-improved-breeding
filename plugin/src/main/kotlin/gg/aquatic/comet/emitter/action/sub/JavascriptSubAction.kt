package gg.aquatic.comet.emitter.action.sub

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.NotMyType
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.script.expr.JSRunnable.Companion.constructRunnable
import javax.script.ScriptException

class JavascriptSubAction(
    private val emitterScripts: List<Runnable>,
    private val particleScripts: List<Runnable>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData,
) : SubAction {
    override fun execute(context: ActionContext) {
        myEmitterData.copyFrom(context.otherEmitterData)
        for (script in emitterScripts) {
            //modifies my emitter data
            script.run()
        }

        context.otherParticleData?.let { otherParticleData ->
            myParticleData.copyFrom(otherParticleData)
            for (script in particleScripts) {
                //modifies my particle data and possibly my emitter data
                try {
                    script.run()
                } catch (sc: ScriptException) {
                    AbstractParticleEmitter.INSTANCE.logger.severe("Javascript error while executing ${myEmitterData.emitter?.unrealizedEmitter?.id}!")
                    AbstractParticleEmitter.INSTANCE.logger.severe(sc.message)
                    myEmitterData.emitter?.kill()
                }
            }

            otherParticleData.copyFrom(myParticleData)
        }

        context.otherEmitterData.copyFrom(myEmitterData)
    }

    companion object : ComponentParser<JavascriptSubAction> {
        override val id: String = "expressions"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<JavascriptSubAction> {
            if (!(jsonElement.isJsonObject && jsonElement.asJsonObject.getAsJsonArray("expressions") != null)) {
                return Result.failure(NotMyType())
            }

            val emitterData = EmitterData()
            val emitterEngine = emitterEngine(emitterData)
            val (particleEngine, particleData) = particleEngine(emitterData)

            val emitterScripts: MutableList<Runnable> = mutableListOf()
            val particleScripts: MutableList<Runnable> = mutableListOf()
            val jsonArray = jsonElement.asJsonObject.getAsJsonArray("expressions")

            for (element in jsonArray) {
                val asString = element.asString
                if ("particle" in asString || "particle_variable" in asString) {
                    particleScripts += asString.constructRunnable(
                        engine = particleEngine,
                        macros = macros,
                    ).fold(
                        { it },
                        { return Result.failure(it) }
                    )
                } else {
                    emitterScripts += asString.constructRunnable(
                        engine = emitterEngine,
                        macros = macros,
                    ).fold(
                        { it },
                        { return Result.failure(it) }
                    )
                }
            }

            return Result.success(JavascriptSubAction(
                emitterScripts,
                particleScripts,
                emitterData,
                particleData
            ))
        }
    }
}