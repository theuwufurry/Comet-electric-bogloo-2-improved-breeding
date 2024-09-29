package com.ixume.particleemitter

import com.ixume.particleemitter.command.CustomParticleSpawnCommand
import com.ixume.particleemitter.command.ReloadParticleScriptsCommand
import com.ixume.particleemitter.emitter.lifetime.TimedEmitterLifetimeComponent
import com.ixume.particleemitter.emitter.rate.SteadyRateComponent
import com.ixume.particleemitter.emitter.shape.PointShapeComponent
import com.ixume.particleemitter.parsing.ParticleJsonParser
import com.ixume.particleemitter.particle.color.ConstantColorComponent
import com.ixume.particleemitter.particle.color.GradientColorComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeExpressionComponent
import com.ixume.particleemitter.particle.position.ExpressionPositionComponent
import com.ixume.particleemitter.particle.texture.ConstantSpriteComponent
import com.ixume.particleemitter.particle.transformation.scale.ExpressionScaleComponent
import org.bukkit.plugin.java.JavaPlugin
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory
import sun.misc.Unsafe


class ParticleEmitter : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: ParticleEmitter
        lateinit var scriptEngineFactory: NashornScriptEngineFactory

        lateinit var unsafe: Unsafe
    }

    override fun onEnable() {
        INSTANCE = this

        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        unsafe = field.get("null") as Unsafe

        scriptEngineFactory = NashornScriptEngineFactory()
        initComponentParsers()

        ParticleJsonParser.parseJsons()
        dataFolder.mkdir()

        CustomParticleSpawnCommand
        ReloadParticleScriptsCommand
    }

    private fun initComponentParsers() {
        TimedEmitterLifetimeComponent
        SteadyRateComponent
        PointShapeComponent

        ConstantColorComponent
        GradientColorComponent
        ParticleLifetimeExpressionComponent
        ConstantSpriteComponent
        ExpressionPositionComponent
        ExpressionScaleComponent
    }
}