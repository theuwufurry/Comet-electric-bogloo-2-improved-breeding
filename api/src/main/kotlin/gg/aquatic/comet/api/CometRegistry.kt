package gg.aquatic.comet.api

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.YawPitch
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.parsing.AbstractParticleJsonParser
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.PreInitComponentParser
import gg.aquatic.comet.api.parsing.macro.MacroParser
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import java.util.function.Consumer
import java.util.function.Supplier

object CometRegistry {
    val macroParsers: MutableMap<String, MacroParser> = mutableMapOf()
    val rateComponentParsers: MutableMap<String, ComponentParser<out RateComponent>> = mutableMapOf()
    val componentParsers: MutableMap<String, BaseComponentParser> = mutableMapOf()
    val preInitComponentParsers: MutableMap<String, PreInitComponentParser> = mutableMapOf()

    val updateFrequencyParsers: MutableMap<String, ComponentParser<out UpdateFrequencyComponent>> = mutableMapOf()

    lateinit var jsonParser: AbstractParticleJsonParser

    fun unrealizedEmitterByID(id: String): AbstractUnrealizedEmitter? {
        return jsonParser.getUnrealizedEmitterByID(id)
    }

    fun BaseComponentParser.register() {
        componentParsers += this.id to this
    }

    fun PreInitComponentParser.register() {
        preInitComponentParsers += this.id to this
    }

    fun ComponentParser<out RateComponent>.registerRate() {
        rateComponentParsers += this.id to this
    }

    fun ComponentParser<out UpdateFrequencyComponent>.registerUpdate() {
        updateFrequencyParsers += this.id to this
    }

    /**
     * @return False if emitter not found by ID, true on success.
     */
    @JvmOverloads
    fun spawn(
        id: String,
        parent: Parent? = null,
        location: Location,
        environmentData: EnvironmentData = EnvironmentData(),
        audience: AquaticAudience = GlobalAudience(),
        mount: Mount? = null,
        yawpitchSupplier: Supplier<YawPitch>? = null,
        after: Consumer<AbstractEmitter> = Consumer {  },
    ): Boolean {
        (unrealizedEmitterByID(id) ?: return false).realize(
            parent, location, environmentData, audience, mount, yawpitchSupplier, after
        )

        return true
    }
}