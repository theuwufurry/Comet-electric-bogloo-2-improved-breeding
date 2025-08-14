package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.*
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.particle.color.addDependency
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.waves.shadow.com.retrooper.packetevents.PacketEvents
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.particle.Particle
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.particle.data.ParticleData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.particle.data.ParticleDustData
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.particle.type.ParticleType
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.particle.type.ParticleTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d
import gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3f
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle
import java.awt.Color

class ParticleVanillaSpawnSubAction(
    private val compiledVanillaParticleData: CompiledVanillaParticle
) : SubAction {
    override fun execute(context: ActionContext) {
        val playerManager = PacketEvents.getAPI().playerManager
        context.pose ?: return

        val particlePacket = compiledVanillaParticleData.realize(context) ?: return

        context.otherEmitterData.emitter!!.players.forEach { playerManager.sendPacketSilently(it, particlePacket) }
    }

    companion object : ComponentParser<ParticleVanillaSpawnSubAction> {
        override val id: String = "vanilla_particle"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): Result<ParticleVanillaSpawnSubAction> {
            val obj = if (jsonElement.isJsonObject) {
                jsonElement.asJsonObject
            } else return Result.failure(NotMyType())
            val particle = CompiledVanillaParticle.parse(
                obj["vanilla_particle"] ?: return Result.failure(
                    NotMyType()
                ), macros
            )
            return Result.success(ParticleVanillaSpawnSubAction(particle.fold({ it }, { return Result.failure(it) })))
        }
    }
}

class CompiledVanillaParticle(
    private val type: ParticleType<out ParticleData>,
    private val compiledData: CompiledData<*>,
    private val longDistance: Boolean,
    private val offset: Vector3f,
    private val maxSpeed: Float,
    private val count: Int
) {
    companion object : ComponentParser<CompiledVanillaParticle> {
        override val id: String = "vanilla_particle"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<CompiledVanillaParticle> {
            val obj =
                if (!jsonElement.isJsonObject) throw MalformedJsonException("Particle should be a json object!") else jsonElement.asJsonObject!!
            val type: ParticleType<out ParticleData> = ParticleTypes.getByName(
                obj["type"]?.asStringOrNull() ?: throw MalformedJsonException("Particle data needs a type!")
            ) ?: throw MalformedJsonException("Invalid particle type!")
            val longDistance = obj["long_distance"]?.asBooleanOrNull() ?: false
            val offset = obj["offset"]?.asVector3fWithDefaultValues() ?: org.joml.Vector3f()
            val maxSpeed = obj["max_speed"]?.asNumberOrNull()?.toFloat() ?: 0f
            val count = obj["count"]?.asNumberOrNull()?.toInt() ?: 1

            val data: CompiledData<*> = obj["data"]?.let {
                if (type == ParticleTypes.DUST) {
                    parseParticleDustData(it, macros).fold({ it }, { return Result.failure(it) })
                } else CompiledData.EmptyDustData()
            } ?: CompiledData.EmptyDustData()

            return Result.success(
                CompiledVanillaParticle(
                    type,
                    data,
                    longDistance,
                    Vector3f(
                        offset.x,
                        offset.y,
                        offset.z
                    ),
                    maxSpeed,
                    count
                )
            )
        }
    }

    fun realize(context: ActionContext): WrapperPlayServerParticle? {
        val data = compiledData.realize(context.otherEmitterData, context.otherParticleData ?: return null).getOrThrow()
        val particle = when (type) {
            ParticleTypes.DUST -> Particle(
                type as? ParticleType<ParticleDustData> ?: return null,
                data as ParticleDustData
            )

            else -> Particle(type as ParticleType<ParticleData>, data)
        }

        val pose = context.pose!!

        return WrapperPlayServerParticle(
            particle,
            longDistance,
            Vector3d(
                pose.pos.x,
                pose.pos.y,
                pose.pos.z
            ),
            offset,
            maxSpeed,
            count
        )
    }
}

interface CompiledData<T : ParticleData> {
    fun realize(otherEmitterData: EmitterData, otherParticleData: gg.aquatic.comet.api.particle.ParticleData): Result<T>

    class EmptyDustData : CompiledData<ParticleData> {
        override fun realize(
            otherEmitterData: EmitterData,
            otherParticleData: gg.aquatic.comet.api.particle.ParticleData
        ): Result<ParticleData> {
            return Result.success(ParticleData())
        }
    }

    class CompiledDustData(
        private val colorScript: Expr<Color>,
        private val scaleScript: Expr<Number>?,
        private val myEmitterData: EmitterData,
        private val myParticleData: gg.aquatic.comet.api.particle.ParticleData
    ) : CompiledData<ParticleDustData> {
        override fun realize(
            otherEmitterData: EmitterData,
            otherParticleData: gg.aquatic.comet.api.particle.ParticleData
        ): Result<ParticleDustData> {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            val color = colorScript.eval().fold({ it }, { return Result.failure(it) })
            return Result.success(
                ParticleDustData(
                    scaleScript?.eval()?.fold({ it.toFloat() }, { return Result.failure(it) }) ?: 1f,
                    color.red, color.green, color.blue
                )
            )
        }
    }
}

fun parseParticleDustData(
    jsonElement: JsonElement,
    macros: Map<String, Macro>?
): Result<CompiledData.CompiledDustData> {
    val obj =
        if (jsonElement.isJsonObject) jsonElement.asJsonObject else throw MalformedJsonException("Dust particle data should be an object!")
    val emitterData = EmitterData()
    val (engine, particleData) = particleEngine(emitterData)


    val colorScript = obj.getExprOrNull("bg")
        ?.addDependency()
        ?.constructExpr<Color>(engine, macros)
        ?.fold({ it }, { return Result.failure(it) })
        ?: JSExpr(
            engine.compile(
                input = "new Color(255, 255, 255)",
                macros = macros,
            ).getOrThrow()
        )

    val scaleScript =
        obj.getExprOrNull("scale")?.constructExpr<Number>(engine, macros)?.fold({ it }, { return Result.failure(it) })

    return Result.success(
        CompiledData.CompiledDustData(
            colorScript,
            scaleScript,
            emitterData, particleData
        )
    )
}