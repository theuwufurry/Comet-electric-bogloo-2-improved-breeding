package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.api.particle.LightData
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.v2.runtime.emitter.Effect
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.joml.Quaterniond
import org.joml.Vector3d
import java.util.*

data class V2ParticleData(
    val effect: Effect,
    val uuid: UUID = UUID.randomUUID(),
    var dead: Boolean = false,
    var age: Int = 0,
    var displayData: DisplayData<*> = SpriteData(""),
    var color: Int = -1,
    var translation: Vector3d = Vector3d(),
    var rotation: Quaterniond = Quaterniond(),
    var scale: Vector3d = Vector3d(1.0),
    var billboardConstraints: BillboardConstraints = BillboardConstraints.CENTER,
    var interpolationDelay: Int = 0,
    var transformationInterpolationDuration: Int = 2,
    var teleportationDuration: Int = 1,
    var light: LightData? = null,
    var seeThrough: Boolean = false,
    var shadow: Boolean = false,
    var sensitiveCentering: Boolean = false,
    var origin: Vector3d = Vector3d(),
    var relPos: Vector3d = Vector3d(),
) : ProxyObject {

    private val defaultFields = arrayOf(
        "effect",
        "uuid",
        "dead",
        "age",
        "displayData",
        "color",
        "translation",
        "rotation",
        "scale",
        "billboard",
        "light",
        "seeThrough",
        "shadow",
        "sensitiveCentering",
        "origin",
        "relPos",
        "spawn",
    )

    private val allFields = mutableSetOf<String>().also { it += defaultFields }

    private val extraData = mutableMapOf<String, Any?>()
    private val spawnExecutable = ProxyExecutable { effect.spawnParticle(this) }

    override fun getMember(key: String?): Any? {
        return when (key) {
            "effect" -> effect.proxy
            "uuid" -> uuid
            "dead" -> dead
            "age" -> age
            "displayData" -> displayData
            "color" -> color
            "translation" -> translation
            "rotation" -> rotation
            "scale" -> scale
            "billboard" -> billboardConstraints
            "light" -> light
            "seeThrough" -> seeThrough
            "shadow" -> shadow
            "sensitiveCentering" -> sensitiveCentering
            "origin" -> origin
            "relPos" -> relPos
            "spawn" -> spawnExecutable
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
            if (key == "effect" ||
                key == "uuid" ||
                key == "spawn"
            ) throw UnsupportedOperationException()

            value?.let {
                when (key) {
                    "age" -> age = it.asInt()
                    "dead" -> dead = it.asBoolean()
                    "displayData" -> displayData = it.`as`(DisplayData::class.java)
                    "color" -> color = it.asInt()
                    "translation" -> translation = it.`as`(Vector3d::class.java)
                    "rotation" -> rotation = it.`as`(Quaterniond::class.java)
                    "scale" -> scale = it.`as`(Vector3d::class.java)
                    "billboard" -> billboardConstraints = it.`as`(BillboardConstraints::class.java)
                    "light" -> light = it.`as`(LightData::class.java)
                    "seeThrough" -> seeThrough = it.asBoolean()
                    "shadow" -> shadow = it.asBoolean()
                    "sensitiveCentering" -> sensitiveCentering = it.asBoolean()
                    "origin" -> origin = it.`as`(Vector3d::class.java)
                    "relPos" -> relPos = it.`as`(Vector3d::class.java)
                }
            }
        } else {
            allFields += key
            extraData[key] = value
        }
    }
}