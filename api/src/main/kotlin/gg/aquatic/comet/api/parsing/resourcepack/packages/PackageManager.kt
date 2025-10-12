package gg.aquatic.comet.api.parsing.resourcepack.packages

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.resourcepack.Model
import gg.aquatic.comet.api.parsing.resourcepack.Texture
import java.io.File

object PackageManager {
    var packages = listOf<Package>()

    fun compile() {
        val packages = mutableListOf<Package>()

        val dataDir = AbstractParticleEmitter.Companion.INSTANCE.dataFolder
        if (!dataDir.exists()) {
            this.packages = packages
            return
        }

        val packagesDir = File(dataDir, "packages")
        packagesDir.mkdirs()
        if (!packagesDir.isDirectory) {
            this.packages = packages
            return
        }

        val ls = packagesDir.listFiles()!!
        ls.sortBy { it.name }
        for (packageFile in ls) {
            if (!packageFile.isDirectory) {
                AbstractParticleEmitter.INSTANCE.logger.warning("Expected package ${packageFile.name} to be a directory!")
                continue
            }

            val textures = mutableListOf<Texture>()
            val models = mutableListOf<Model>()
            val effects = mutableListOf<File>()
            val js = mutableListOf<File>()

            val texturesDir = File(packageFile, "textures")
            if (texturesDir.exists() && texturesDir.isDirectory) {
                val ls = texturesDir.listFiles()!!
                for (f in ls) {
                    if (f.extension == "png") {
                        textures += Texture(packageFile.nameWithoutExtension, f)
                    }
                }
            }

            val modelsDir = File(packageFile, "models")
            if (modelsDir.exists() && modelsDir.isDirectory) {
                val ls = modelsDir.listFiles()!!
                for (f in ls) {
                    if (f.isDirectory) {
                        models += Model(packageFile.nameWithoutExtension, f)
                    }
                }
            }

            val effectsDir = File(packageFile, "effects")
            if (effectsDir.exists() && effectsDir.isDirectory) {
                val ls = effectsDir.listFiles()!!
                for (f in ls) {
                    if (f.extension == "json") {
                        effects += f
                    }
                }
            }

            val jsDir = File(packageFile, "js")
            if (jsDir.exists() && jsDir.isDirectory) {
                val ls = jsDir.listFiles()!!
                for (f in ls) {
                    if (f.extension == "js") {
                        js += f
                    }
                }
            }

            packages += Package(packageFile.nameWithoutExtension, textures, models, effects, js)
        }

        this.packages = packages
    }
}