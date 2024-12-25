package gg.aquatic.comet.command

import gg.aquatic.comet.ParticleEmitter
import gg.aquatic.comet.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object ExplosionParticleSpawnCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("explosionparticle")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return false

        val spawnLoc = sender.eyeLocation.add(sender.location.direction.multiply(10.0))

//        Bukkit.getScheduler().runTaskTimer(ParticleEmitter.INSTANCE, Runnable {
        ParticleJsonParser.jsonUnrealizedEmitters["explosion_init"]!!.realize(spawnLoc)
        ParticleJsonParser.jsonUnrealizedEmitters["explosion_fire"]!!.realize(spawnLoc)
        ParticleJsonParser.jsonUnrealizedEmitters["explosion_ring"]!!.realize(spawnLoc)
        ParticleJsonParser.jsonUnrealizedEmitters["explosion_projectile"]!!.realize(spawnLoc)
//        ParticleJsonParser.jsonUnrealizedEmitters["explosion_smoke"]!!.realize(spawnLoc)
//        }, 0, 10)

        return true
    }
}