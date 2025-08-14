package gg.aquatic.comet.particle.transformation.rotation

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.asin
import kotlin.math.atan2

class VelocityRotationComponent(
    private val spriteRotation: Float,
    private val directedRotation: Expr<Number>?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, RotationComponent {
    override val priority = 0
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

        val evaluatedDirectedRotation = directedRotation?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toFloat() ?: 0f

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

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<Component> {
            val obj = jsonElement.asJsonObjectOrNull()
                ?: throw MalformedJsonException("Velocity rotation component is not a json object!")

            val spriteRotation = obj.getExprOrNull("sprite_rotation")
                ?.let { AbstractParticleEmitter.scriptEngineFactory.scriptEngine.eval(it) as Number }?.toFloat() ?: 0f
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            val directedRotation = obj.getExprOrNull("directed_rotation")
                ?.constructExpr<Number>(engine, macros)
                ?.fold({ it }, { return Result.failure(it) })

            return Result.success(
                VelocityRotationComponent(
                    spriteRotation,
                    directedRotation,
                    emitterData, particleData
                )
            )
        }
    }
}