package gg.aquatic.comet.snowstorm.deserialized.curve

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent

class CatmullCurve(
    private val name: String,
    private val input: String,
    private val times: List<Double>,
    private val points: List<Double>,
    private val range: String
) : DeserializedComponent {
    override fun serialize(root: JsonObject) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", "catmull")
        jsonObject.addProperty("input", input)

        val xArray = JsonArray()
        for (time in times) {
            xArray.add(time)
        }

        jsonObject.add("x", xArray)

        val yArray = JsonArray()
        for (point in points) {
            yArray.add(point)
        }

        jsonObject.add("y", yArray)
        jsonObject.addProperty("range", range)

        root["macros"].asJsonObject.add(name, jsonObject)
    }
}