package gg.aquatic.comet.snowstorm.deserialized.emitterlifetime

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.lifetime.LoopingEmitterLifetimeComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class EmitterLifetimeLooping(
    private val activeTime: List<Expr>,
    private val sleepTime: List<Expr>
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("active_time", deserializedEffect.toJS(activeTime))
        jsonObject.addProperty("sleep_time", deserializedEffect.toJS(sleepTime))
        root["components"].asJsonObject.add(LoopingEmitterLifetimeComponent.id, jsonObject)
    }
}