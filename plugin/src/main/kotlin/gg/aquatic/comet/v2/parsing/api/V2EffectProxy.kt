package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.v2.runtime.emitter.Effect
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.joml.Vector3d

class V2EffectProxy(val effect: Effect) : ProxyObject {
    private val defaultFields = arrayOf(
        "age",
        "dead",
        "relPos",
        "pos",
        "rot",
        "parent",
        "runtime",
        "createParticle",
    )

    private val allFields = mutableSetOf<String>().also { it += defaultFields }
    private val extraData = mutableMapOf<String, Any?>()

    private val createParticleCallable = ProxyExecutable {
        return@ProxyExecutable effect.createParticle()
    }

    var age = 0
    var dead = false

    override fun getMember(key: String?): Any? {
        return when (key) {
            "age" -> age
            "dead" -> dead
            "relPos" -> effect.relPose.pos
            "pos" -> effect.pose.pos
            "rot" -> effect.pose.rot
            "parent" -> effect.parent
            "runtime" -> effect.runtime.proxy
            "createParticle" -> createParticleCallable
            else -> extraData[key]
        }
    }

    override fun getMemberKeys(): Any? {
        return allFields.toTypedArray()
    }

    override fun hasMember(key: String?): Boolean {
        return key in allFields
    }

    override fun putMember(key: String?, value: Value?) {
        if (key == null) return
        if (key in defaultFields) {
            if (key == "runtime" ||
                key == "createParticle" ||
                key == "pos"
            ) throw UnsupportedOperationException()

            when (key) {
                "age" -> age = value!!.asInt()
                "dead" -> dead = value!!.asBoolean()
                "parent" -> effect.parent = value!!.`as`(Parent::class.java)
                "relPos" -> {
                    val vec = value!!.`as`(Vector3d::class.java)
                    effect.relPose.pos.set(vec)
                }
            }
        } else {
            allFields += key
            extraData[key] = value
        }
    }
}