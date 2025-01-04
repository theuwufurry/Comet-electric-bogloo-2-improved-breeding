package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.ComponentResult
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript
import kotlin.math.pow

class SpherePositionComponent(
    private val radiusScript: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : PositionComponent {
    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): ComponentResult<Vector3d> {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return if (otherParticleData.age == 0.0) {
            val radius = (radiusScript.eval() as Number).toDouble()
            val radiusSquared = radius * radius
            var sphereOffset = randomVector(radius)
            while (sphereOffset.lengthSquared() > radiusSquared) {
                sphereOffset = randomVector(radius)
            }

            ComponentResult(Vector3d(otherParticleData.relativePosition).add(sphereOffset))
        } else ComponentResult(otherParticleData.relativePosition)
    }

    private fun randomVector(radius: Double): Vector3d {
        return Vector3d(Math.random() * 2.0 * radius - radius, Math.random() * 2.0 * radius - radius, Math.random() * 2.0 * radius - radius)
    }

    companion object : ComponentParser<SpherePositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "sphere_position" to this
        }

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