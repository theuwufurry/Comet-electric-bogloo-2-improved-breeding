package gg.aquatic.comet.v2.parsing.api.default

import gg.aquatic.comet.api.particle.data.BillboardConstraints
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

object Billboard : ProxyObject {
    private val fields = arrayOf("FIXED", "VERTICAL", "HORIZONTAL", "CENTER")
    override fun getMember(key: String?): Any? {
        return when (key) {
            "FIXED" -> BillboardConstraints.FIXED
            "VERTICAL" -> BillboardConstraints.VERTICAL
            "HORIZONTAL" -> BillboardConstraints.HORIZONTAL
            "CENTER" -> BillboardConstraints.CENTER
            else -> null
        }
    }

    override fun getMemberKeys(): Any? {
        return fields
    }

    override fun hasMember(key: String?): Boolean {
        return key in fields
    }

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException()
    }
}