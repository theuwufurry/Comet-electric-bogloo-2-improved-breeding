package com.ixume.particleemitter.particle.position

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.ComponentResult
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class ExpressionPositionComponent(
    private val xOffset: CompiledScript,
    private val yOffset: CompiledScript,
    private val zOffset: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : PositionComponent {
    companion object : ComponentParser<ExpressionPositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "expression_position" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionPositionComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionPositionComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros),
                engine.compile(jsonObject.expression("y") ?: return null, macros),
                engine.compile(jsonObject.expression("z") ?: return null, macros),
                emitterData, particleData
            )
        }
    }

    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): ComponentResult<Vector3d> {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val newPos = Vector3d(xOffset.eval() as Double, yOffset.eval() as Double, zOffset.eval() as Double)
        return ComponentResult(newPos.rotate(myEmitterData.rotation), true)
    }
}