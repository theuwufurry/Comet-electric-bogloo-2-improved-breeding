package gg.aquatic.comet.parsing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.aquatic.comet.Component
import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.emitter.EmitterTickersHolder
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.action.event.EmitterDeathComponent
import gg.aquatic.comet.emitter.action.event.EmitterInitComponent
import gg.aquatic.comet.emitter.action.event.EmitterTickComponent
import gg.aquatic.comet.emitter.action.event.EmitterTimelineComponent
import gg.aquatic.comet.emitter.lifetime.EmitterLifetimeComponent
import gg.aquatic.comet.emitter.lifetime.TimedEmitterLifetimeComponent
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.IntervalUpdateFrequencyComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.ManualUpdateFrequencyComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.emitter.rate.InstantRateComponent
import gg.aquatic.comet.emitter.rate.ManualRateComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import gg.aquatic.comet.emitter.rate.SteadyRateComponent
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.parsing.macro.MacrosParser
import gg.aquatic.comet.particle.action.event.ParticleDeathComponent
import gg.aquatic.comet.particle.action.event.ParticleInitComponent
import gg.aquatic.comet.particle.action.event.ParticleTickComponent
import gg.aquatic.comet.particle.action.event.ParticleTimelineComponent
import gg.aquatic.comet.particle.color.ColorComponent
import gg.aquatic.comet.particle.color.ConstantColorComponent
import gg.aquatic.comet.particle.color.GradientColorComponent
import gg.aquatic.comet.particle.data.BillboardConstraints
import gg.aquatic.comet.particle.display.DisplayComponent
import gg.aquatic.comet.particle.display.model.ConstantModelComponent
import gg.aquatic.comet.particle.display.sprite.ConstantSpriteComponent
import gg.aquatic.comet.particle.display.sprite.ExpressionSpriteComponent
import gg.aquatic.comet.particle.display.sprite.FlipbookSpriteComponent
import gg.aquatic.comet.particle.lifetime.ParticleLifetimeComponent
import gg.aquatic.comet.particle.lifetime.ParticleLifetimeExpressionComponent
import gg.aquatic.comet.particle.position.AttractorPositionComponent
import gg.aquatic.comet.particle.position.ExpressionPositionComponent
import gg.aquatic.comet.particle.position.MotionPositionComponent
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.particle.position.initial.InitialExpressionPositionComponent
import gg.aquatic.comet.particle.position.initial.SpherePositionComponent
import gg.aquatic.comet.particle.transformation.rotation.DirectionRotationComponent
import gg.aquatic.comet.particle.transformation.rotation.ExpressionRotationComponent
import gg.aquatic.comet.particle.transformation.rotation.RotationComponent
import gg.aquatic.comet.particle.transformation.scale.ExpressionScaleComponent
import gg.aquatic.comet.particle.transformation.scale.ScaleComponent
import gg.aquatic.comet.particle.variable.RandomsInitializerComponent
import org.joml.Vector3d
import org.joml.Vector3f
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
}

interface BaseComponentParser {
    fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component?
}

object ParticleJsonParser {
    val componentParsers: MutableMap<String, BaseComponentParser> = mutableMapOf()

    val rateComponentParsers: MutableMap<String, ComponentParser<out RateComponent>> = mutableMapOf()

    lateinit var distanceCullingParser: Pair<String, ComponentParser<DistanceCullingComponent>>
    val updateFrequencyParsers: MutableMap<String, ComponentParser<out UpdateFrequencyComponent>> = mutableMapOf()

    fun init() {
        MacrosParser

        TimedEmitterLifetimeComponent

        SteadyRateComponent
        InstantRateComponent
        ManualRateComponent

        ConstantColorComponent
        GradientColorComponent

        ParticleLifetimeExpressionComponent

        ConstantSpriteComponent
        ExpressionSpriteComponent
        FlipbookSpriteComponent

        ConstantModelComponent

        InitialExpressionPositionComponent
        ExpressionPositionComponent
        MotionPositionComponent
        AttractorPositionComponent
        SpherePositionComponent

        ExpressionScaleComponent

        ExpressionRotationComponent
        DirectionRotationComponent

        DistanceCullingComponent

        IntervalUpdateFrequencyComponent
        ManualUpdateFrequencyComponent

        ParticleInitComponent
        ParticleTickComponent
        EmitterInitComponent
        RandomsInitializerComponent

        EmitterTickComponent
        EmitterTimelineComponent
        ParticleTimelineComponent

        EmitterDeathComponent
        ParticleDeathComponent
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

        val components: MutableList<Component> = mutableListOf()
        var rateComponent: RateComponent? = null
        var distanceCullingComponent: DistanceCullingComponent? = null
        var updateFrequencyComponent: UpdateFrequencyComponent? = null
        var billboardConstraints: BillboardConstraints? = null

        for ((key, componentElement) in componentsObject.entrySet()) {
            if (key in componentParsers) {
                val component = componentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    components += component

                    continue
                }
            }

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

            if (key in updateFrequencyParsers) {
                val component = updateFrequencyParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    updateFrequencyComponent = component
                }
            }

            if (key == distanceCullingParser.first) {
                distanceCullingComponent = distanceCullingParser.second.parse(componentElement, macros)
            }
        }

        ensureNecessaryComponents(components)

        return UnrealizedEmitter(
            components,
            rateComponent ?: RateComponent.default(),
            distanceCullingComponent ?: DistanceCullingComponent.default(),
            updateFrequencyComponent ?: UpdateFrequencyComponent.default(),
            billboardConstraints ?: BillboardConstraints.CENTER
        )
    }

    private fun ensureNecessaryComponents(components: MutableList<Component>) {
        if (components.none { it is ScaleComponent }) components += ScaleComponent.default()
        if (components.none { it is RotationComponent }) components += RotationComponent.default()
        if (components.none { it is PositionComponent }) components += PositionComponent.default()
        if (components.none { it is ColorComponent }) components += ColorComponent.default()
        if (components.none { it is ParticleLifetimeComponent }) components += ParticleLifetimeComponent.default()
        if (components.none { it is EmitterLifetimeComponent }) components += EmitterLifetimeComponent.default()
        if (components.none { it is DisplayComponent }) components += DisplayComponent.default()
    }

    private fun postInit() {
        for ((_, unrealizedEmitter) in jsonUnrealizedEmitters) {
            unrealizedEmitter.components.forEach { (it as? PostInit)?.realize() }
        }
    }
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
