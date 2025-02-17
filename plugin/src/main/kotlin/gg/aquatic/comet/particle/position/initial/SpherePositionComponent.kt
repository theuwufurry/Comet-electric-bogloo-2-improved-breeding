package gg.aquatic.comet.particle.position.initial

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.*
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.particle.position.direction.DirectionSubcomponent
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.script.CompiledScript

class SpherePositionComponent(
    private val radiusScript: CompiledScript,
    private val dir: SphereDirection?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData,
) : ParticleComponent, PositionComponent, DirectionSubcomponent {
    override val priority = 0

    private val directionMap: MutableMap<UUID, Vector3d> = ConcurrentHashMap()

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

            directionMap[otherParticleData.id] = sphereOffset

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

    override fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        if (dir == null) return Vector3d()

        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val magnitude = (dir.magnitude?.eval() as? Number)?.toDouble() ?: 1.0

        return when(dir.type) {
            DirType.INWARDS -> {
                directionMap[otherParticleData.id]!!.mul(-1.0).normalize(magnitude)
            }

            DirType.OUTWARDS -> {
                directionMap[otherParticleData.id]!!.normalize(magnitude)
            }
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "sphere_position"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SpherePositionComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val dir = jsonObject["direction"]?.let top@{
                val magnitudeScript = jsonObject["magnitude"]?.expression()?.let { magnitude -> engine.compile(magnitude, macros) }

                it.asStringOrNull()?.let { str ->
                    when (str) {
                        "inwards" -> { return@top SphereDirection(DirType.INWARDS, magnitudeScript) }
                        "outwards" -> { return@top SphereDirection(DirType.OUTWARDS, magnitudeScript) }
                        else -> { null }
                    }
                }
            }

            return SpherePositionComponent(
                engine.compile(jsonObject.expression("radius") ?: "1", macros),
                dir,
                emitterData, particleData
            )
        }
    }

    class SphereDirection(
        val type: DirType,
        val magnitude: CompiledScript? = null,
    )

    enum class DirType {
        INWARDS,
        OUTWARDS
    }
}