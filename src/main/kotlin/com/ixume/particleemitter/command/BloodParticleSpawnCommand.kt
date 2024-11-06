package com.ixume.particleemitter.command

import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.parsing.ParticleJsonParser
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object BloodParticleSpawnCommand : CommandExecutor {
    init {
        ParticleEmitter.INSTANCE.getCommand("bloodparticle")!!.setExecutor(this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return false

        val spawnLoc = sender.eyeLocation.add(sender.location.direction.multiply(4.0))
        ParticleJsonParser.jsonUnrealizedEmitters["blood_splatter"]!!.realize(spawnLoc)

        return true
    }
}