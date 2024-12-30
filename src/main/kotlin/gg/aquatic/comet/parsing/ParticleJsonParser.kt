package gg.aquatic.comet.parsing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.emitter.EmitterTickersHolder
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.bundle.BundledEmitterComponent
import gg.aquatic.comet.emitter.lifetime.EmitterLifetimeComponent
import gg.aquatic.comet.emitter.lifetime.TimedEmitterLifetimeComponent
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.IntervalUpdateFrequencyComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.emitter.rate.RateComponent
import gg.aquatic.comet.emitter.rate.SteadyRateComponent
import gg.aquatic.comet.emitter.recursive.RecursiveEmitterComponent
import gg.aquatic.comet.emitter.shape.PointShapeComponent
import gg.aquatic.comet.emitter.shape.ShapeComponent
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.parsing.macro.MacrosParser
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
import gg.aquatic.comet.particle.position.ExpressionPositionComponent
import gg.aquatic.comet.particle.position.MotionPositionComponent
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.particle.transformation.rotation.ExpressionRotationComponent
import gg.aquatic.comet.particle.transformation.rotation.RotationComponent
import gg.aquatic.comet.particle.transformation.scale.ExpressionScaleComponent
import gg.aquatic.comet.particle.transformation.scale.ScaleComponent
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

    lateinit var distanceCullingParser: Pair<String, ComponentParser<DistanceCullingComponent>>
    val updateFrequencyParsers: MutableMap<String, ComponentParser<out UpdateFrequencyComponent>> = mutableMapOf()

    lateinit var recursiveEmitterComponentParser: Pair<String, ComponentParser<RecursiveEmitterComponent>>
    lateinit var bundledEmitterComponentParser: Pair<String, ComponentParser<BundledEmitterComponent>>

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

        ExpressionPositionComponent
        MotionPositionComponent

        ExpressionScaleComponent

        ExpressionRotationComponent

        DistanceCullingComponent

        IntervalUpdateFrequencyComponent

        RecursiveEmitterComponent

        BundledEmitterComponent
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
        var distanceCullingComponent: DistanceCullingComponent? = null
        var updateFrequencyComponent: UpdateFrequencyComponent? = null
        var billboardConstraints: BillboardConstraints? = null

        var recursiveEmitterComponent: RecursiveEmitterComponent? = null
        var bundledEmitterComponent: BundledEmitterComponent? = null

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

            if (key in updateFrequencyParsers) {
                val component = updateFrequencyParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    updateFrequencyComponent = component
                }
            }

            if (key == distanceCullingParser.first) {
                distanceCullingComponent = distanceCullingParser.second.parse(componentElement, macros)
            }

            if (key == recursiveEmitterComponentParser.first) {
                recursiveEmitterComponent = recursiveEmitterComponentParser.second.parse(componentElement, macros)
            }

            if (key == bundledEmitterComponentParser.first) {
                bundledEmitterComponent = bundledEmitterComponentParser.second.parse(componentElement, macros)
            }
        }

        return UnrealizedEmitter(
            rateComponent ?: RateComponent.default(),
            particleLifetimeComponent ?: ParticleLifetimeComponent.default(),
            displayComponent ?: DisplayComponent.default(),
            shapeComponent ?: ShapeComponent.default(),
            colorComponent ?: ColorComponent.default(),
            emitterLifetimeComponent ?: EmitterLifetimeComponent.default(),
            positionComponent ?: PositionComponent.default(),
            scaleComponent ?: ScaleComponent.default(),
            rotationComponent ?: RotationComponent.default(),
            recursiveEmitterComponent,
            distanceCullingComponent ?: DistanceCullingComponent.default(),
            updateFrequencyComponent ?: UpdateFrequencyComponent.default(),
            bundledEmitterComponent,
            billboardConstraints ?: BillboardConstraints.CENTER
        )
    }

    private fun postInit() {
        for ((_, unrealizedEmitter) in jsonUnrealizedEmitters) {
            unrealizedEmitter.recursiveEmitterComponent?.realize()
            unrealizedEmitter.bundledEmitterComponent?.realize()
            (unrealizedEmitter.positionComponent as? PostInit)?.realize()
        }
    }
}

