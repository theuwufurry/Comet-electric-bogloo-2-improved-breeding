package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.v2.runtime.audience.Audience
import org.bukkit.entity.Player
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

class AudienceProxy(val audience: Audience) : ProxyObject {
    private val members = arrayOf("players", "includes")
    private val includes = ProxyExecutable { args ->
        return@ProxyExecutable audience.includes(args[0]!!.`as`(Player::class.java))
    }

    override fun getMember(key: String?): Any? {
        return when(key) {
            "players" -> audience.players
            "includes" -> includes
            else -> null
        }
    }

    override fun getMemberKeys(): Any? {
        return members
    }

    override fun hasMember(key: String?): Boolean {
        return key in members
    }

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException()
    }
}