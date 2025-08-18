package gg.aquatic.comet.snowstorm.component.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.rate.RateInstant
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import java.io.File

object RateInstantDeserializer : Deserializer {
    override val id = "minecraft:emitter_rate_instant"
    override fun deserialize(
        jsonElement: JsonElement,
        file: File,
        deserializedEffect: DeserializedParticleEffect
    ) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val numParticles =
            jsonObject.getExprOrNull("num_particles")?.let { deserializedEffect.parseExpr(it) } ?: listOf(LiteralExpr(10))
        deserializedEffect.components += RateInstant(numParticles)
    }
}