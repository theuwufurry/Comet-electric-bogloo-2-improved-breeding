package gg.aquatic.comet.hook

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.animation.property.IAnimationProperty
import com.ticxo.modelengine.api.animation.script.ScriptReader
import gg.aquatic.comet.parsing.ParticleJsonParser

object ModelEngineHook : IHook {
    override fun initialize() {
        ModelEngineAPI.getAPI().scriptReaderRegistry.register(
            "comet", ParticleScriptReader
        )
    }

    private object ParticleScriptReader : ScriptReader {
        override fun read(property: IAnimationProperty, script: String) {
            val args = script.split(" ")
            var emitterId: String? = null
            var bone: String? = null
            for (arg in args) {
                if (!arg.startsWith("-")) {
                    emitterId = arg
                    continue
                }
                if (arg.startsWith("-bone:")) {
                    bone = arg.replace("-bone:", "")
                    continue
                }
            }
            bone ?: return
            val emitter = ParticleJsonParser.jsonUnrealizedEmitters[emitterId] ?: return

            property.model.getBone(bone).ifPresent { modelBone ->
                emitter.realize(modelBone.location)
            }

        }
    }
}