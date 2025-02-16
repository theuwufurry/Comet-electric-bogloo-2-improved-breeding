package gg.aquatic.comet.snowstorm.component.emitterlifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.emitterlifetime.EmitterLifetimeLooping
import java.io.File

object EmitterLifetimeLoopingDeserializer : Deserializer {
    override val id = "minecraft:emitter_lifetime_looping"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val activeTime =
            (jsonObject.expression("active_time")?.let { deserializedEffect.parseMolang(it, true) } ?: "200")
        val sleepTime = (jsonObject.expression("sleep_time")?.let { deserializedEffect.parseMolang(it, true) } ?: "0")
        deserializedEffect.components += EmitterLifetimeLooping(activeTime, sleepTime)
    }
}