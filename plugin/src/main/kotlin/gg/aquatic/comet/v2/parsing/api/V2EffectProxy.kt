package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.v2.runtime.emitter.Effect
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.joml.Vector3d

class V2EffectProxy(val effect: Effect) : ProxyObject {
    private val defaultFields = arrayOf(
        "age",
        "dead",
        "pos",
        "rot",
        "runtime",
        "createParticle",
    )

    private val allFields = mutableSetOf<String>().also { it += defaultFields }
    private val extraData = mutableMapOf<String, Any?>()

    private val createParticleCallable = ProxyExecutable {
        val data = V2ParticleData(effect = effect)
        data.origin = Vector3d(effect.pose.pos)
        data
    }

    var age = 0
    var dead = false

    override fun getMember(key: String?): Any? {
        return when (key) {
            "age" -> age
            "dead" -> dead
            "pos" -> effect.pose.pos
            "rot" -> effect.pose.rot
            "runtime" -> effect.runtime
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
                key == "createParticle"
            ) throw UnsupportedOperationException()

            when (key) {
                "age" -> age = value!!.asInt()
                "dead" -> dead = value!!.asBoolean()
                "pos" -> {
                    val vec = value!!.`as`(Vector3d::class.java)
                    effect.pose.pos.set(vec)
                }
            }
        } else {
            allFields += key
            extraData[key] = value
        }
    }
}