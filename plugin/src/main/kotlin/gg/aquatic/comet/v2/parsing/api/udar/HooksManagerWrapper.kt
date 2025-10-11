package gg.aquatic.comet.v2.parsing.api.udar

import com.ixume.udar.body.active.hook.CollisionContext
import com.ixume.udar.body.active.hook.HookManager
import com.ixume.udar.body.active.hook.RemovalLambda
import gg.aquatic.comet.v2.runtime.EmitterRuntime
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import org.bukkit.World
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

data class HooksManagerWrapper(val runtime: EmitterRuntime, val hooksManager: HookManager) : ProxyObject {
    private val keys = arrayOf("addCollisionListener")

    private val addCollisionListener = ProxyExecutable { args ->
        if (args.size != 2) throw IllegalArgumentException("Expected 2 argument!")
        val effect = args[0]!!.asProxyObject<Effect>()
        val lambda = args[1]!!
        if (!lambda.canExecute()) throw IllegalArgumentException("Expected 2nd arg to be executable!")

        val listener = { context: CollisionContext, removal: RemovalLambda ->
            runtime.submitExecutable(object : BoundExecutable {
                override val effect: Effect = effect

                override fun execute(world: World) {
                    lambda.executeVoid(context.x, context.y, context.z, context.impulse)
                }

                override fun invalidate() {
                    removal.lambda()
                }
            })
        }

        hooksManager.registerOnCollisionListener(listener)
        effect.registerOnKill { hooksManager.deregisterOnCollisionListener(listener) }
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