package gg.aquatic.comet.v2.parsing

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.v2.parsing.api.DefaultAPI
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import org.graalvm.polyglot.*
import org.joml.Vector3d
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.nameWithoutExtension

object V2Parser {
    private val vector3DMapping = HostAccess.newBuilder()
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
        .build()
    private val engine = Engine.newBuilder("js")
        .build()
    private val contexts = mutableListOf<Context>()

    val effects = mutableMapOf<String, JSEffectAPI>()

    fun load() {
        contexts.clear()
        effects.clear()

        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder
        dataFolder.mkdirs()

        val modulesFolder = File(dataFolder, "modules")
        modulesFolder.mkdirs()

        Files.walkFileTree(modulesFolder.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val context = Context.newBuilder("js")
                    .engine(engine)
                    .allowAllAccess(true)
                    .allowPolyglotAccess(PolyglotAccess.ALL)
                    .build()
                contexts += context

                val api = JSEffectAPI(context)
                effects[file.nameWithoutExtension] = api
                context.getBindings("js").putMember("effect", api)
                context.getBindings("js").putMember("comet", DefaultAPI)

                val str = file.toFile().readText()

                val source = Source.newBuilder("js", str, "${file.nameWithoutExtension}.js")
                    .cached(true)
                    .build()

                context.eval(source)

                return FileVisitResult.CONTINUE
            }
        })
    }

    fun disable() {
        contexts.forEach { it.close() }
        engine.close()
    }
}