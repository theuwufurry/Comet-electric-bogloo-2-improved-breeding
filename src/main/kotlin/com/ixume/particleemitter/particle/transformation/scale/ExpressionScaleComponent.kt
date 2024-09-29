package com.ixume.particleemitter.particle.transformation.scale

import com.google.gson.JsonElement
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.ComponentParser
import com.ixume.particleemitter.parsing.ParticleJsonParser
import com.ixume.particleemitter.parsing.expression
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3f
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class ExpressionScaleComponent (private val xScale: CompiledScript, private val yScale: CompiledScript, private val zScale: CompiledScript, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : ScaleComponent {
    companion object : ComponentParser<ScaleComponent> {
        init {
            ParticleJsonParser.scaleComponentParsers += "expression_scale" to this
        }

        override fun parse(jsonElement: JsonElement): ScaleComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData()
            val particleData = ParticleData()
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
            val jsonObject = jsonElement.asJsonObject
            return ExpressionScaleComponent(
                (engine as Compilable).compile(jsonObject.expression("x") ?: return null),
                (engine as Compilable).compile(jsonObject.expression("y") ?: return null),
                (engine as Compilable).compile(jsonObject.expression("z") ?: return null),
                emitterData,
                particleData)
        }
    }

    override fun scale(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3f {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        return Vector3f((xScale.eval() as Double).toFloat(), (yScale.eval() as Double).toFloat(), (zScale.eval() as Double).toFloat())
    }
}