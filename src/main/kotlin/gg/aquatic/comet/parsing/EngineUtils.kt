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
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter_variable" to emitterData.variable)
    return engine as Compilable
}

fun particleEngine(emitterData: EmitterData): Pair<Compilable, ParticleData> {
    val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
    val particleData = ParticleData()
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter_variable" to emitterData.variable)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle_variable" to particleData.variable)
    return Pair(engine as Compilable, particleData)
}


fun Compilable.compile(input: String, macros: Map<String, Macro>?, tryAsSimpleString: Boolean = false): CompiledScript {
    var output = input
    if (macros != null) {
        for ((from, macro) in macros) {
            output = output.replace(from, macro.to)
            if (macro.binding != null) {
                (this as NashornScriptEngine).getBindings(ScriptContext.ENGINE_SCOPE) += macro.binding
            }
        }
    }

    val compiled = compileOrNull(output)
    if (tryAsSimpleString) {
        try {
            if (compiled == null) {
                val escapedCompiled = compile("\"" + output + "\"")
                escapedCompiled?.eval()
                return escapedCompiled

            } else {
                compiled.eval()
            }
        } catch (ignored: Exception) {
            val escapedCompiled = compile("\"" + output + "\"")
            try {
                escapedCompiled.eval()
                return escapedCompiled
            } catch (ignored: Exception) {
                return compiled!!
            }
        }
    }

    return compiled!!
}

fun Compilable.compileOrNull(script: String): CompiledScript? {
    return try {
        compile(script)
    } catch (ignored: Exception) {
        null
    }
}