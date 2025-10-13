package gg.aquatic.comet.v2.parsing

import gg.aquatic.comet.api.parsing.resourcepack.packages.PackageManager
import gg.aquatic.comet.v2.parsing.context.JSContextProvider
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value

object V2Parser {
    val effects = mutableMapOf<String, Source>()

    fun load() {
        JSContextProvider.reload()
        effects.clear()

        for (pack in PackageManager.packages) {
            for (file in pack.js) {
                val name = "${pack.name}.${file.nameWithoutExtension}"
                val str = file.readText()

                val source = Source.newBuilder("js", str, "$name.js")
                    .cached(true)
                    .build()

                effects[name] = source
            }
        }
    }

    fun disable() {
        JSContextProvider.close()
    }
}

fun Value.getMemberOrNull(identifier: String): Value? {
    return if (hasMember(identifier)) getMember(identifier) else null
}