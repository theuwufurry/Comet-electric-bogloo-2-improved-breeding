package gg.aquatic.comet.snowstorm.deserialized.rate

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.rate.InstantRateComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class RateInstant(
    private val numParticles: List<Expr>
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("amount", deserializedEffect.toJS(numParticles))
        root["components"].asJsonObject.add(InstantRateComponent.id, jsonObject)
    }
}