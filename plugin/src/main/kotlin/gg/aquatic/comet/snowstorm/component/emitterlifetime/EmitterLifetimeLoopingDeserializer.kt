package gg.aquatic.comet.snowstorm.component.emitterlifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.emitterlifetime.EmitterLifetimeLooping
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
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
            (jsonObject.getExprOrNull("active_time")?.let { deserializedEffect.parseExpr(it, true) } ?: listOf(
                LiteralExpr(
                    200.0
                )
            ))
        val sleepTime =
            (jsonObject.getExprOrNull("sleep_time")?.let { deserializedEffect.parseExpr(it, true) } ?: listOf(
                LiteralExpr(
                    0.0
                )
            ))
        deserializedEffect.components += EmitterLifetimeLooping(activeTime, sleepTime)
    }
}