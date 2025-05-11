package gg.aquatic.comet.emitter


import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.PreInitComponent
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.emitter.impl.Emitter
import gg.aquatic.comet.emitter.impl.OptimizedEmitter
import gg.aquatic.comet.emitter.optimization.VirtualEmitter
import gg.aquatic.comet.emitter.optimization.VirtualRuntime
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.audience.GlobalAudience
import org.bukkit.Location
import org.joml.Vector3d
import java.util.*
import java.util.function.Consumer

data class UnrealizedEmitter(
    override val id: String,
    override val preInitComponents: List<PreInitComponent>,
    override val components: List<Component>,
    val rateComponent: RateComponent,
    val distanceCullingComponent: DistanceCullingComponent,
    val updateFrequencyComponent: UpdateFrequencyComponent,
    override val billboardConstraints: BillboardConstraints,
    override val forwardVector: Vector3d,
    val isListed: Boolean,
    override val isDoubleSided: Boolean,
    override val lookahead: Int,
) : AbstractUnrealizedEmitter() {
    override fun realize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        after: Consumer<AbstractEmitter>,
    ) {
        val initialization = {
            val emitterData = EmitterData(UUID.randomUUID())
            emitterData.world = location.world
            emitterData.location = location
            preInitComponents.forEach { it.init(emitterData, environmentData) }
            emitterData.variable.putAll(environmentData.data)

            val optimized = checkOptimized(environmentData)
            val emitter: AbstractEmitter = if (optimized) OptimizedEmitter(
                parent,
                components,
                rateComponent,
                distanceCullingComponent,
                billboardConstraints, location, emitterData, this, forwardVector, environmentData, audience, false
            ) else Emitter(
                parent,
                components,
                rateComponent,
                distanceCullingComponent,
                updateFrequencyComponent,
                billboardConstraints, location, emitterData, this, forwardVector, environmentData, audience,
            )

            after.accept(emitter)

            emitter
        }

        GlobalTicker.addInitialization(initialization)
    }

    private fun checkOptimized(data: EnvironmentData): Boolean {
        return !(data.data["optimize"] != null && data.data["optimize"] == false)
    }

    override fun realize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        after: Consumer<AbstractEmitter>,
    ) {
        realize(parent, location, environmentData, GlobalAudience(), after)
    }

    override fun internalRealize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID
    ): AbstractEmitter {
        val emitterData = EmitterData(uuid)
        emitterData.world = location.world
        emitterData.location = location
        preInitComponents.forEach { it.init(emitterData, environmentData) }
        emitterData.variable.putAll(environmentData.data)

        val optimized = checkOptimized(environmentData)

        val e = if (optimized) OptimizedEmitter(
            parent,
            components,
            rateComponent,
            distanceCullingComponent,
            billboardConstraints,
            location,
            emitterData,
            this,
            forwardVector,
            environmentData,
            audience,
            true,
            seed = random.kotlinRandom.nextInt(),
        ) else Emitter(
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
            seed = random.kotlinRandom.nextInt(),
        )

        GlobalTicker.addEmitter(e)

        return e
    }

    fun virtualRealize(
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        random: DeterministicRandom,
        runtime: VirtualRuntime,
        uuid: UUID,
        timeOffset: Int,
    ) {
        val emitterData = EmitterData(uuid)
        emitterData.world = location.world
        emitterData.location = location
        preInitComponents.forEach { it.init(emitterData, environmentData) }
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
                seed = random.kotlinRandom.nextInt(),
                timeOffset
            )
        )
    }
}