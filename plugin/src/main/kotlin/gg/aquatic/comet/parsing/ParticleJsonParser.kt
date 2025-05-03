package gg.aquatic.comet.parsing

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.CometRegistry.componentParsers
import gg.aquatic.comet.api.CometRegistry.preInitComponentParsers
import gg.aquatic.comet.api.CometRegistry.rateComponentParsers
import gg.aquatic.comet.api.CometRegistry.register
import gg.aquatic.comet.api.CometRegistry.registerRate
import gg.aquatic.comet.api.CometRegistry.registerUpdate
import gg.aquatic.comet.api.CometRegistry.updateFrequencyParsers
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.PreInitComponent
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.AbstractParticleJsonParser
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.macro.MacrosParser
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.display.DisplayComponent
import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.action.event.EmitterDeathComponent
import gg.aquatic.comet.emitter.action.event.EmitterInitComponent
import gg.aquatic.comet.emitter.action.event.EmitterTickComponent
import gg.aquatic.comet.emitter.action.event.EmitterTimelineComponent
import gg.aquatic.comet.emitter.environment.EnvironmentDataComponent
import gg.aquatic.comet.emitter.lifetime.*
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.IntervalUpdateFrequencyComponent
import gg.aquatic.comet.emitter.optimization.updatefrequency.ManualUpdateFrequencyComponent
import gg.aquatic.comet.emitter.rate.InstantRateComponent
import gg.aquatic.comet.emitter.rate.ManualRateComponent
import gg.aquatic.comet.emitter.rate.SteadyRateComponent
import gg.aquatic.comet.particle.action.event.ParticleDeathComponent
import gg.aquatic.comet.particle.action.event.ParticleInitComponent
import gg.aquatic.comet.particle.action.event.ParticleTickComponent
import gg.aquatic.comet.particle.action.event.ParticleTimelineComponent
import gg.aquatic.comet.particle.color.ColorComponent
import gg.aquatic.comet.particle.color.ConstantColorComponent
import gg.aquatic.comet.particle.color.GradientColorComponent
import gg.aquatic.comet.particle.display.model.ConstantModelComponent
import gg.aquatic.comet.particle.display.model.FlipbookModelComponent
import gg.aquatic.comet.particle.display.sprite.ConstantSpriteComponent
import gg.aquatic.comet.particle.display.sprite.ExpressionSpriteComponent
import gg.aquatic.comet.particle.display.sprite.FlipbookSpriteComponent
import gg.aquatic.comet.particle.display.sprite.RandomSpriteComponent
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
import gg.aquatic.comet.particle.transformation.rotation.VelocityRotationComponent
import gg.aquatic.comet.particle.transformation.scale.ExpressionScaleComponent
import gg.aquatic.comet.particle.transformation.scale.ScaleComponent
import gg.aquatic.comet.particle.transformation.translation.ExpressionTranslationComponent
import gg.aquatic.comet.particle.variable.RandomsInitializerComponent
import gg.aquatic.comet.snowstorm.SnowstormTranspiler
import org.joml.Vector3d
import java.io.File
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

object ParticleJsonParser : AbstractParticleJsonParser() {
    lateinit var distanceCullingParser: Pair<String, ComponentParser<DistanceCullingComponent>>

    fun init() {
        for (parser in listOf(
            TimedEmitterLifetimeComponent,
            InfiniteEmitterLifetimeComponent,
            LoopingEmitterLifetimeComponent,
            ExpressionEmitterLifetimeComponent,

            ConstantColorComponent,
            GradientColorComponent,

            ParticleLifetimeExpressionComponent,

            ConstantSpriteComponent,
            ExpressionSpriteComponent,
            FlipbookSpriteComponent,
            RandomSpriteComponent,

            ConstantModelComponent,
            FlipbookModelComponent,

            InitialExpressionPositionComponent,
            ExpressionPositionComponent,
            MotionPositionComponent,
            AttractorPositionComponent,
            SpherePositionComponent,

            ExpressionScaleComponent,

            ExpressionTranslationComponent,

            ExpressionRotationComponent,
            DirectionRotationComponent,
            VelocityRotationComponent,

            ParticleInitComponent,
            ParticleTickComponent,
            EmitterInitComponent,
            RandomsInitializerComponent,

            EmitterTickComponent,
            EmitterTimelineComponent,
            ParticleTimelineComponent,

            EmitterDeathComponent,
            ParticleDeathComponent
        )) {
            parser.register()
        }

        for (parser in listOf(
            EnvironmentDataComponent
        )) {
            parser.register()
        }

        distanceCullingParser = DistanceCullingComponent.id to DistanceCullingComponent

        for (parser in listOf(
            SteadyRateComponent,
            InstantRateComponent,
            ManualRateComponent,
        )) {
            parser.registerRate()
        }

        for (parser in listOf(
            IntervalUpdateFrequencyComponent,
            ManualUpdateFrequencyComponent
        )) {
            parser.registerUpdate()
        }
    }

    lateinit var jsonUnrealizedEmitters: MutableMap<String, UnrealizedEmitter>
        private set

    override fun parseJsons() {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder
        dataFolder.mkdirs()

        SnowstormTranspiler.load()

        val effectsFolder = File(dataFolder.path + "/effects/")
        effectsFolder.mkdirs()
        val effects = recursivelyFindJsons(effectsFolder)

        val unrealizedEmitters: MutableMap<String, UnrealizedEmitter> = mutableMapOf()

        GlobalTicker.killInstances()

        for (file in effects) {
            val rootObject = JsonParser.parseReader(FileReader(file)).asJsonObject

            val emitter = parseComponents(rootObject, file.nameWithoutExtension)
            emitter?.run {
                unrealizedEmitters += file.nameWithoutExtension to emitter
            } ?: run {
                AbstractParticleEmitter.INSTANCE.logger.warning("""Field "components" is null in ${file.name}!""")
            }
        }

        jsonUnrealizedEmitters = unrealizedEmitters

        postInit()
    }

    override fun getUnrealizedEmitterByID(id: String): AbstractUnrealizedEmitter? {
        return jsonUnrealizedEmitters[id]
    }

    fun recursivelyFindJsons(dir: File): Set<File> {
        val files: MutableSet<File> = mutableSetOf()
        for (file in dir.listFiles()!!) {
            if (file.isDirectory) {
                files += recursivelyFindJsons(file)
                continue
            }

            if (file.extension == "json") files += file
        }

        return files
    }

    private fun parseComponents(rootObject: JsonObject, id: String): UnrealizedEmitter? {
        val macros: Map<String, Macro>? = rootObject.getAsJsonObject("macros")?.let { MacrosParser.parseMacros(it) }

        val isListed = let {
            val elem = rootObject["listed"] ?: return@let true
            val primitive = if (elem.isJsonPrimitive) elem.asJsonPrimitive else return@let true
            return@let if (primitive.isBoolean) primitive.asBoolean else true
        }

        val componentsObject: JsonObject = rootObject.getAsJsonObject("components") ?: return null

        val preInitComponents: MutableList<PreInitComponent> = mutableListOf()
        val components: MutableList<Component> = mutableListOf()
        var rateComponent: RateComponent? = null
        var distanceCullingComponent: DistanceCullingComponent? = null
        var updateFrequencyComponent: UpdateFrequencyComponent? = null
        var billboardConstraints: BillboardConstraints? = null
        var forwardVector = Vector3d(0.0, 0.0, 1.0)

        for ((key, componentElement) in componentsObject.entrySet()) {
            if (key in componentParsers) {
                val component = componentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    components += component

                    continue
                }
            }

            if (key in preInitComponentParsers) {
                val component = preInitComponentParsers[key]!!.parse(componentElement, macros)
                if (component != null) {
                    preInitComponents += component

                    continue
                }
            }

            if (key == "display_type") {
                billboardConstraints = when (componentElement.asString) {
                    "fixed" -> BillboardConstraints.FIXED
                    "center" -> BillboardConstraints.CENTER
                    "vertical" -> BillboardConstraints.VERTICAL
                    "horizontal" -> BillboardConstraints.HORIZONTAL
                    else -> BillboardConstraints.CENTER
                }
            }

            if (key == "forward_vector") {
                val obj = componentElement.asJsonObject
                forwardVector = Vector3d(
                    obj["x"].asNumber.toDouble(),
                    obj["y"].asNumber.toDouble(),
                    obj["z"].asNumber.toDouble(),
                ).normalize()
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
            id,
            preInitComponents,
            components,
            rateComponent ?: SteadyRateComponent.default(),
            distanceCullingComponent ?: DistanceCullingComponent.default(),
            updateFrequencyComponent ?: IntervalUpdateFrequencyComponent.default(),
            billboardConstraints ?: BillboardConstraints.CENTER,
            forwardVector,
            isListed
        )
    }

    private fun ensureNecessaryComponents(components: MutableList<Component>) {
        ConstantSpriteComponent
        if (components.none { it is ScaleComponent }) components += ScaleComponent.default()
        if (components.none { it is RotationComponent }) components += RotationComponent.default()
        if (components.none { it is PositionComponent }) components += PositionComponent.default()
        if (components.none { it is ColorComponent }) components += ColorComponent.default()
        if (components.none { it is ParticleLifetimeComponent }) components += ParticleLifetimeComponent.default()
        if (components.none { it is EmitterLifetimeComponent }) components += EmitterLifetimeComponent.default()
        if (components.none { it is DisplayComponent }) components += ConstantSpriteComponent.default()
    }

    private fun postInit() {
        for ((_, unrealizedEmitter) in jsonUnrealizedEmitters) {
            unrealizedEmitter.components.forEach { (it as? PostInit)?.realize(unrealizedEmitter) }
        }
    }

    fun onDisable() {
        jsonUnrealizedEmitters.clear()
    }
}
