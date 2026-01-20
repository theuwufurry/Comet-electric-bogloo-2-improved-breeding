package gg.aquatic.comet.command

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.parsing.ParticleJsonParser
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object KillCommand : ICommand {
    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("Must be a player!")
            return
        }

        val types: List<String> = run type@{
            val idx = args.indexOf("--type")
            if (idx == -1 || idx == args.size - 1) return@type emptyList<String>()
            args.asList().subList(idx + 1, args.size).takeWhile { it in ParticleJsonParser.jsonUnrealizedEmitters.keys }
        }

        val emitters = if (types.isNotEmpty()) {
            GlobalTicker.emitters.filter { it.unrealizedEmitter.id in types }
        } else {
            val ems = mutableListOf<AbstractEmitter>()
            for (arg in args.asList().subList(1, args.size)) {
                val ls = GlobalTicker.emitters.filter { it.id.toString().startsWith(arg) }
                if (ls.size > 1) {
                    sender.sendMessage("'$arg' had multiple matches: ${ls.joinToString { it.id.toString() }}")
                    continue
                }

                if (ls.isEmpty()) {
                    sender.sendMessage("'$arg' had no matches!")
                    continue
                }

                ems += ls
            }

            ems
        }

        emitters.forEach { it.kill() }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}