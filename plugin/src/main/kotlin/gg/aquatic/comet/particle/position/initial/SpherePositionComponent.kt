package gg.aquatic.comet.particle.position.initial

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.particle.position.PositionComponent
import org.joml.Vector3d
import javax.script.CompiledScript

class SpherePositionComponent(
    private val radiusScript: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent {
    override val priority = 0
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        otherParticleData.relativePosition = if (otherParticleData.age == 0.0) {
            val radius = (radiusScript.eval() as Number).toDouble()
            val radiusSquared = radius * radius
            var sphereOffset = randomVector(radius)
            while (sphereOffset.lengthSquared() > radiusSquared) {
                sphereOffset = randomVector(radius)
            }

            Vector3d(otherParticleData.relativePosition).add(sphereOffset)
        } else otherParticleData.relativePosition
    }

    private fun randomVector(radius: Double): Vector3d {
        return Vector3d(
            Math.random() * 2.0 * radius - radius,
            Math.random() * 2.0 * radius - radius,
            Math.random() * 2.0 * radius - radius
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "sphere_position"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SpherePositionComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return SpherePositionComponent(
                engine.compile(jsonObject.expression("radius") ?: "1", macros),
                emitterData, particleData
            )
        }
    }
}