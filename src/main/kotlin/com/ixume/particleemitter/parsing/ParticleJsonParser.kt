package com.ixume.particleemitter.parsing

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.UnrealizedEmitter
import com.ixume.particleemitter.emitter.lifetime.LifetimeComponent
import com.ixume.particleemitter.emitter.lifetime.LifetimeExpressionComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.rate.SteadyRateComponent
import com.ixume.particleemitter.emitter.shape.PointShapeComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import java.io.FileReader

fun JsonObject.expression(field: String): String? {
    if (field !in keySet()) return null
    val fieldElement = this.get(field)
    if (!fieldElement.isJsonPrimitive) return null
    val fieldPrimitive = fieldElement.asJsonPrimitive
    return ((if (fieldPrimitive.isNumber) fieldPrimitive.asNumber.toString() else fieldPrimitive.asString)).also { println(it) }
}

object ParticleJsonParser {
    lateinit var jsonUnrealizedEmitters: Map<String, UnrealizedEmitter>
        private set

    fun parseJsons() {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder
        if (!dataFolder.exists()) return

        val logger = ParticleEmitter.INSTANCE.logger

        val unrealizedEmitters: MutableMap<String, UnrealizedEmitter> = mutableMapOf()

        for (file in dataFolder.listFiles()!!) {
            if (file.extension != "json") continue

            val rootObject = JsonParser.parseReader(FileReader(file)).asJsonObject

            val componentsObject: JsonObject? = rootObject.getAsJsonObject("components")
            if (componentsObject == null) {
                logger.warning("""Field "components" is null in ${file.name}!""")
                continue
            }

            val emitter = parseComponents(componentsObject)
            emitter?.run {
                unrealizedEmitters += file.nameWithoutExtension to emitter
            }
        }

        jsonUnrealizedEmitters = unrealizedEmitters
    }

    private fun parseComponents(componentsObject: JsonObject): UnrealizedEmitter? {
        var rateComponent: RateComponent? = null
        var lifetimeComponent: LifetimeComponent? = null
        var shapeComponent: ShapeComponent? = null
        for ((key, componentElement) in componentsObject.entrySet()) {
            when (key) {
                "minecraft:emitter_rate_steady" -> {
                    rateComponent = SteadyRateComponent.parse(componentElement)
                }
                "minecraft:particle_lifetime_expression" -> {
                    lifetimeComponent = LifetimeExpressionComponent.parse(componentElement)
                }
                "minecraft:emitter_shape_point" -> {
                    shapeComponent = PointShapeComponent.parse(componentElement)
                }
            }
        }

        return UnrealizedEmitter(
            rateComponent ?: return null,
            lifetimeComponent ?: return null,
            shapeComponent ?: return null)
    }
}