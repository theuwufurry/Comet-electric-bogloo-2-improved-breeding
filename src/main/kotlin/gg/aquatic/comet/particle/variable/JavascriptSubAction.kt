package gg.aquatic.comet.particle.variable

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import javax.script.CompiledScript

class JavascriptSubAction(
    private val emitterScripts: List<CompiledScript>,
    private val particleScripts: List<CompiledScript>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData,
) : SubAction {
    override fun execute(context: ActionContext) {
        myEmitterData.copyFrom(context.otherEmitterData)
        for (script in emitterScripts) {
            //modifies my emitter data
            script.eval()
        }

        if (context.otherParticleData != null) {
            myParticleData.copyFrom(context.otherParticleData)
            for (script in particleScripts) {
                //modifies my particle data and possibly my emitter data
                script.eval()
            }

            context.otherParticleData.copyFrom(myParticleData)
        }

        context.otherEmitterData.copyFrom(myEmitterData)
    }

    companion object : ComponentParser<JavascriptSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): JavascriptSubAction? {
            if (!(jsonElement.isJsonObject && jsonElement.asJsonObject.getAsJsonArray("expressions") != null)) return null

            val emitterData = EmitterData()
            val emitterEngine = emitterEngine(emitterData)
            val (particleEngine, particleData) = particleEngine(emitterData)

            val emitterScripts: MutableList<CompiledScript> = mutableListOf()
            val particleScripts: MutableList<CompiledScript> = mutableListOf()
            val jsonArray = jsonElement.asJsonObject.getAsJsonArray("expressions")

            for (element in jsonArray) {
                val asString = element.asString
                if ("particle_variable" in asString) particleScripts += particleEngine.compile(
                    asString,
                    macros
                ) else emitterScripts += emitterEngine.compile(asString, macros)
            }

            return JavascriptSubAction(
                emitterScripts,
                particleScripts,
                emitterData,
                particleData
            )
        }
    }
}