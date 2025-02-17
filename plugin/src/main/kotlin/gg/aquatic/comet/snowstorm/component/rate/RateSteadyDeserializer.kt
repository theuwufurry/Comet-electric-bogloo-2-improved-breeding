package gg.aquatic.comet.snowstorm.component.rate

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.rate.RateSteady
import gg.aquatic.comet.snowstorm.transpilation.expression.LiteralExpr
import java.io.File

object RateSteadyDeserializer : Deserializer {
    override val id = "minecraft:emitter_rate_steady"
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

        val spawnRate = jsonObject.expression("spawn_rate")?.let { deserializedEffect.parseExpr(it) } ?: listOf(LiteralExpr(1))
        val maxParticles =
            jsonObject.expression("max_particles")?.let { deserializedEffect.parseExpr(it) } ?: listOf(LiteralExpr(50))

        deserializedEffect.components += RateSteady(spawnRate, maxParticles)
    }
}