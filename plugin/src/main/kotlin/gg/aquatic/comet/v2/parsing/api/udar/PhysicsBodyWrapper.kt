package gg.aquatic.comet.v2.parsing.api.udar

import com.ixume.udar.body.active.ActiveBody
import gg.aquatic.comet.v2.runtime.WorldRuntime.Companion.cometRuntime
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

data class PhysicsBodyWrapper(val actual: ActiveBody) : ProxyObject {
    private val runtime = actual.world.cometRuntime
    private val keys = arrayOf("actual", "hookManager")
    private val hooksManagerWrapper = HooksManagerWrapper(runtime, actual.hookManager)

    override fun getMember(key: String?): Any? {
        return when (key) {
            "actual" -> actual
            "hookManager" -> hooksManagerWrapper
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