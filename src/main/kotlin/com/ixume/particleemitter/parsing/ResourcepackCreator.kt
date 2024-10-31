package com.ixume.particleemitter.parsing

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ixume.particleemitter.ParticleEmitter
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileReader
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
        val images: MutableList<File> = mutableListOf()

        val oldRPFolder = File(dataFolder.path + "/output/$RP_NAME")
        if (oldRPFolder.exists()) oldRPFolder.deleteRecursively()

        if (texturesFolder.exists()) {
            for (file in texturesFolder.listFiles()!!) {
                if (file.extension != "png") continue
                images += file
            }

            genSprites(images)
        }

        val modelFolder = File(dataFolder.path + "/models")

        if (modelFolder.exists()) {
            val models: MutableList<File> = mutableListOf()
            for (file in modelFolder.listFiles()!!) {
                if (file.isDirectory) models += file
            }

            genModels(models)
        }
    }

    private fun genModels(files: List<File>) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val itemFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/models/item")
        itemFolder.mkdirs()

        val modelFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/models/item/particlecreator")
        modelFolder.mkdirs()

        val texturesFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/textures/item/particlecreator")
        texturesFolder.mkdirs()

        for (file in files) {
            val model = File(file.path + "/" + file.nameWithoutExtension + ".json")
            val texture = File(file.path + "/" + file.nameWithoutExtension + ".png")
            if (!model.exists() || !texture.exists()) continue
            var item: File? = null

            for (subFile in file.listFiles()!!) {
                if (subFile != model && subFile != texture) {
                    item = subFile
                    break
                }
            }

            if (item == null) continue

            val newModel = File(modelFolder.path + "/" + model.name)
            model.copyTo(newModel)

            val rootObject = JsonParser.parseReader(FileReader(newModel)).asJsonObject
            val texturesObject = rootObject.getAsJsonObject("textures")

            texturesObject.remove("0")
            texturesObject.addProperty("0", "item/particlecreator/" + file.nameWithoutExtension)
            texturesObject.remove("particle")
            texturesObject.addProperty("particle", "item/particlecreator/" + file.nameWithoutExtension)

            val gson = GsonBuilder().setPrettyPrinting().create()
            newModel.writeText(gson.toJson(rootObject))

            texture.copyTo(File(texturesFolder.path + "/" + texture.name))
            item.copyTo(File(itemFolder.path + "/" + item.name))
        }
    }

    private fun genSprites(images: List<File>) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val packImage = File(dataFolder.path + "/output/$RP_NAME/pack.png")
        packImage.parentFile.mkdirs()
        packImage.writeBytes(ParticleEmitter.INSTANCE.getResource(RP_IMAGE)!!.readAllBytes())

        val packMeta = File(dataFolder.path + "/output/$RP_NAME/pack.mcmeta")
        packMeta.writeBytes(ParticleEmitter.INSTANCE.getResource(RP_META)!!.readAllBytes())

        genImages(images)
        genFont(images)
        genLang(images)
    }

    private fun genImages(images: List<File>) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val texturesFolder = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/textures/font")
        texturesFolder.mkdirs()

        for (image in images) {
            val imageFile = File(texturesFolder.path + "/" + image.name)
            image.copyTo(imageFile)
        }
    }

    private fun genFont(images: List<File>) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val font = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/font/default.json")
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
            jsonObject.addProperty("file", "$NAMESPACE:font/${image.name}")
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

    private fun genLang(images: List<File>) {
        val dataFolder = ParticleEmitter.INSTANCE.dataFolder

        val lang = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/lang/en_us.json")
        lang.parentFile.mkdirs()

        val jsonObject = JsonObject()
        var index = '\uE000'

        for (image in images) {
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