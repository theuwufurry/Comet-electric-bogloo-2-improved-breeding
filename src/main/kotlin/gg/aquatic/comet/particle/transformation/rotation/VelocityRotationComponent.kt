package gg.aquatic.comet.particle.transformation.rotation

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class VelocityRotationComponent : ParticleComponent, RotationComponent {
    val oldPositionMap: MutableMap<UUID, Vector3f> = ConcurrentHashMap()

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (otherParticleData.age == 0.0) {
            oldPositionMap[otherParticleData.id] = Vector3f(
                otherParticleData.relativePosition.x.toFloat(),
                otherParticleData.relativePosition.y.toFloat(),
                otherParticleData.relativePosition.z.toFloat()
            )
            return
        }

        val delta = Vector3f(
            otherParticleData.relativePosition.x.toFloat(),
            otherParticleData.relativePosition.y.toFloat(),
            otherParticleData.relativePosition.z.toFloat()
        ).sub(oldPositionMap[otherParticleData.id]).normalize()
        otherParticleData.rotation.rotationTo(FORWARD_VECTOR, delta)

        oldPositionMap[otherParticleData.id] = Vector3f(
            otherParticleData.relativePosition.x.toFloat(),
            otherParticleData.relativePosition.y.toFloat(),
            otherParticleData.relativePosition.z.toFloat()
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
    }

    companion object : BaseComponentParser {
        val FORWARD_VECTOR = Vector3f(0f, 0f, 1f)

        init {
            ParticleJsonParser.componentParsers += "velocity_rotation" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            return VelocityRotationComponent()
        }
    }
}