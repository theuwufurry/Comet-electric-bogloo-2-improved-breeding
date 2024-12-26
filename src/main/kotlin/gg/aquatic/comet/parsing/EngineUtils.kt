package gg.aquatic.comet.parsing

import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import org.openjdk.nashorn.api.scripting.NashornScriptEngine
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

fun emitterEngine(emitterData: EmitterData): Compilable {
    val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
    return engine as Compilable
}

fun particleEngine(emitterData: EmitterData): Pair<Compilable, ParticleData> {
    val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
    val particleData = ParticleData()
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
    return Pair(engine as Compilable, particleData)
}


fun Compilable.compile(input: String, macros: Map<String, Macro>?): CompiledScript {
    var output = input
    if (macros != null) {
        for ((from, macro) in macros) {
            output = output.replace(from, macro.to)
            if (macro.binding != null) {
                (this as NashornScriptEngine).getBindings(ScriptContext.ENGINE_SCOPE) += macro.binding
            }
        }
    }

    return compile(output)
}