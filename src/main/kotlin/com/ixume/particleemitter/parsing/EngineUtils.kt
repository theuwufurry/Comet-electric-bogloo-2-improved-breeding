package com.ixume.particleemitter.parsing

import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
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


fun Compilable.compile(input: String, macros: Map<String, String>?): CompiledScript {
    var output = input
    if (macros != null) {
        for ((from, to ) in macros) {
            output = output.replace(from, to)
        }
    }

    return compile(output)
}