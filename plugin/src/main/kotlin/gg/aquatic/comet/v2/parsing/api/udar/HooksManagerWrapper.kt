package gg.aquatic.comet.v2.parsing.api.udar

import com.ixume.udar.body.active.hook.CollisionContext
import com.ixume.udar.body.active.hook.HookManager
import com.ixume.udar.body.active.hook.RemovalLambda
import gg.aquatic.comet.v2.parsing.api.V2EffectProxy
import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.context.WorldContext
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import org.bukkit.World
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

data class HooksManagerWrapper(val runtime: EffectRuntime, val hooksManager: HookManager) : ProxyObject {
    private val keys = arrayOf("addCollisionListener")

    private val addCollisionListener = ProxyExecutable { args ->
        if (args.size != 2) throw IllegalArgumentException("Expected 2 argument!")
        val effect = args[0]!!.asProxyObject<V2EffectProxy>()
        val lambda = args[1]!!
        if (!lambda.canExecute()) throw IllegalArgumentException("Expected 2nd arg to be executable!")

        val listener = { collisionContext: CollisionContext, removal: RemovalLambda ->
            runtime.submitExecutable(object : BoundExecutable {
                override val effect: Effect = effect.effect

                override fun execute(context: WorldContext) {
                    lambda.executeVoid(collisionContext.x, collisionContext.y, collisionContext.z, collisionContext.impulse)
                }

                override fun invalidate() {
                    removal.lambda()
                }
            })
        }

        hooksManager.registerOnCollisionListener(listener)
        effect.effect.registerOnKill { hooksManager.deregisterOnCollisionListener(listener) }
    }

    override fun getMember(key: String?): Any? {
        return when (key) {
            "addCollisionListener" -> addCollisionListener
            else -> throw IllegalArgumentException()
        }
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
}