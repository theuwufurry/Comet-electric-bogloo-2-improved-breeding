package gg.aquatic.comet.particle.action

import com.destroystokyo.paper.ParticleBuilder
import com.google.gson.JsonElement
import com.google.gson.stream.MalformedJsonException
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.*
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.particle.color.addDependency
import org.bukkit.Particle
import org.joml.Vector3d
import org.joml.Vector3f
import java.awt.Color
import javax.script.CompiledScript

class ParticleVanillaSpawnSubAction(
    private val compiledVanillaParticleData: CompiledVanillaParticle
) : SubAction {
    override fun execute(context: ActionContext) {
        context.pose ?: return

        val particlePacket = compiledVanillaParticleData.realize(context) ?: return
        particlePacket.receivers(context.otherEmitterData.emitter!!.players).spawn()
    }

    companion object : ComponentParser<ParticleVanillaSpawnSubAction> {
        override val id: String = "vanilla_particle"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ParticleVanillaSpawnSubAction? {
            val obj = if (jsonElement.isJsonObject) jsonElement.asJsonObject else return null
            val particle = CompiledVanillaParticle.parse(obj["vanilla_particle"] ?: return null, macros)
            return ParticleVanillaSpawnSubAction(particle)
        }
    }
}

class CompiledVanillaParticle(
    private val type: Particle,
    private val compiledData: CompiledData<*>,
    private val longDistance: Boolean,
    private val offset: Vector3d,
    private val maxSpeed: Float,
    private val count: Int
) {
    companion object : ComponentParser<CompiledVanillaParticle> {
        override val id: String = "vanilla_particle"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): CompiledVanillaParticle {
            val obj =
                if (!jsonElement.isJsonObject) throw MalformedJsonException("Particle should be a json object!") else jsonElement.asJsonObject!!
            val type = Particle.valueOf(
                obj["type"]?.asStringOrNull()?.uppercase()
                    ?: throw MalformedJsonException("Particle data needs a type!")
            )
            val longDistance = obj["long_distance"]?.asBooleanOrNull() ?: false
            val offset = obj["offset"]?.asVector3fWithDefaultValues() ?: Vector3f()
            val maxSpeed = obj["max_speed"]?.asNumberOrNull()?.toFloat() ?: 0f
            val count = obj["count"]?.asNumberOrNull()?.toInt() ?: 1

            val data: CompiledData<*> = obj["data"]?.let {
                if (type == Particle.DUST) {
                    parseParticleDustData(it, macros)
                } else CompiledData.EmptyDustData()
            } ?: CompiledData.EmptyDustData()


            return CompiledVanillaParticle(
                type,
                data,
                longDistance,
                Vector3d(
                    offset.x.toDouble(),
                    offset.y.toDouble(),
                    offset.z.toDouble()
                ),
                maxSpeed,
                count
            )
        }
    }

    fun realize(context: ActionContext): ParticleBuilder? {
        val data = compiledData.realize(context.otherEmitterData, context.otherParticleData ?: return null)
        val particleBuilder = when (type) {
            Particle.DUST -> {
                val dustData = data as? CompiledData.ParticleDustData ?: return null
                ParticleBuilder(type).color(
                    org.bukkit.Color.fromRGB(dustData.red, dustData.green, dustData.blue),
                    data.scale
                )
            }

            else -> ParticleBuilder(type)
        }

        val pose = context.pose!!

        particleBuilder.offset(offset.x, offset.y, offset.z)
        particleBuilder.count(count)
        particleBuilder.extra(maxSpeed.toDouble())
        particleBuilder.location(pose.location)

        return particleBuilder
    }
}

interface CompiledData<T> {
    fun realize(otherEmitterData: EmitterData, otherParticleData: gg.aquatic.comet.api.particle.ParticleData): T

    class EmptyDustData : CompiledData<Unit> {
        override fun realize(
            otherEmitterData: EmitterData,
            otherParticleData: gg.aquatic.comet.api.particle.ParticleData
        ) {
            return
        }
    }

    class CompiledDustData(
        private val colorScript: CompiledScript,
        private val scaleScript: CompiledScript,
        private val myEmitterData: EmitterData,
        private val myParticleData: gg.aquatic.comet.api.particle.ParticleData
    ) : CompiledData<ParticleDustData> {
        override fun realize(
            otherEmitterData: EmitterData,
            otherParticleData: gg.aquatic.comet.api.particle.ParticleData
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

    class ParticleDustData(
        val scale: Float,
        val red: Int,
        val green: Int,
        val blue: Int
    )
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