package gg.aquatic.comet.emitter


import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.emitter.optimization.VirtualEmitter
import gg.aquatic.comet.emitter.optimization.VirtualRuntime
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import org.joml.Vector3d
import java.util.*
import kotlin.system.measureNanoTime

data class UnrealizedEmitter(
    override val id: String,
    override val components: List<Component>,
    val rateComponent: RateComponent,
    val distanceCullingComponent: DistanceCullingComponent,
    val updateFrequencyComponent: UpdateFrequencyComponent,
    override val billboardConstraints: BillboardConstraints,
    override val forwardVector: Vector3d,
    val isListed: Boolean
) : AbstractUnrealizedEmitter() {
    override fun realize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        after: (AbstractEmitter) -> Unit,
    ) {
        val emitterData = EmitterData(UUID.randomUUID())
        emitterData.world = location.world
        emitterData.location = location
        emitterData.variable.putAll(environmentData.data)

        val initialization = {
            val emitter: Emitter
            val t = measureNanoTime {
                emitter = Emitter(
                    parent,
                    components,
                    rateComponent,
                    distanceCullingComponent,
                    updateFrequencyComponent,
                    billboardConstraints, location, emitterData, this, forwardVector, environmentData, audience, false
                )
            }

            println("$id took ${t.toDouble() / 1_000_000.0}ms")

            after(emitter)

            emitter
        }

        GlobalTicker.addInitialization(initialization)
    }

    override fun realize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        after: (AbstractEmitter) -> Unit,
    ) {
        realize(parent, location, environmentData, GlobalAudience(), after)
    }

    override fun internalRealize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom,
    ): AbstractEmitter {
        val emitterData = EmitterData(random.uuid())
        emitterData.world = location.world
        emitterData.location = location
        emitterData.variable.putAll(environmentData.data)

        return Emitter(
            parent,
            components,
            rateComponent,
            distanceCullingComponent,
            updateFrequencyComponent,
            billboardConstraints,
            location,
            emitterData,
            this,
            forwardVector,
            environmentData,
            audience,
            true,
            seed = random.kotlinRandom.nextInt()
        ).also {
            GlobalTicker.addEmitter(it)
        }
    }

    fun virtualRealize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        random: DeterministicRandom,
        runtime: VirtualRuntime,
    ) {
        val emitterData = EmitterData(random.uuid())
        emitterData.world = location.world
        emitterData.location = location
        emitterData.variable.putAll(environmentData.data)

        runtime.addEmitter(
            VirtualEmitter(
                parent = parent,
                unrealizedEmitter = this,
                runtime = runtime,
                rateComponent = rateComponent,
                components = components,
                billboardConstraints = billboardConstraints,
                backerLocation = location,
                backerEmitterData = emitterData,
                backerForwardVector = forwardVector,
                backerEnvironmentData = environmentData,
                seed = random.kotlinRandom.nextInt()
            )
        )
    }
}