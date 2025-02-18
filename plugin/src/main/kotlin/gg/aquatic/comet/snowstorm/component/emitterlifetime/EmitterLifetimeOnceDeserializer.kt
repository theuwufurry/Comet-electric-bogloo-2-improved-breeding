package gg.aquatic.comet.snowstorm.component.emitterlifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.emitterlifetime.EmitterLifetimeOnce
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import java.io.File

object EmitterLifetimeOnceDeserializer : Deserializer {
    override val id = "minecraft:emitter_lifetime_once"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val activeTime =
            (jsonObject.expression("active_time")?.let { deserializedEffect.parseExpr(it, true) } ?: listOf(
                LiteralExpr(
                    200.0
                )
            ))

        deserializedEffect.components += EmitterLifetimeOnce(activeTime)
    }
}