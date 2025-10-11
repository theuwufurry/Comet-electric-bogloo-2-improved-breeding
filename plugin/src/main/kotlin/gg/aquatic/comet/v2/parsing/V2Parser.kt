package gg.aquatic.comet.v2.parsing

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.emitter.TemporalEffect
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.joml.Vector3d
import org.joml.Vector3f
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.nameWithoutExtension

object V2Parser {
    val hostAccess: HostAccess = HostAccess.newBuilder()
        .targetTypeMapping(
            Value::class.java,
            Vector3d::class.java,
            { value -> value.hasMembers() },
            { value ->
                Vector3d(
                    value.getMember("x").asDouble(),
                    value.getMember("y").asDouble(),
                    value.getMember("z").asDouble()
                )
            }
        )
        .targetTypeMapping(
            Value::class.java,
            Vector3f::class.java,
            { value -> value.hasMembers() },
            { value ->
                Vector3f(
                    value.getMember("x").asFloat(),
                    value.getMember("y").asFloat(),
                    value.getMember("z").asFloat(),
                )
            }
        )
        .allowPublicAccess(true)
        .build()
    val engine: Engine = Engine.newBuilder("js")
        .build()
    val effects = mutableMapOf<String, Source>()

    fun load() {
        effects.clear()

        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder
        dataFolder.mkdirs()

        val modulesFolder = File(dataFolder, "modules")
        modulesFolder.mkdirs()

        Files.walkFileTree(modulesFolder.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val str = file.toFile().readText()

                val source = Source.newBuilder("js", str, "${file.nameWithoutExtension}.js")
                    .cached(true)
                    .build()

                effects[file.nameWithoutExtension] = source

                return FileVisitResult.CONTINUE
            }
        })
    }

    fun disable() {
        engine.close()
    }
}