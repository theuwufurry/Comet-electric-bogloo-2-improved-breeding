package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.color.addDependency
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
import javax.script.CompiledScript

class ParticleVanillaSpawnSubAction(
    private val compiledVanillaParticleData: CompiledVanillaParticle
) : SubAction {
    override fun execute(context: ActionContext) {
        val playerManager = PacketEvents.getAPI().playerManager
        context.pos ?: return

        val particlePacket = compiledVanillaParticleData.realize(context) ?: return

        context.otherEmitterData.emitter!!.players().forEach { playerManager.sendPacket(it, particlePacket) }
    }

    companion object : ComponentParser<ParticleVanillaSpawnSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ParticleVanillaSpawnSubAction? {
            val obj = if (jsonElement.isJsonObject) jsonElement.asJsonObject else return null
            val particle = CompiledVanillaParticle.parse(obj["vanilla_particle"] ?: return null, macros)
            return ParticleVanillaSpawnSubAction(particle)
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
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): CompiledVanillaParticle {
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
                    parseParticleDustData(it, macros)
                } else CompiledData.EmptyDustData()
            } ?: CompiledData.EmptyDustData()


            return CompiledVanillaParticle(
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
        }
    }

    fun realize(context: ActionContext): WrapperPlayServerParticle? {
        val data = compiledData.realize(context.otherEmitterData, context.otherParticleData ?: return null)
        val particle = when (type) {
            ParticleTypes.DUST -> Particle(type as ParticleType<ParticleDustData>, data as ParticleDustData)
            else -> Particle(type as ParticleType<ParticleData>, data)
        }

        context.pos!!

        return WrapperPlayServerParticle(
            particle,
            longDistance,
            Vector3d(
                context.pos.x,
                context.pos.y,
                context.pos.z
            ),
            offset,
            maxSpeed,
            count
        )
    }
}

interface CompiledData<T : ParticleData> {
    fun realize(otherEmitterData: EmitterData, otherParticleData: gg.aquatic.comet.particle.ParticleData): T

    class EmptyDustData : CompiledData<ParticleData> {
        override fun realize(
            otherEmitterData: EmitterData,
            otherParticleData: gg.aquatic.comet.particle.ParticleData
        ): ParticleData {
            return ParticleData()
        }
    }

    class CompiledDustData(
        private val colorScript: CompiledScript,
        private val scaleScript: CompiledScript,
        private val myEmitterData: EmitterData,
        private val myParticleData: gg.aquatic.comet.particle.ParticleData
    ) : CompiledData<ParticleDustData> {
        override fun realize(
            otherEmitterData: EmitterData,
            otherParticleData: gg.aquatic.comet.particle.ParticleData
        ): ParticleDustData {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            val color = (colorScript.eval() as Color)
            return ParticleDustData(
                (scaleScript.eval() as Number).toFloat(),
                color.red, color.green, color.blue
            )
        }
    }
}

fun parseParticleDustData(jsonElement: JsonElement, macros: Map<String, Macro>?): CompiledData.CompiledDustData {
    val obj =
        if (jsonElement.isJsonObject) jsonElement.asJsonObject else throw MalformedJsonException("Dust particle data should be an object!")
    val emitterData = EmitterData()
    val (engine, particleData) = particleEngine(emitterData)

    val colorScript =
        engine.compile(obj.expression("color")?.addDependency() ?: "new Color(255, 255, 255)".addDependency(), macros)
    val scaleScript = engine.compile(obj.expression("scale") ?: "1", macros)

    return CompiledData.CompiledDustData(
        colorScript,
        scaleScript,
        emitterData, particleData
    )
}