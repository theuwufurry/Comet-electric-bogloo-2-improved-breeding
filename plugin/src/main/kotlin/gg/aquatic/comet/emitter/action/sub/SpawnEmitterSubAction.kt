package gg.aquatic.comet.emitter.action.sub

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.emitter.parent.EmitterSpace
import gg.aquatic.comet.api.parsing.*
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.optimization.TimestampedParticleActions
import gg.aquatic.comet.emitter.optimization.VirtualEmitter
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
import org.bukkit.Location
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Vector3d

/*
emitter can be in:
- world space, same as regular
- parent space, follows parent particle OR parent emitter?
    - emitter space - available in all contexts, binds to the emitter that spawned it
    - particle space - available in particle context, binds to particle

    pass Parent interface with a getLocation type function


mounted emitters need custom handling to convert from tp -> translation
mounted emitters with changing yaw/rotation need constant tp component, meaning can't have optimized
 */

class SpawnEmitterSubAction(
    private val unrealizedEmitterIDs: List<String>,
    private val space: EmitterSpace,
    private val magnitude: Float,
    private val rot: Vector3d?,
    private val invert: Boolean
) : SubAction, PostInit {
    private lateinit var unrealizedEmitters: List<UnrealizedEmitter>

    override fun realize(unrealizedEmitter: AbstractUnrealizedEmitter) {
        unrealizedEmitters = unrealizedEmitterIDs.map {
            ParticleJsonParser.jsonUnrealizedEmitters[it]
                ?: throw NullPointerException("$it is not a valid emitter ID!")
        }
    }

    override fun execute(context: ActionContext) {
        val pose = context.pose ?: throw NullPointerException("Cannot use Spawn Emitter SubAction in this event!")

        val vEm =
            if (context.otherEmitterData.emitter!!.isPregen) context.otherEmitterData.emitter!! as VirtualEmitter else null

        val parent = when (space) {
            EmitterSpace.PARENT_EMITTER -> context.otherEmitterData.emitter!!
            EmitterSpace.PARENT_PARTICLE -> context.otherParticleData!!.particle
            else -> null
        }

        if (rot != null) {
            pose.dir.rotate(Quaterniond().rotateXYZ(rot.x, rot.y, rot.z))
        }

        if (invert) {
            pose.dir.mul(-1.0)
        }

        for (unrealizedEmitter in unrealizedEmitters) {
            val location = Location(
                context.otherEmitterData.world,
                pose.pos.x + pose.dir.x * context.otherEmitterData.emitter!!.random.kotlinRandom.nextDouble() * magnitude,
                pose.pos.y + pose.dir.y * context.otherEmitterData.emitter!!.random.kotlinRandom.nextDouble() * magnitude,
                pose.pos.z + pose.dir.z * context.otherEmitterData.emitter!!.random.kotlinRandom.nextDouble() * magnitude
            ).apply { direction = Vector(pose.dir.x, pose.dir.y, pose.dir.z) }

            val uuid = context.otherEmitterData.emitter!!.random.uuid()

            vEm?.let {
                if (space == EmitterSpace.PARENT_PARTICLE) {
                    (it.particleActionsBuffer[context.otherParticleData!!.id] ?: run {
                        val a = TimestampedParticleActions(
                            context.otherParticleData!!.age.toInt(),
                            it.absoluteTime,
                            mutableListOf()
                        )
                        it.particleActionsBuffer[context.otherParticleData!!.id] = a
                        a
                    }).let { actionsBuffer ->
//                        println("O.PASPAWNING ${unrealizedEmitter.id} at ${it.absoluteTime}")
                        actionsBuffer.actions += { em, pd ->
                            em.realize(
                                unrealizedEmitter,
                                pd,
                                location,
                                em.environmentData,
                                em.audience,
                                em.random,
                                uuid
                            )
                        }
                    }
                } else {
                    it.emitterActionsBuffer.let { actionsBuffer ->
//                        println("O.EMSPAWNING ${unrealizedEmitter.id} at ${it.absoluteTime}")
                        actionsBuffer += { em ->
                            em.realize(
                                unrealizedEmitter,
                                if (space != EmitterSpace.WORLD) em else null,
                                location,
                                em.environmentData,
                                em.audience,
                                em.random,
                                uuid
                            )
                        }
                    }
                }
            }

            context.otherEmitterData.emitter!!.realize(
                unrealizedEmitter,
                parent,
                location,
                context.otherEmitterData.emitter!!.environmentData,
                context.otherEmitterData.emitter!!.audience,
                context.otherEmitterData.emitter!!.random,
                uuid
            )
        }
    }

    companion object : ComponentParser<SpawnEmitterSubAction> {
        override val id: String = "emitter_spawn"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): SpawnEmitterSubAction? {
            if (!(jsonElement.isJsonObject && jsonElement.asJsonObject.has("emitter"))) return null
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            val unrealizedEmitterIDs = if (jsonObject.get("emitter").isJsonPrimitive) {
                listOf(
                    engine.compile(jsonObject.expression("emitter") ?: return null, macros, true).eval() as String
                )
            } else {
                val arr = jsonObject.getAsJsonArray("emitter")
                val ids: MutableList<String> = mutableListOf()
                for (elem in arr) {
                    ids += engine.compile(elem.asString, macros, true).eval() as String
                }

                ids
            }

            val rot = jsonObject["rot"]?.let {
                if (!it.isJsonObject) return@let null
                it as JsonObject
                Vector3d(
                    it["x"]?.asNumberOrNull()?.toDouble()?.let { a -> Math.toRadians(a) } ?: 0.0,
                    it["y"]?.asNumberOrNull()?.toDouble()?.let { a -> Math.toRadians(a) } ?: 0.0,
                    it["z"]?.asNumberOrNull()?.toDouble()?.let { a -> Math.toRadians(a) } ?: 0.0,
                )
            }

            val invert = jsonObject["invert"]?.asBooleanOrNull() ?: false

            val offsetMagnitude = jsonObject.getAsJsonPrimitive("offset")?.asNumber?.toFloat() ?: 0f
            val space = when (jsonObject.getAsJsonPrimitive("space")?.asString) {
                "parent_emitter" -> EmitterSpace.PARENT_EMITTER
                "parent_particle" -> EmitterSpace.PARENT_PARTICLE
                else -> EmitterSpace.WORLD
            }

            return SpawnEmitterSubAction(unrealizedEmitterIDs, space, offsetMagnitude, rot, invert)
        }
    }
}