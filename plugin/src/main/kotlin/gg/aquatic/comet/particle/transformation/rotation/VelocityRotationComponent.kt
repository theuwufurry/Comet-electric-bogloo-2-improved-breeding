package gg.aquatic.comet.particle.transformation.rotation

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.script.CompiledScript
import kotlin.math.asin
import kotlin.math.atan2

class VelocityRotationComponent(
    private val spriteRotation: Float,
    private val directedRotation: CompiledScript?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, RotationComponent {
    val oldPositionMap: MutableMap<UUID, Vector3f> = ConcurrentHashMap()

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
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

        val yaw = atan2(delta.x, delta.z) + Math.PI.toFloat()
        val pitch = asin(delta.y) + Math.PI.toFloat() * -0.5f

        val evaluatedDirectedRotation = (directedRotation?.eval() as? Number)?.toFloat() ?: 0f

        otherParticleData.rotation = Quaternionf()
            .rotationY(yaw)
            .rotateX(pitch)
            .rotateY(evaluatedDirectedRotation)
            .rotateZ(spriteRotation)

        oldPositionMap[otherParticleData.id] = Vector3f(
            otherParticleData.relativePosition.x.toFloat(),
            otherParticleData.relativePosition.y.toFloat(),
            otherParticleData.relativePosition.z.toFloat()
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
    }

    companion object : BaseComponentParser {
        override val id: String = "velocity_rotation"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            val obj = jsonElement.asJsonObjectOrNull() ?: throw MalformedJsonException("Velocity rotation component is not a json object!")

            val spriteRotation = obj.expression("sprite_rotation")?.let { AbstractParticleEmitter.scriptEngineFactory.scriptEngine.eval(it) as Number }?.toFloat() ?: 0f
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            val directedRotation = engine.compile(obj.expression("directed_rotation") ?: "0", macros)

            return VelocityRotationComponent(
                spriteRotation,
                directedRotation,
                emitterData, particleData
            )
        }
    }
}