package com.ixume.particleemitter.command

import com.ixume.particleemitter.GlobalTicker
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CustomParticleSpawnCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("customparticle")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player || args?.size != 1) return false
        GlobalTicker.emitters += ParticleJsonParser.jsonUnrealizedEmitters[args[0]]!!.realize(sender.location)
        return true
    }
}