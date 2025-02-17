package gg.aquatic.comet.snowstorm.component

import com.google.gson.JsonObject
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.deserialized.curve.CatmullCurve

object CurveDeserializer {
    fun parse(
        fieldName: String,
        obj: JsonObject,
        deserializedParticleEffect: DeserializedParticleEffect
    ): CatmullCurve {
        assert(obj["type"].asString == "catmull")

        val nodesArray = obj["nodes"].asJsonArray
        val times: MutableList<Double> = mutableListOf()
        val values: MutableList<Double> = mutableListOf()

        for (node in nodesArray) {
            times += times.size.toDouble() / (nodesArray.size() - 1)
            values += node.asNumber.toDouble()
        }

        val horizontalRange = deserializedParticleEffect.parseExpr(obj["horizontal_range"].asString)

        return CatmullCurve(
            fieldName,
            deserializedParticleEffect.parseExpr(obj["input"].asString),
            times, values, horizontalRange
        )
    }
}