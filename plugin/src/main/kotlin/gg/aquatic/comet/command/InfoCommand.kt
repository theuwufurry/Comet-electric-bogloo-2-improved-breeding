package gg.aquatic.comet.command

import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.roundToInt

object InfoCommand : ICommand {
    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }

        if (sender !is Player) {
            sender.sendMessage("Must be a player!")
            return
        }

        val verbose = args.any { it == "--verbose" }

        val types: List<String> = run type@{
            val idx = args.indexOf("--type")
            if (idx == -1 || idx == args.size - 1) return@type emptyList<String>()
            args.asList().subList(idx + 1, args.size).takeWhile { it in ParticleJsonParser.jsonUnrealizedEmitters.keys }
        }

        val emitters = GlobalTicker.emitters
            .filter { (types.isNotEmpty() && it.unrealizedEmitter.id in types) || (types.isEmpty()) }

        val strBuilder = StringBuilder()

        strBuilder.append("=== INFO ===\n")
        strBuilder.append("World '${sender.world.name}' has ${emitters.size} emitters!\n")
        strBuilder.append("occurrences <-> type\n")

        strBuilder.append(
            emitters
                .groupingBy { it.unrealizedEmitter.id }
                .eachCount()
                .map { (str, count) -> str to count }
                .joinToString(separator = "") { (str, count) -> " - '$str' : $count\n" }
        )

        if (verbose) {
            strBuilder.append("type <-> id\n")
            for (emitter in emitters) {
                strBuilder.append(" - ${emitter.unrealizedEmitter.id} : ${emitter.pose.location.x.roundToInt()}, ${emitter.pose.location.y.roundToInt()}, ${emitter.pose.location.z.roundToInt()} : ${emitter.id} \n")
            }
        }

        sender.sendMessage(strBuilder.toString())
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}
