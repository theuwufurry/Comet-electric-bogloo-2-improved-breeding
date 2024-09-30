package com.ixume.particleemitter.particle.transformation.scale

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3f
import javax.script.CompiledScript

class ExpressionScaleComponent (private val xScale: CompiledScript, private val yScale: CompiledScript, private val zScale: CompiledScript, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : ScaleComponent {
    companion object : ComponentParser<ScaleComponent> {
        init {
            ParticleJsonParser.scaleComponentParsers += "expression_scale" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, String>?): ScaleComponent? {
            val (engine, emitterData, particleData) = particleEngine()
            val jsonObject = jsonElement.asJsonObject
            return ExpressionScaleComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros),
                engine.compile(jsonObject.expression("y") ?: return null, macros),
                engine.compile(jsonObject.expression("z") ?: return null, macros),
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