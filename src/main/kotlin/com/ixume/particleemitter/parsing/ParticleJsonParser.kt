package com.ixume.particleemitter.parsing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.UnrealizedEmitter
import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.position.PositionComponent
import com.ixume.particleemitter.particle.texture.SpriteComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import java.io.FileReader

fun JsonObject.expression(field: String): String? {
    if (field !in keySet()) return null
    val fieldElement = this.get(field)
    if (!fieldElement.isJsonPrimitive) return null
    val fieldPrimitive = fieldElement.asJsonPrimitive
    return ((if (fieldPrimitive.isNumber) fieldPrimitive.asNumber.toString() else fieldPrimitive.asString)).also { println(it) }
}

interface ComponentParser<T> {
    fun parse(jsonElement: JsonElement): T?
}

object ParticleJsonParser {
    val particleLifetimeComponentParsers: MutableMap<String, ComponentParser<out ParticleLifetimeComponent>> = mutableMapOf()
    val spriteComponentParsers: MutableMap<String, ComponentParser<out SpriteComponent>> = mutableMapOf()
    val colorComponentParsers: MutableMap<String, ComponentParser<out ColorComponent>> = mutableMapOf()
    val positionComponentParsers: MutableMap<String, ComponentParser<out PositionComponent>> = mutableMapOf()
    val scaleComponentParsers: MutableMap<String, ComponentParser<out ScaleComponent>> = mutableMapOf()

    val emitterLifetimeComponentParsers: MutableMap<String, ComponentParser<out EmitterLifetimeComponent>> = mutableMapOf()
    val rateComponentParsers: MutableMap<String, ComponentParser<out RateComponent>> = mutableMapOf()
    val shapeComponentParsers: MutableMap<String, ComponentParser<out ShapeComponent>> = mutableMapOf()

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
        var particleLifetimeComponent: ParticleLifetimeComponent? = null
        var shapeComponent: ShapeComponent? = null
        var spriteComponent: SpriteComponent? = null
        var colorComponent: ColorComponent? = null
        var emitterLifetimeComponent: EmitterLifetimeComponent? = null
        var positionComponent: PositionComponent? = null
        var scaleComponent: ScaleComponent? = null
        for ((key, componentElement) in componentsObject.entrySet()) {
            if (key in rateComponentParsers) {
                val component = rateComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                    rateComponent = component
                }

                continue
            }

            if (key in emitterLifetimeComponentParsers) {
                val component = emitterLifetimeComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                    emitterLifetimeComponent = component
                }

                continue
            }

            if (key in shapeComponentParsers) {
                val component = shapeComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                    shapeComponent = component
                }

                continue
            }

            if (key in particleLifetimeComponentParsers) {
                val component = particleLifetimeComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                   particleLifetimeComponent = component
                }

                continue
            }

            if (key in colorComponentParsers) {
                val component = colorComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                    colorComponent = component
                }

                continue
            }

            if (key in spriteComponentParsers) {
                val component = spriteComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                    spriteComponent = component
                }

                continue
            }

            if (key in positionComponentParsers) {
                val component = positionComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                    positionComponent = component
                }

                continue
            }

            if (key in scaleComponentParsers) {
                val component = scaleComponentParsers[key]!!.parse(componentElement)
                if (component != null) {
                    scaleComponent = component
                }

                continue
            }
        }

        return UnrealizedEmitter(
            rateComponent ?: return null,
            particleLifetimeComponent ?: return null,
            spriteComponent ?: return null,
            shapeComponent ?: return null,
            colorComponent ?: return null,
            emitterLifetimeComponent ?: return null,
            positionComponent ?: return null,
            scaleComponent ?: return null)
    }
}