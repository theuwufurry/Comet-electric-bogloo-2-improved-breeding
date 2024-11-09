package com.ixume.particleemitter.parsing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterTickersHolder
import com.ixume.particleemitter.emitter.UnrealizedEmitter
import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.emitter.lifetime.TimedEmitterLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.rate.SteadyRateComponent
import com.ixume.particleemitter.emitter.recursive.RecursiveEmitterComponent
import com.ixume.particleemitter.emitter.shape.PointShapeComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.parsing.macro.MacrosParser
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.color.ConstantColorComponent
import com.ixume.particleemitter.particle.color.GradientColorComponent
import com.ixume.particleemitter.particle.display.DisplayComponent
import com.ixume.particleemitter.particle.display.model.ConstantModelComponent
import com.ixume.particleemitter.particle.display.sprite.ConstantSpriteComponent
import com.ixume.particleemitter.particle.display.sprite.ExpressionSpriteComponent
import com.ixume.particleemitter.particle.display.sprite.FlipbookSpriteComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeExpressionComponent
import com.ixume.particleemitter.particle.position.*
import com.ixume.particleemitter.particle.transformation.rotation.ExpressionRotationComponent
import com.ixume.particleemitter.particle.transformation.rotation.RotationComponent
import com.ixume.particleemitter.particle.transformation.scale.ExpressionScaleComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import net.minecraft.world.entity.Display.BillboardConstraints
import java.io.FileReader

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

interface ComponentParser<T> {
    fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): T?
//    fun parseProvider(jsonElement: JsonElement, macros: Map<String, Macro>?): ComponentProvider<T>?
}

object ParticleJsonParser {
    val particleLifetimeComponentParsers: MutableMap<String, ComponentParser<out ParticleLifetimeComponent>> =
        mutableMapOf()
    val displayComponentParsers: MutableMap<String, ComponentParser<out DisplayComponent>> = mutableMapOf()
    val colorComponentParsers: MutableMap<String, ComponentParser<out ColorComponent>> = mutableMapOf()
    val positionComponentParsers: MutableMap<String, ComponentParser<out PositionComponent>> = mutableMapOf()
    val scaleComponentParsers: MutableMap<String, ComponentParser<out ScaleComponent>> = mutableMapOf()
    val rotationComponentParsers: MutableMap<String, ComponentParser<out RotationComponent>> = mutableMapOf()

    val emitterLifetimeComponentParsers: MutableMap<String, ComponentParser<out EmitterLifetimeComponent>> =
        mutableMapOf()
    val rateComponentParsers: MutableMap<String, ComponentParser<out RateComponent>> = mutableMapOf()
    val shapeComponentParsers: MutableMap<String, ComponentParser<out ShapeComponent>> = mutableMapOf()

    lateinit var recursiveEmitterComponentParser: Pair<String, ComponentParser<RecursiveEmitterComponent>>

    fun init() {
        MacrosParser

        TimedEmitterLifetimeComponent

        SteadyRateComponent

        PointShapeComponent

        ConstantColorComponent
        GradientColorComponent

        ParticleLifetimeExpressionComponent

        ConstantSpriteComponent
        ExpressionSpriteComponent
        FlipbookSpriteComponent

        ConstantModelComponent

        RecursiveEmitterComponent

        ExpressionPositionComponent
        MotionPositionComponent

        ExpressionScaleComponent

        ExpressionRotationComponent

        RecursiveEmitterComponent
    }

    lateinit var jsonUnrealizedEmitters: Map<String, UnrealizedEmitter>
        private set

    fun parseJsons() {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder
        if (!dataFolder.exists()) return

        val unrealizedEmitters: MutableMap<String, UnrealizedEmitter> = mutableMapOf()

        EmitterTickersHolder.kill()

        for (file in dataFolder.listFiles()!!) {
            if (file.extension != "json") continue

            val rootObject = JsonParser.parseReader(FileReader(file)).asJsonObject

            val emitter = parseComponents(rootObject)
            emitter?.run {
                unrealizedEmitters += file.nameWithoutExtension to emitter
            } ?: run {
                ParticleEmitter.INSTANCE.logger.warning("""Field "components" is null in ${file.name}!""")
            }
        }

        jsonUnrealizedEmitters = unrealizedEmitters

        postInit()
    }

    private fun parseComponents(rootObject: JsonObject): UnrealizedEmitter? {
        val macros: Map<String, Macro>? = rootObject.getAsJsonObject("macros")?.let { MacrosParser.parseMacros(it) }

        val componentsObject: JsonObject = rootObject.getAsJsonObject("components") ?: return null

        var rateComponent: RateComponent? = null
        var particleLifetimeComponent: ParticleLifetimeComponent? = null
        var shapeComponent: ShapeComponent? = null
        var displayComponent: DisplayComponent? = null
        var colorComponent: ColorComponent? = null
        var emitterLifetimeComponent: EmitterLifetimeComponent? = null
        var positionComponent: PositionComponent? = null
        var scaleComponent: ScaleComponent? = null
        var rotationComponent: RotationComponent? = null
        var billboardConstraints: BillboardConstraints? = null
        var recursiveEmitterComponent: RecursiveEmitterComponent? = null

        for ((key, componentElement) in componentsObject.entrySet()) {
            if (key == "display_type") {
                billboardConstraints = when (componentElement.asString) {
                    "fixed" -> BillboardConstraints.FIXED
                    "center" -> BillboardConstraints.CENTER
                    else -> BillboardConstraints.CENTER
                }
            }

            if (key in rateComponentParsers) {
                val component = rateComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    rateComponent = component
                }

                continue
            }

            if (key in emitterLifetimeComponentParsers) {
                val component = emitterLifetimeComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    emitterLifetimeComponent = component
                }

                continue
            }

            if (key in shapeComponentParsers) {
                val component = shapeComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    shapeComponent = component
                }

                continue
            }

            if (key in particleLifetimeComponentParsers) {
                val component = particleLifetimeComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    particleLifetimeComponent = component
                }

                continue
            }

            if (key in colorComponentParsers) {
                val component = colorComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    colorComponent = component
                }

                continue
            }

            if (key in displayComponentParsers) {
                val component = displayComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    displayComponent = component
                }

                continue
            }

            if (key in positionComponentParsers) {
                val component = positionComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    positionComponent = component
                }

                continue
            }

            if (key in scaleComponentParsers) {
                val component = scaleComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    scaleComponent = component
                }

                continue
            }

            if (key in rotationComponentParsers) {
                val component = rotationComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    rotationComponent = component
                }

                continue
            }

            if (key == recursiveEmitterComponentParser.first) {
                recursiveEmitterComponent = recursiveEmitterComponentParser.second.parse(componentElement, macros)
            }
        }

        if (rateComponent == null) println("Rate component null!")
        if (particleLifetimeComponent == null) println("Particle lifetime null!")
        if (displayComponent == null) println("Display null!")
        if (shapeComponent == null) println("Shape null!")
        if (colorComponent == null) println("Color null!")
        if (emitterLifetimeComponent == null) println("Emitter life null!")
        if (positionComponent == null) println("Position null!")
        if (scaleComponent == null) println("Scale null!")
        if (rotationComponent == null) println("Rotation null!")
        if (billboardConstraints == null) println("Billboard null!")

        return UnrealizedEmitter(
            rateComponent ?: return null,
            particleLifetimeComponent ?: return null,
            displayComponent ?: return null,
            shapeComponent ?: return null,
            colorComponent ?: return null,
            emitterLifetimeComponent ?: return null,
            positionComponent ?: return null,
            scaleComponent ?: return null,
            rotationComponent ?: return null,
            recursiveEmitterComponent,
            billboardConstraints ?: return null
        )
    }

    private fun postInit() {
        for ((_, unrealizedEmitter) in jsonUnrealizedEmitters) {
            unrealizedEmitter.recursiveEmitterComponent?.realize()
            (unrealizedEmitter.positionComponent as? PostInit)?.realize()
        }
    }
}