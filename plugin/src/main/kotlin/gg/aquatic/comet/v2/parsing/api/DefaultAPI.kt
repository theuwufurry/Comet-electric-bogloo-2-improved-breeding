package gg.aquatic.comet.v2.parsing.api

import com.ixume.udar.body.active.ActiveBody
import com.ixume.udar.body.active.Cuboid
import com.ixume.udar.body.active.blockEntity
import com.ixume.udar.physicsWorld
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.udar.UdarBodyParent
import gg.aquatic.comet.v2.parsing.api.udar.PhysicsBodyWrapper
import gg.aquatic.comet.v2.runtime.WorldRuntime.Companion.cometRuntime
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.graalvm.polyglot.Value
import org.joml.Quaterniond
import org.joml.Vector3d

object DefaultAPI {
    fun createSpriteData(id: String): SpriteData {
        return SpriteData(id)
    }

    @JvmOverloads
    fun spawnPhysicsObject(
        effect: Value,
        x: Double, y: Double, z: Double,
        callback: Value? = null,
    ) {
        val effect = effect.asProxyObject<Effect>()
        val world = effect.pose.world
        val physicsWorld = world.physicsWorld ?: return
        Bukkit.getScheduler().runTask(AbstractParticleEmitter.INSTANCE, Runnable {
            val body =
                Cuboid(
                    world = world,
                    pos = Vector3d(x, y, z),
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
            effect.registerOnKill {
                Bukkit.getScheduler().runTask(AbstractParticleEmitter.INSTANCE, Runnable {
                    physicsWorld.removeBody(body)
                })
            }

            callback?.let {
                world.cometRuntime.submitExecutable(object : BoundExecutable {
                    override val effect: Effect = effect

                    override fun execute(world: World) {
                        callback.execute(PhysicsBodyWrapper(body))
                    }
                })
            }
        })
    }

    @JvmOverloads
    fun spawnEmitter(
        effect: Value,
        emitterID: String,
        options: Value,
        callback: Value? = null,
    ) {
        val effect = effect.asProxyObject<Effect>()
        val world = effect.pose.world
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
                    override val effect: Effect = effect

                    override fun execute(world: World) {
                        it.execute(world)
                    }
                })
            }
        }
    }

    fun parentOf(body: ActiveBody): UdarBodyParent {
        return UdarBodyParent(body)
    }
}