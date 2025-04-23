package gg.aquatic.comet.snowstorm.deserialized.emitterlifetime

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.lifetime.TimedEmitterLifetimeComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class EmitterLifetimeOnce(
    private val activeTime: List<Expr>,
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("max_lifetime", deserializedEffect.toJS(activeTime))
        root["components"].asJsonObject.add(TimedEmitterLifetimeComponent.id, jsonObject)
    }
}