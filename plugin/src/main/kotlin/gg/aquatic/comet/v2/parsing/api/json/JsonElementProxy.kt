package gg.aquatic.comet.v2.parsing.api.json

import com.google.gson.JsonElement
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

/**
 * This represents environment data. Data can be added anyway to emitter via proxy extra data. However for flexibility, it would still be helpful for this to be mutable and constructable from JavaScript.
 */
class JsonElementProxy(val backer: JsonElement) : ProxyObject {
    private val asString = ProxyExecutable { args ->
        check(args.isEmpty())
        return@ProxyExecutable backer.asString
    }

    private val asDouble = ProxyExecutable { args ->
        check(args.isEmpty())
        return@ProxyExecutable backer.asDouble
    }

    private val asBoolean = ProxyExecutable { args ->
        check(args.isEmpty())
        return@ProxyExecutable backer.asDouble
    }

    private val fields = arrayOf("asString", "asDouble", "asBoolean")
    override fun getMember(key: String?): Any? {
        return if (backer.isJsonObject) {
            JsonElementProxy(backer.asJsonObject.get(key))
        } else {
            when (key) {
                "asString" -> asString
                "asDouble" -> asDouble
                "asBoolean" -> asBoolean
                else -> null
            }
        }
    }

    override fun getMemberKeys(): Any? {
        return if (backer.isJsonObject)
            backer.asJsonObject.keySet().toTypedArray()
        else fields
    }

    override fun hasMember(key: String?): Boolean {
        return if (backer.isJsonObject)
            key in backer.asJsonObject.keySet()
        else key in fields
    }

    override fun putMember(key: String?, value: Value?) {
        key ?: return
        val asObj = backer.asJsonObject
        val toAdd = value ?: return
        if (toAdd.isString) {
            asObj.addProperty(key, toAdd.asString())
        } else if (toAdd.isNumber) {
            asObj.addProperty(key, toAdd.asDouble())
        } else if (toAdd.isBoolean) {
            asObj.addProperty(key, toAdd.asBoolean())
        } else {
            val elem = toAdd.asProxyObject<JsonElementProxy>()!!
            asObj.add(key, elem.backer)
        }
    }
}