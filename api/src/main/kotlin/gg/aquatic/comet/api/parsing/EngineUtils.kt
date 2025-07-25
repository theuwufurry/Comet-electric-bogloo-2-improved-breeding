package gg.aquatic.comet.api.parsing

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.js.MathExtras
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleData
import org.openjdk.nashorn.api.scripting.NashornScriptEngine
import java.util.*
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext
import javax.script.ScriptException

fun emitterEngine(emitterData: EmitterData): Compilable {
    val engine = AbstractParticleEmitter.scriptEngineFactory.getScriptEngine("-scripting")
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter_variable" to emitterData.externalVariable)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("MathExtras" to MathExtras)
    return engine as Compilable
}

fun particleEngine(emitterData: EmitterData): Pair<Compilable, ParticleData> {
    val engine = AbstractParticleEmitter.scriptEngineFactory.getScriptEngine("-scripting")
    val particleData = ParticleData(UUID.randomUUID())
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter_variable" to emitterData.externalVariable)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle_variable" to particleData.externalVariable)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("MathExtras" to MathExtras)
    return Pair(engine as Compilable, particleData)
}


fun Compilable.compile(
    input: String,
    macros: Map<String, Macro>?,
    tryAsSimpleString: Boolean = false
): CompiledScript? {
    var output = input
    if (macros != null) {
        for ((from, macro) in macros) {
            output = output.replace(from, macro.to)
            if (macro.binding != null) {
                (this as NashornScriptEngine).getBindings(ScriptContext.ENGINE_SCOPE) += macro.binding
            }
        }
    }

    output = output.replace("Math.random()", "emitter.emitter.random.kotlinRandom.nextDouble()")

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
            try {
                val escapedCompiled = compile("\"" + output + "\"")
                escapedCompiled.eval()
                return escapedCompiled
            } catch (ignored: Exception) {
                return compiled!!
            }
        }
    }

    return try {
        compile(output)
    } catch (sc: ScriptException) {
        AbstractParticleEmitter.INSTANCE.logger.severe("Issue while compiling Javascript one of your scripts!")
        AbstractParticleEmitter.INSTANCE.logger.severe(sc.message)

        null
    }
}

fun Compilable.compileOrNull(script: String): CompiledScript? {
    return try {
        compile(script)
    } catch (ignored: Exception) {
        null
    }
}