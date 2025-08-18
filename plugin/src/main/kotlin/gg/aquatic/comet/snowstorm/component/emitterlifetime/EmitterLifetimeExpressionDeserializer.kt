package gg.aquatic.comet.snowstorm.component.emitterlifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asJsonObjectOrNull
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.snowstorm.Deserializer
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.emitterlifetime.EmitterLifetimeExpression
import java.io.File

object EmitterLifetimeExpressionDeserializer : Deserializer {
    override val id = "minecraft:emitter_lifetime_expression"

    override fun deserialize(jsonElement: JsonElement, file: File, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = jsonElement.asJsonObjectOrNull()
        if (jsonObject == null) {
            AbstractParticleEmitter.INSTANCE.logger.warning("Malformed $id component parsing ${file.path}")
            return
        }

        val activation = jsonObject.getExprOrNull("activation_expression")?.let { deserializedEffect.parseExpr(it, true) }
        val expiration = jsonObject.getExprOrNull("expiration_expression")?.let { deserializedEffect.parseExpr(it, true) }

        deserializedEffect.components += EmitterLifetimeExpression(activation, expiration)
    }
}