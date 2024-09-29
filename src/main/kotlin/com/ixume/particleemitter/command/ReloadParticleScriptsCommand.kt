package com.ixume.particleemitter.command

import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

object ReloadParticleScriptsCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("reloadparticles")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        ParticleJsonParser.parseJsons()
        return true
    }
}