package com.ixume.particleemitter.particle.position

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.position.direction.DirectionSubcomponent
import com.ixume.particleemitter.particle.position.direction.ExpressionDirectionSubcomponent
import com.ixume.particleemitter.particle.position.direction.RandomDirectionSubcomponent
import org.joml.Vector3d
import javax.script.CompiledScript

class MotionPositionComponent(private val initialVelocityComponent: DirectionSubcomponent,
                              private val accelerationScript: Triple<CompiledScript, CompiledScript, CompiledScript>,
                              private val dragScript: CompiledScript,
                              private val myEmitterData: EmitterData,
                              private val myParticleData: ParticleData) : PositionComponent {
    companion object : ComponentParser<PositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "motion_position" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): PositionComponent? {
            val (engine, emitterData, particleData) = particleEngine()
            val jsonObject = jsonElement.asJsonObject

            var velocityComponent: DirectionSubcomponent? = null

            if ("initial_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("initial_velocity") ?: return null
                velocityComponent = ExpressionDirectionSubcomponent(
                    engine.compile(velocityObject.expression("x") ?: return null, macros),
                    engine.compile(velocityObject.expression("y") ?: return null, macros),
                    engine.compile(velocityObject.expression("z") ?: return null, macros),
                    emitterData,
                    particleData)
            } else if ("random_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("random_velocity") ?: return null
                velocityComponent = RandomDirectionSubcomponent(
                    velocityObject.expression("magnitude")?.let { engine.compile(it, macros) },
                    emitterData,
                    particleData)
            }

            val accelerationObject = jsonObject.getAsJsonObject("acceleration") ?: return null
            val accelerationScript: Triple<CompiledScript, CompiledScript, CompiledScript> = Triple(
                engine.compile(accelerationObject.expression("x") ?: return null, macros),
                engine.compile(accelerationObject.expression("y") ?: return null, macros),
                engine.compile(accelerationObject.expression("z") ?: return null, macros))
            return MotionPositionComponent(
                velocityComponent ?: return null,
                accelerationScript,
                engine.compile(jsonObject.expression("drag") ?: return null, macros),
                emitterData,
                particleData)
        }
    }

    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        if (otherParticleData.age == 0.0) {
            otherParticleData.velocity = initialVelocityComponent.dir()
        }

        val acceleration = Vector3d(accelerationScript.first.eval() as Double, accelerationScript.second.eval() as Double, accelerationScript.third.eval() as Double)
        otherParticleData.velocity.add(acceleration).mul(1.0 - (dragScript.eval() as Double))
        return Vector3d(myParticleData.relativePosition).add(otherParticleData.velocity)
    }
}