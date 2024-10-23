package com.ixume.particleemitter.parsing

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.ixume.particleemitter.ParticleEmitter
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object ResourcepackCreator {
    private const val RP_NAME = "Particle Creator"
    private const val RP_IMAGE = "pack.png"
    private const val RP_META = "pack.mcmeta"
    private const val NAMESPACE = "particlecreator"

    fun genPack() {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder
        if (!dataFolder.exists()) return

        val texturesFolder = File(dataFolder.path + "/textures")
        if (!texturesFolder.exists()) return
        val images: MutableList<File> = mutableListOf()

        val oldRPFolder = File(dataFolder.path + "/output/$RP_NAME")
        if (oldRPFolder.exists()) oldRPFolder.deleteRecursively()

        for (file in texturesFolder.listFiles()!!) {
            if (file.extension != "png") continue
            images += file
        }

        genPackFiles(images)
    }

    private fun genPackFiles(images: List<File>) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val packImage = File(dataFolder.path + "/output/$RP_NAME/pack.png")
        packImage.parentFile.mkdirs()
        packImage.writeBytes(ParticleEmitter.INSTANCE.getResource(RP_IMAGE)!!.readAllBytes())

        val packMeta = File(dataFolder.path + "/output/$RP_NAME/pack.mcmeta")
        packMeta.writeBytes(ParticleEmitter.INSTANCE.getResource(RP_META)!!.readAllBytes())

        for (i in 0..255) {
            genTextures(images, i)
            genFont(images, i)
            genLang(images, i)
        }
    }

    private fun genTextures(images: List<File>, alpha: Int) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val texturesFolder = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE.$alpha/textures/font")
        texturesFolder.mkdirs()

        for (image in images) {
            val rawImage = ImageIO.read(image)
            val alphaImage = BufferedImage(rawImage.width, rawImage.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = alphaImage.createGraphics()
            graphics.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255F)
            graphics.drawImage(rawImage, 0, 0, null)
            graphics.dispose()
            val imageFile = File(texturesFolder.path + "/" + image.name)
            ImageIO.write(alphaImage, "png", imageFile)
        }
    }

    private fun genFont(images: List<File>, alpha: Int) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val font = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE.$alpha/font/default.json")
        font.parentFile.mkdirs()

        val providers = JsonArray()
        var index = '\uE000'

        for (image in images) {
            val bufferedImage: BufferedImage = ImageIO.read(image) ?: continue
            val width = bufferedImage.width
            val height = bufferedImage.height
            if (width % height != 0) {
                continue
            }

            val count = width / height
            val chars = StringBuilder()

            for (i in 0 until count) {
                chars.append(index.toString())
                index += 1
            }

            val jsonObject = JsonObject()

            val charArray = JsonArray()
            charArray.add(chars.toString())

            jsonObject.addProperty("type", "bitmap")
            jsonObject.addProperty("file", "$NAMESPACE.$alpha:font/${image.name}")
            jsonObject.addProperty("ascent", 1)
            jsonObject.addProperty("height", 8)
            jsonObject.add("chars", charArray)

            providers.add(jsonObject)
        }

        val completeObject = JsonObject()
        completeObject.add("providers", providers)

        val gson = GsonBuilder().setPrettyPrinting().create()
        font.writeText(gson.toJson(completeObject))
    }

    private fun genLang(images: List<File>, alpha: Int) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val lang = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE.$alpha/lang/en_us.json")
        lang.parentFile.mkdirs()

        val jsonObject = JsonObject()
        var index = '\uE000'

        for(image in images) {
            val bufferedImage: BufferedImage = ImageIO.read(image) ?: continue
            val width = bufferedImage.width
            val height = bufferedImage.height
            if (width % height != 0) {
                println("height not multiple of width")
                continue
            }

            val count = width / height

            if (count > 1) {
                for (i in 0 until count) {
                    jsonObject.addProperty(image.nameWithoutExtension + "." + i, index.toString())
                    index += 1
                }
            } else {
                jsonObject.addProperty(image.nameWithoutExtension, index.toString())
                index += 1
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        lang.writeText(gson.toJson(jsonObject))
    }
}