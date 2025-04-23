package gg.aquatic.comet.snowstorm.deserialized.variable

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.action.event.EmitterInitComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedComponent
import gg.aquatic.comet.snowstorm.deserialized.DeserializedParticleEffect
import gg.aquatic.comet.snowstorm.transpilation.expression.Expr

class EmitterInit(
    private val expr: List<Expr>
) : DeserializedComponent {
    override fun serialize(root: JsonObject, deserializedEffect: DeserializedParticleEffect) {
        val arr = JsonArray()
        val jsObj = JsonObject()
        val exprsArr = JsonArray()

        exprsArr.add(deserializedEffect.toJS(expr))
        jsObj.add("expressions", exprsArr)
        arr.add(jsObj)
        root["components"].asJsonObject.add(EmitterInitComponent.id, arr)
    }
}