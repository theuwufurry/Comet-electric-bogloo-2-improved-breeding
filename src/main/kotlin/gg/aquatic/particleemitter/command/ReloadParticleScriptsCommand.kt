package gg.aquatic.particleemitter.command

import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

object ReloadParticleScriptsCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("reloadparticles")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        ParticleJsonParser.parseJsons()
        return true
    }
}