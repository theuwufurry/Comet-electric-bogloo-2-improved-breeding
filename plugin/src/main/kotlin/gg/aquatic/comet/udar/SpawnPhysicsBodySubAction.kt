package gg.aquatic.comet.udar

import com.google.gson.JsonElement
import com.ixume.udar.body.active.ActiveBody
import com.ixume.udar.body.active.Cuboid
import com.ixume.udar.body.active.blockEntity
import com.ixume.udar.physicsWorld
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.NotMyType
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.action.ACTION_ID_FIELD
import gg.aquatic.comet.emitter.optimization.VirtualEmitter
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.asUnrealizedEmitters
import org.bukkit.Bukkit
import org.bukkit.Material
import org.joml.Quaterniond
import org.joml.Vector3d
import java.util.concurrent.atomic.AtomicBoolean

class SpawnPhysicsBodySubAction(
    private val unrealizedEmitterIDs: List<String>,
) : SubAction, PostInit {
    private lateinit var unrealizedEmitters: List<UnrealizedEmitter>

    override fun realize(unrealizedEmitter: AbstractUnrealizedEmitter) {
        unrealizedEmitters = unrealizedEmitterIDs.mapNotNull {
            val r = ParticleJsonParser.jsonUnrealizedEmitters[it]

            if (r == null) {
                AbstractParticleEmitter.INSTANCE.logger.severe("$it is not a valid emitter ID!")
            }

            r
        }
    }

    override fun execute(context: ActionContext) {
        val pose = context.pose ?: return

        val uuid = context.otherEmitterData.emitter!!.random.uuid()

        val parent = object : Parent {
            var body: ActiveBody? = null
            override val pose: Pose
                get() {
                    return body?.let {
                        Pose(
                            world = it.world,
                            pos = it.pos,
                            rot = it.q,
                        )
                    } ?: pose
                }
            override val dead: AtomicBoolean
                get() {
                    return body?.dead ?: AtomicBoolean(false)
                }

        }

        val emitter = context.otherEmitterData.emitter!!

        if (context.otherEmitterData.emitter!!.isPregen) {
            val virtual = context.otherEmitterData.emitter!! as VirtualEmitter
            if (context.otherParticleData == null) {
                virtual.emitterActionsBuffer.let { actionsBuffer ->
                    actionsBuffer += { em ->
                        action(pose) {
                            parent.body = it
                            for (unrealizedEmitter in unrealizedEmitters) {
                                em.realize(
                                    unrealizedEmitter,
                                    parent,
                                    pose,
                                    em.environmentData,
                                    em.audience,
                                    em.random,
                                    uuid
                                )
                            }

                            emitter.registerOnKill {
                                Bukkit.getScheduler().runTask(
                                    AbstractParticleEmitter.INSTANCE,
                                    Runnable {
                                        it.physicsWorld.removeBody(it)
                                    })
                            }
                        }
                    }
                }
            } else {
                virtual.addParticleAction(context.otherParticleData!!) { em, pd ->
                    action(pose) {
                        parent.body = it
                        for (unrealizedEmitter in unrealizedEmitters) {
                            em.realize(
                                unrealizedEmitter,
                                parent,
                                pose,
                                em.environmentData,
                                em.audience,
                                em.random,
                                uuid
                            )
                        }

                        emitter.registerOnKill {
                            Bukkit.getScheduler().runTask(
                                AbstractParticleEmitter.INSTANCE,
                                Runnable {
                                    it.physicsWorld.removeBody(it)
                                })
                        }
                    }
                }
            }
        } else {
            action(pose) {
                parent.body = it

                emitter.registerOnKill {
                    Bukkit.getScheduler().runTask(
                        AbstractParticleEmitter.INSTANCE,
                        Runnable {
                            it.physicsWorld.removeBody(it)
                        })
                }
            }
        }

        for (unrealizedEmitter in unrealizedEmitters) {
            context.otherEmitterData.emitter!!.realize(
                unrealizedEmitter,
                parent,
                pose,
                context.otherEmitterData.emitter!!.environmentData,
                context.otherEmitterData.emitter!!.audience,
                context.otherEmitterData.emitter!!.random,
                uuid,
            )
        }
    }

    private inline fun action(pose: Pose, crossinline after: (ActiveBody) -> Unit) {
        val physicsWorld = pose.location.world.physicsWorld ?: return

        Bukkit.getScheduler().runTask(AbstractParticleEmitter.INSTANCE, Runnable {
            val body =
                Cuboid(
                    world = pose.location.world,
                    pos = Vector3d(pose.pos),
                    velocity = Vector3d(),
                    q = Quaterniond(),
                    omega = Vector3d(),
                    width = 1.0,
                    height = 1.0,
                    length = 1.0,
                    density = 1.0,
                    hasGravity = true
                ).blockEntity(Material.COPPER_BLOCK)
            physicsWorld.registerBody(body)
            after(body)
        })
    }

    companion object : ComponentParser<SpawnPhysicsBodySubAction> {
        override val id: String = "spawn_physics_object"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?,
        ): Result<SpawnPhysicsBodySubAction> {
            if (!jsonElement.isJsonObject) return Result.failure(NotMyType())
            val obj = jsonElement.asJsonObject
            val actionIDElem = obj.get(ACTION_ID_FIELD) ?: return Result.failure(NotMyType())
            if (!actionIDElem.isJsonPrimitive) return Result.failure(NotMyType())
            if (!actionIDElem.asJsonPrimitive.isString) return Result.failure(NotMyType())
            if (actionIDElem.asJsonPrimitive.asString != id) return Result.failure(NotMyType())

            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            val unrealizedEmitterIDs =
                obj.get("emitter")?.asUnrealizedEmitters(engine, macros)?.getOrElse { return Result.failure(it) }
                ?: listOf()

            return Result.success(SpawnPhysicsBodySubAction(unrealizedEmitterIDs))
        }
    }
}