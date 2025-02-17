package gg.aquatic.comet.snowstorm.deserialized.rate

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.rate.SteadyRateComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class RateSteady(
    private val spawnRate: List<Expr>,
    private val maxParticles: List<Expr>
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("spawn_rate", deserializedEffect.toJS(spawnRate))
        jsonObject.addProperty("max_particles", deserializedEffect.toJS(maxParticles))
        root["components"].asJsonObject.add(SteadyRateComponent.id, jsonObject)
    }
}