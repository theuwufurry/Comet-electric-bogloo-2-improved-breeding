package gg.aquatic.comet.snowstorm.deserialized.emitterlifetime

import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.lifetime.LoopingEmitterLifetimeComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class EmitterLifetimeLooping(
    private val activeTime: String,
    private val sleepTime: String
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("active_time", activeTime)
        jsonObject.addProperty("sleep_time", sleepTime)
        root["components"].asJsonObject.add(LoopingEmitterLifetimeComponent.id, jsonObject)
    }
}