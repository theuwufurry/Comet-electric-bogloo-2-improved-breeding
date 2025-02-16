package gg.aquatic.comet.snowstorm.component.particlelifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.particlelifetime.ParticleLifetimeExpression
import java.io.File

object ParticleLifetimeExpressionDeserializer : Deserializer {
    override val id = "minecraft:particle_lifetime_expression"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val maxLifetime =
            (jsonObject.expression("max_lifetime")?.let { deserializedEffect.parseMolang(it, true) } ?: "200")
        deserializedEffect.components += ParticleLifetimeExpression(maxLifetime)
    }
}