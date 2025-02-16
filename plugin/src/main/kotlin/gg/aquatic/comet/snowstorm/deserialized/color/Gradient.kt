package gg.aquatic.comet.snowstorm.deserialized.color

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.aquatic.comet.particle.color.GradientColorComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class Gradient(
    private val interpolant: String,
    private val data: List<Pair<Double, String>>
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()

        jsonObject.addProperty("interpolant", interpolant)

        val dataArr = JsonArray()

        for ((time, colorStr) in data) {
            val datumObj = JsonObject()
            datumObj.addProperty("index", time)
            datumObj.addProperty("color", colorStr)

            dataArr.add(datumObj)
        }

        jsonObject.add("data", dataArr)

        root["components"].asJsonObject.add(GradientColorComponent.id, jsonObject)
    }
}