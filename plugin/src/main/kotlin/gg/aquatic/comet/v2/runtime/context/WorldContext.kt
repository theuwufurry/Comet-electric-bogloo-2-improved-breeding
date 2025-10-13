package gg.aquatic.comet.v2.runtime.context

import com.google.gson.JsonNull
import com.ixume.udar.body.active.JavaModelBody
import com.ixume.udar.physicsWorld
import com.ixume.udar.rp.RPManager
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.v2.parsing.api.V2EffectProxy
import gg.aquatic.comet.v2.parsing.api.json.JsonElementProxy
import gg.aquatic.comet.v2.parsing.api.udar.PhysicsBodyWrapper
import gg.aquatic.comet.v2.parsing.getMemberOrNull
import gg.aquatic.comet.v2.runtime.WorldRuntime.Companion.cometRuntime
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import org.joml.Quaterniond
import org.joml.Vector3d
import java.util.concurrent.CopyOnWriteArrayList

class WorldContext(
    val world: World,
    val members: Map<String, Any>,
) : ProxyObject {
    private val keys = members.keys.toTypedArray()

    override fun getMember(key: String?): Any? {
        return members[key]
    }

    override fun getMemberKeys(): Any? {
        return keys
    }

    override fun hasMember(key: String?): Boolean {
        return key in keys
    }

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException()
    }

    companion object {
        val transformers = CopyOnWriteArrayList<WorldContextTransformer>()
            .apply {
                this += Method("spawnPhysicsModel") {
                    val effectArg = it[0]!!
                    val options = it[1]!!
                    val callback = it.getOrNull(2)

                    val modelID = options.getMember("id").asString()
                    val pos = options.getMember("pos").`as`(Vector3d::class.java)
                    val scale = options.getMember("scale")?.asDouble() ?: 1.0
                    val hasGravity = options.getMember("hasGravity")?.asBoolean() ?: true
                    val velocity = options.getMember("velocity")?.`as`(Vector3d::class.java) ?: Vector3d()
                    val omega = options.getMember("omega")?.`as`(Vector3d::class.java) ?: Vector3d()

                    val effect = effectArg.asProxyObject<V2EffectProxy>()
                    val world = effect.effect.relPose.world
                    val physicsWorld = world.physicsWorld ?: return@Method null
                    val worldRuntime = world.cometRuntime
                    Bukkit.getScheduler().runTask(AbstractParticleEmitter.Companion.INSTANCE, Runnable {
                        val model = RPManager.modelMap[modelID] ?: return@Runnable
                        val body = JavaModelBody.Companion.construct(physicsWorld, pos, model, scale)
                        body.hasGravity = hasGravity
                        body.velocity.set(velocity)
                        body.omega.set(omega)

                        physicsWorld.registerBody(body)
                        effect.effect.registerOnKill {
                            Bukkit.getScheduler().runTask(AbstractParticleEmitter.Companion.INSTANCE, Runnable {
                                physicsWorld.removeBody(body)
                            })
                        }

                        callback?.let {
                            worldRuntime.submitExecutable(object : BoundExecutable {
                                override val effect: Effect = effect.effect

                                override fun execute(context: WorldContext) {
                                    callback.execute(PhysicsBodyWrapper(body, worldRuntime))
                                }
                            })
                        }
                    })
                }
                this += Method("spawnEmitter") { args ->
                    val effectArg = args[0]!!
                    val emitterID = args[1]!!.asString()
                    val options = args[2]!!
                    val callback = args.getOrNull(3)

                    val effect = effectArg.asProxyObject<V2EffectProxy>()
                    val world = effect.effect.relPose.world
                    val pos = options.getMember("pos").`as`(Vector3d::class.java)
                    val parent = options.getMember("parent")?.`as`(Parent::class.java)

                    val em = ParticleJsonParser.jsonUnrealizedEmitters[emitterID]
                             ?: throw IllegalArgumentException("No emitter with $emitterID exists!")
                    em.realize(
                        parent = parent,
                        pose = Pose(world, pos, Quaterniond()),
                        environmentData = EnvironmentData(),
                        mount = null,
                        yawpitchSupplier = null,
                    ) {
                        callback?.let {
                            world.cometRuntime.submitExecutable(object : BoundExecutable {
                                override val effect: Effect = effect.effect

                                override fun execute(context: WorldContext) {
                                    it.execute(context)
                                }
                            })
                        }
                    }
                }

                this += Method("spawnEffect") { args ->
                    val effectArg = args[0]!!
                    val id = args[1]!!.asString()
                    val options = args[2]!!
                    val callback = args.getOrNull(3)

                    val effect = effectArg.asProxyObject<V2EffectProxy>()
                    val world = effect.effect.relPose.world
                    val runtime = world.cometRuntime
                    val pos = options.getMemberOrNull("relPos")?.`as`(Vector3d::class.java) ?: Vector3d()
                    val parent = options.getMember("parent")?.`as`(Parent::class.java)
                    val data = options.getMemberOrNull("data")?.asProxyObject<JsonElementProxy>()?.backer ?: JsonNull.INSTANCE

                    runtime.getAPI(id) { api ->
                        api ?: return@getAPI

                        api.realize(
                            world = world,
                            pose = Pose(
                                world = world,
                                pos = pos,
                                rot = Quaterniond(),
                            ),
                            parent = parent,
                            data = data,
                        ) { eff -> callback?.executeVoid(eff.proxy) }
                    }
                }

                this += Method("spawnVanillaParticle") { args ->
                    val effectArg = args[0]!!
                    val options = args[1]!!

                    val particleStr = options.getMember("particle").asString().uppercase()
                    val particle = Particle.valueOf(particleStr)
                    val pos = options.getMember("pos")?.`as`(Vector3d::class.java) ?: return@Method null
                    val count = options.getMember("count")?.asInt() ?: 1
                    val offset = options.getMember("offset")?.`as`(Vector3d::class.java) ?: Vector3d()
                    val extra = options.getMember("extra")?.asDouble() ?: 0.0
                    val force = options.getMember("force")?.asBoolean() ?: false
                    val dataVal = options.getMember("data")
                    val dataType = particle.dataType
                    val data: Any? = if (dataVal != null && !dataVal.isNull && dataType != Void::class.java) {
                        dataVal.`as`(dataType)
                    } else null

                    val effectProxy = effectArg.asProxyObject<V2EffectProxy>()
                    val world = effectProxy.effect.relPose.world

                    Bukkit.getScheduler().runTask(AbstractParticleEmitter.Companion.INSTANCE, Runnable {
                        val location = Location(world, pos.x, pos.y, pos.z)
                        world.spawnParticle(particle, location, count, offset.x, offset.y, offset.z, extra, data, force)
                    })
                }
            }

        fun construct(
            world: World,
        ): WorldContext {
            val members = mutableMapOf<String, Any>()
            for (transformer in transformers) {
                members += transformer.transform(world)
            }

            return WorldContext(world, members)
        }
    }
}