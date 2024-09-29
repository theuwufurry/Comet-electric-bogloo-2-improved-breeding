package gg.aquatic.particleemitter.command

import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CustomParticleSpawnCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("customparticle")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player || args.size != 1) return false
        ParticleJsonParser.jsonUnrealizedEmitters[args[0]]!!.realize(sender.location)
        return true
    }
}