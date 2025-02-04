package gg.aquatic.comet.parsing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import org.joml.Vector3d
import org.joml.Vector3f

fun JsonObject.expression(field: String): String? {
    if (field !in keySet()) return null
    val fieldElement = this.get(field)
    if (!fieldElement.isJsonPrimitive) return null
    val fieldPrimitive = fieldElement.asJsonPrimitive
    return ((if (fieldPrimitive.isNumber) fieldPrimitive.asNumber.toString() else fieldPrimitive.asString))
}

fun JsonElement.expression(): String? {
    if (!this.isJsonPrimitive) return null
    val asPrimitive = this.asJsonPrimitive
    return ((if (asPrimitive.isNumber) asPrimitive.asNumber.toString() else asPrimitive.asString))
}

object ParticleJsonParser {
    val componentParsers: MutableMap<String, BaseComponentParser> = mutableMapOf()

    val rateComponentParsers: MutableMap<String, ComponentParser<out RateComponent>> = mutableMapOf()

    lateinit var distanceCullingParser: Pair<String, ComponentParser<DistanceCullingComponent>>
    val updateFrequencyParsers: MutableMap<String, ComponentParser<out UpdateFrequencyComponent>> = mutableMapOf()

    fun parseJsons() {}
}

fun JsonElement.asJsonObjectOrNull(): JsonObject? {
    return if (isJsonObject) asJsonObject else null
}

fun JsonElement.asStringOrNull(): String? {
    return if (isJsonPrimitive && asJsonPrimitive.isString) asString else null
}

fun JsonElement.asBooleanOrNull(): Boolean? {
    return if (isJsonPrimitive && asJsonPrimitive.isBoolean) asBoolean else null
}

fun JsonElement.asNumberOrNull(): Number? {
    return if (isJsonPrimitive && asJsonPrimitive.isNumber) asNumber else null
}

fun JsonElement.asVector3dWithDefaultValues(def: Vector3d = Vector3d()): Vector3d? {
    val obj = if (isJsonObject) asJsonObject else return null
    return Vector3d(
        obj["x"]?.asNumberOrNull()?.toDouble() ?: def.x,
        obj["y"]?.asNumberOrNull()?.toDouble() ?: def.y,
        obj["z"]?.asNumberOrNull()?.toDouble() ?: def.z,
    )
}

fun JsonElement.asVector3fWithDefaultValues(def: Vector3f = Vector3f()): Vector3f? {
    val obj = if (isJsonObject) asJsonObject else return null
    return Vector3f(
        obj["x"]?.asNumberOrNull()?.toFloat() ?: def.x,
        obj["y"]?.asNumberOrNull()?.toFloat() ?: def.y,
        obj["z"]?.asNumberOrNull()?.toFloat() ?: def.z,
    )
}
