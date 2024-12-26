package gg.aquatic.particleemitter

import gg.aquatic.particleemitter.particle.lifetime.ParticleLifetimeExpressionComponent
import gg.aquatic.particleemitter.command.CustomParticleSpawnCommand
import gg.aquatic.particleemitter.command.ReloadParticleScriptsCommand
import gg.aquatic.particleemitter.emitter.lifetime.TimedEmitterLifetimeComponent
import gg.aquatic.particleemitter.emitter.rate.SteadyRateComponent
import gg.aquatic.particleemitter.emitter.shape.PointShapeComponent
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import gg.aquatic.particleemitter.particle.color.ConstantColorComponent
import gg.aquatic.particleemitter.particle.color.GradientColorComponent
import gg.aquatic.particleemitter.particle.position.ExpressionPositionComponent
import gg.aquatic.particleemitter.particle.texture.ConstantSpriteComponent
import gg.aquatic.particleemitter.particle.transformation.scale.ExpressionScaleComponent
import jdk.nashorn.api.scripting.NashornScriptEngineFactory
import org.bukkit.plugin.java.JavaPlugin
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