package com.ixume.particleemitter.particle.position

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.emitter.shape.PointShapeComponent
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class ExpressionPositionComponent(private val xOffset: CompiledScript, private val yOffset: CompiledScript, private val zOffset: CompiledScript, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : PositionComponent {
    companion object : ComponentParser<ExpressionPositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "expression_relative_position" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<ExpressionPositionComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedExpressionPositionComponent(
                jsonObject.expression("x") ?: return null,
                jsonObject.expression("y") ?: return null,
                jsonObject.expression("z") ?: return null,
                macros)
        }
    }

    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        return Vector3d(xOffset.eval() as Double, yOffset.eval() as Double, zOffset.eval() as Double)
    }
}

class UnrealizedExpressionPositionComponent(private val xOffset: String, private val yOffset: String, private val zOffset: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<ExpressionPositionComponent> {
    override fun realizeComponent(emitterData: EmitterData): ExpressionPositionComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return ExpressionPositionComponent(
            engine.compile(xOffset, macros),
            engine.compile(yOffset, macros),
            engine.compile(zOffset, macros),
            emitterData, particleData)
    }
}