package gg.aquatic.comet.v2.parsing.api.udar

import com.ixume.udar.body.active.ActiveBody
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.WorldRuntime.Companion.cometRuntime
import org.bukkit.Bukkit
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

data class PhysicsBodyWrapper(val actual: ActiveBody, val runtime: EffectRuntime) : ProxyObject {
    private val keys = arrayOf(
        "actual",
        "hookManager",
        "kill"
    )
    private val hooksManagerWrapper = HooksManagerWrapper(runtime, actual.hookManager)
    private val killExecutable = ProxyExecutable {
        Bukkit.getScheduler().runTask(AbstractParticleEmitter.INSTANCE, Runnable {
            actual.physicsWorld.removeBody(actual)
        })
    }

    override fun getMember(key: String?): Any? {
        return when (key) {
            "actual" -> actual
            "hookManager" -> hooksManagerWrapper
            "kill" -> killExecutable
            else -> null
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