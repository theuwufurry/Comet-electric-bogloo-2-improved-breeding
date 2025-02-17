package gg.aquatic.comet.snowstorm.deserialized.curve

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class CatmullCurve(
    val name: String,
    private val input: List<Expr>,
    private val times: List<Double>,
    private val points: List<Double>,
    private val range: List<Expr>
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("type", "catmull")
        jsonObject.addProperty("input", deserializedEffect.toJS(input))

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
        jsonObject.addProperty("range", deserializedEffect.toJS(range))

        root["macros"].asJsonObject.add("__${name.replace('.', '_')}__", jsonObject)
    }
}