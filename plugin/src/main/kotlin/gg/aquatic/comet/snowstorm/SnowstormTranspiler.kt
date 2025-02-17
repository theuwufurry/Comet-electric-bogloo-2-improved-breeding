package gg.aquatic.comet.snowstorm

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.ParticleJsonParser.recursivelyFindJsons
import gg.aquatic.comet.snowstorm.component.CurveDeserializer
import gg.aquatic.comet.snowstorm.component.appearance.BillboardDeserializer
import gg.aquatic.comet.snowstorm.component.appearance.TintingDeserializer
import gg.aquatic.comet.snowstorm.component.emitterlifetime.EmitterLifetimeExpressionDeserializer
import gg.aquatic.comet.snowstorm.component.emitterlifetime.EmitterLifetimeLoopingDeserializer
import gg.aquatic.comet.snowstorm.component.emitterlifetime.EmitterLifetimeOnceDeserializer
import gg.aquatic.comet.snowstorm.component.motion.DynamicDeserializer
import gg.aquatic.comet.snowstorm.component.motion.InitialSpeedDeserializer
import gg.aquatic.comet.snowstorm.component.particlelifetime.ParticleLifetimeExpressionDeserializer
import gg.aquatic.comet.snowstorm.component.rate.RateInstantDeserializer
import gg.aquatic.comet.snowstorm.component.rate.RateSteadyDeserializer
import gg.aquatic.comet.snowstorm.component.shape.EmitterShapePointDeserializer
import gg.aquatic.comet.snowstorm.component.shape.EmitterSphereShapeDeserializer
import gg.aquatic.comet.snowstorm.component.variable.EmitterInitDeserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.emitterlifetime.EmitterLifetimeOnce
import java.io.File
import java.io.FileReader

/*
gather all components in snowstorm file and convert them into intermediate data structure
intermediate should store expressions as javascript
 */

object SnowstormTranspiler {
    private val deserializers: MutableMap<String, Deserializer> = mutableMapOf()

    fun load() {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder
        if (!dataFolder.exists()) return

        val snowstormEffectsFolder = File(dataFolder.path + "/snowstorm/")
        snowstormEffectsFolder.mkdirs()
        val transpiledEffectsFolder = File(dataFolder.path + "/effects/transpiled/")
        transpiledEffectsFolder.mkdirs()
        val effects = recursivelyFindJsons(snowstormEffectsFolder)

        initDeserializers()

        for (file in effects) {
            transpileEffect(file, transpiledEffectsFolder)
        }
    }

    private fun initDeserializers() {
        listOf(
            RateSteadyDeserializer,
            RateInstantDeserializer,

            EmitterLifetimeLoopingDeserializer,
            EmitterLifetimeOnceDeserializer,
            EmitterLifetimeExpressionDeserializer,

            ParticleLifetimeExpressionDeserializer,

            EmitterShapePointDeserializer,
            EmitterSphereShapeDeserializer,

            BillboardDeserializer,

            TintingDeserializer,

            EmitterInitDeserializer,

            DynamicDeserializer,
            InitialSpeedDeserializer
        ).forEach { it.register() }
    }

    private fun Deserializer.register() {
        deserializers += id to this
    }

    private fun transpileEffect(snowstormFile: File, transpiledFile: File): File? {
        val rootObject = JsonParser.parseReader(FileReader(snowstormFile)).asJsonObjectOrNull()
        if (rootObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("${snowstormFile.path} should start with a json object!")
            return null
        }

        val particleEffectObject = rootObject.asJsonObjectOrNull("particle_effect")
        if (particleEffectObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("${snowstormFile.path} should have particle_effect!")
            return null
        }

        val componentsObject = particleEffectObject.asJsonObjectOrNull("components")
        if (componentsObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("${snowstormFile.path} should have components!")
            return null
        }

        val deserialized = DeserializedParticleEffect()

        if (particleEffectObject.has("curves")) {
            val curvesObject = particleEffectObject.getAsJsonObject("curves")
            for ((name, obj) in curvesObject.entrySet()) {
                obj as JsonObject
                val curve = CurveDeserializer.parse(name, obj, deserialized)
                deserialized.curves += curve
            }
        }

        for ((name, element) in componentsObject.entrySet()) {
            val deserializer = deserializers[name] ?: continue
            deserializer.deserialize(element, snowstormFile, deserialized)
        }

        deserialized.serializeInto(File(transpiledFile.path + "/" + snowstormFile.name))
        return transpiledFile
    }
}