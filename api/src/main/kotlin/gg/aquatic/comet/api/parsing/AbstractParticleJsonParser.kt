package gg.aquatic.comet.api.parsing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.parsing.macro.Macro
import org.joml.Vector3d
import org.joml.Vector3f

interface ComponentParser<T> {
    val id: String
    fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): T?
}

interface BaseComponentParser {
    val id: String
    fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component?
}

abstract class AbstractParticleJsonParser {
    abstract fun parseJsons()

    abstract fun getUnrealizedEmitterByID(id: String): AbstractUnrealizedEmitter?
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
