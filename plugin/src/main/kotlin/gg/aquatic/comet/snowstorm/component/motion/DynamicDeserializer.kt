package gg.aquatic.comet.snowstorm.component.motion

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.motion.MotionDynamic
import gg.aquatic.comet.snowstorm.deserialized.primitiveString
import java.io.File

object DynamicDeserializer : Deserializer {
    override val id = "minecraft:particle_motion_dynamic"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val offsetArray = jsonObject["linear_acceleration"] as? JsonArray
        if (offsetArray == null || offsetArray.size() != 3) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed linear acceleration in $id component parsing ${file.path}")
            return
        }

        val parsedArray = offsetArray.map {
            if (it.primitiveString() == "0") return@map null
            "${deserializedEffect.parseMolang(it.primitiveString())} / 256.0"
        }
        val motionDynamic = deserializedEffect.components.firstOrNull { it is MotionDynamic }
        if (motionDynamic == null) {
            deserializedEffect.components += MotionDynamic(
                Triple(
                    parsedArray[0],
                    parsedArray[1],
                    parsedArray[2]
                )
            )
        }
    }
}