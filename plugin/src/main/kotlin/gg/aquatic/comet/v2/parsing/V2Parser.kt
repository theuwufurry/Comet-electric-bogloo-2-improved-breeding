package gg.aquatic.comet.v2.parsing

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.v2.parsing.api.udar.PhysicsBodyWrapper
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
                    value.getMemberOrNull("x")?.asDouble() ?: 0.0,
                    value.getMemberOrNull("y")?.asDouble() ?: 0.0,
                    value.getMemberOrNull("z")?.asDouble() ?: 0.0,
                )
            }
        )
        .targetTypeMapping(
            Value::class.java,
            Vector3f::class.java,
            { value -> value.hasMembers() },
            { value ->
                Vector3f(
                    value.getMemberOrNull("x")?.asFloat() ?: 0f,
                    value.getMemberOrNull("y")?.asFloat() ?: 0f,
                    value.getMemberOrNull("z")?.asFloat() ?: 0f,
                )
            }
        )
        .targetTypeMapping(
            Value::class.java,
            PhysicsBodyWrapper::class.java,
            { value -> value.isProxyObject },
            { value -> value.asProxyObject() }
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

fun Value.getMemberOrNull(identifier: String): Value? {
    return if (hasMember(identifier)) getMember(identifier) else null
}