package com.ixume.particleemitter.parsing

import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import org.openjdk.nashorn.api.scripting.NashornScriptEngine
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

fun emitterEngine(): Pair<Compilable, EmitterData> {
    val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
    val emitterData = EmitterData(0.0)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
    return Pair(engine as Compilable, emitterData)
}

fun particleEngine(): Triple<Compilable, EmitterData, ParticleData> {
    val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
    val emitterData = EmitterData()
    val particleData = ParticleData()
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
    engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
    return Triple(engine as Compilable, emitterData, particleData)
}


fun Compilable.compile(input: String, macros: Map<String, Macro>?): CompiledScript {
    var output = input
    if (macros != null) {
        for ((from, macro ) in macros) {
            output = output.replace(from, macro.to)
            if (macro.binding != null) {
                (this as NashornScriptEngine).getBindings(ScriptContext.ENGINE_SCOPE) += macro.binding
            }
        }
    }

    return compile(output)
}