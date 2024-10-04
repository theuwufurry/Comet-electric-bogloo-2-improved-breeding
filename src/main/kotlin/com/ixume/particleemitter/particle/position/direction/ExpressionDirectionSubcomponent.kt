package com.ixume.particleemitter.particle.position.direction

import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.compile
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.parsing.particleEngine
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class ExpressionDirectionSubcomponent(private val xOffset: CompiledScript, private val yOffset: CompiledScript, private val zOffset: CompiledScript, val myParticleData: ParticleData) : DirectionSubcomponent {
    override fun dir(): Vector3d {
        return Vector3d(xOffset.eval() as Double, yOffset.eval() as Double, zOffset.eval() as Double)
    }
}

class UnrealizedExpressionDirectionSubcomponent(private val xOffset: String, private val yOffset: String, private val zOffset: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<ExpressionDirectionSubcomponent> {
    override fun realizeComponent(emitterData: EmitterData): ExpressionDirectionSubcomponent {
        val (engine, particleData) = particleEngine(emitterData)
        return ExpressionDirectionSubcomponent(engine.compile(xOffset, macros), engine.compile(yOffset, macros), engine.compile(zOffset, macros), particleData)
    }
}