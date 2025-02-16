package gg.aquatic.comet.snowstorm.component.variable

import com.google.gson.JsonObject
import gg.aquatic.comet.particle.variable.RandomsInitializerComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class Randoms(
    var emitter: Int = 0,
    var particle: Int = 0) : DeserializedComponent
{
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("emitter", emitter)
        jsonObject.addProperty("particle", particle)
        root["components"].asJsonObject.add(RandomsInitializerComponent.id, jsonObject)
    }
}