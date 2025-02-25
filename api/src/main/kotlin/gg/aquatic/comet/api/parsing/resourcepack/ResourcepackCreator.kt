package gg.aquatic.comet.api.parsing.resourcepack

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.component.ComponentTypes
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.item.ItemStack
import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.item.type.ItemTypes
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileReader
import javax.imageio.ImageIO
import kotlin.random.Random

object ResourcepackCreator {
    private const val RP_NAME = "Particle Creator"
    private const val RP_IMAGE = "pack.png"
    private const val RP_META = "pack.mcmeta"
    private const val NAMESPACE = "particlecreator"
    private const val PARTICLES_PNG = "particles.png"

    val modelMap: MutableMap<String, ItemStack> = mutableMapOf()

    val uvs: MutableList<UVData> = mutableListOf()
    /*
    look through provided sprites for something of matching name
    generate imgs with that label
    label images with uv coord and size
    img_${x_coord}_${y_coord}_${x_size}_${y_size}
     */
    fun initTextures(texturesFolder: File) {
        val defaultParticles = File(texturesFolder, "particles.png")
        if (!defaultParticles.exists()) {
            defaultParticles.writeBytes(AbstractParticleEmitter.INSTANCE.getResource(PARTICLES_PNG)!!.readAllBytes())
        }
    }

    fun genPack() {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder
        dataFolder.mkdirs()

        val texturesFolder = File(dataFolder.path + "/textures")
        texturesFolder.mkdirs()

        initTextures(texturesFolder)

        val images: MutableList<File> = mutableListOf()

        val oldRPFolder = File(dataFolder.path + "/output/$RP_NAME")
        if (oldRPFolder.exists()) oldRPFolder.deleteRecursively()

        if (texturesFolder.exists()) {
            for (file in texturesFolder.listFiles()!!) {
                if (file.extension != "png") continue
                images += file
            }

            if (uvs.size != 0) {
                val tempDir = File(dataFolder, ".temp/")
                tempDir.mkdirs()

                genUVs(images, tempDir)

                images += tempDir.listFiles()!!.filter { it.extension == "png" }
            }

            genSprites(images)
        }

        val tempDir = File(dataFolder, ".temp/")
        tempDir.deleteRecursively()

        val modelFolder = File(dataFolder.path + "/models")

        if (modelFolder.exists()) {
            val models: MutableList<File> = mutableListOf()
            for (file in modelFolder.listFiles()!!) {
                if (file.isDirectory) models += file
            }

            genModels(models)
        }

        uvs.clear()
    }

    private fun genModels(files: List<File>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val itemFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/models/item")
        itemFolder.mkdirs()

        val modelFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/models/item/particlecreator")
        modelFolder.mkdirs()

        val texturesFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/textures/item/particlecreator")
        texturesFolder.mkdirs()

        val gson = GsonBuilder().setPrettyPrinting().create()

        val texturesObj = JsonObject()

        val itemsObj = JsonObject()
        itemsObj.addProperty("parent", "item/structure_block")
        texturesObj.addProperty("layer0", "item/structure_block")
        itemsObj.add("textures", texturesObj)

        val overridesArr = JsonArray()

        for (file in files) {
            val model = File(file.path + "/" + file.nameWithoutExtension + ".json")
            val texture = File(file.path + "/" + file.nameWithoutExtension + ".png")
            if (!model.exists() || !texture.exists()) continue

            val newModel = File(modelFolder.path + "/" + model.name)
            model.copyTo(newModel)

            val rootObject = JsonParser.parseReader(FileReader(newModel)).asJsonObject
            val texturesObject = rootObject.getAsJsonObject("textures")

            texturesObject.remove("0")
            texturesObject.addProperty("0", "item/particlecreator/" + file.nameWithoutExtension)
            texturesObject.remove("particle")
            texturesObject.addProperty("particle", "item/particlecreator/" + file.nameWithoutExtension)

            newModel.writeText(gson.toJson(rootObject))

            texture.copyTo(File(texturesFolder.path + "/" + texture.name))

            val override = JsonObject()
            val predicate = JsonObject()
            val index = Random.nextInt(1024, Integer.MAX_VALUE)
            val stack = ItemStack.builder().type(ItemTypes.getByName("structure_block")).amount(1).build()
            stack.setComponent(ComponentTypes.CUSTOM_MODEL_DATA, index)

            modelMap += file.nameWithoutExtension to stack

            predicate.addProperty("custom_model_data", index)
            override.add("predicate", predicate)
            override.addProperty("model", "item/particlecreator/${file.nameWithoutExtension}")
            overridesArr.add(override)
        }

        itemsObj.add("overrides", overridesArr)

        val itemTarget = File(itemFolder.path + "/structure_block.json")
        itemTarget.writeText(gson.toJson(itemsObj))
    }

    private fun genSprites(images: List<File>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val packImage = File(dataFolder.path + "/output/$RP_NAME/pack.png")
        packImage.parentFile.mkdirs()
        packImage.writeBytes(AbstractParticleEmitter.INSTANCE.getResource(RP_IMAGE)!!.readAllBytes())

        val packMeta = File(dataFolder.path + "/output/$RP_NAME/pack.mcmeta")
        packMeta.writeBytes(AbstractParticleEmitter.INSTANCE.getResource(RP_META)!!.readAllBytes())

        genImages(images)
        genFont(images)
        genLang(images)
    }

    private fun genImages(images: List<File>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val texturesFolder = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/textures/font")
        texturesFolder.mkdirs()

        for (image in images) {
            val imageFile = File(texturesFolder.path + "/" + image.name)
            image.copyTo(imageFile)
        }
    }

    private fun genFont(images: List<File>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val font = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/font/default.json")
        font.parentFile.mkdirs()

        val providers = JsonArray()
        var index = '\uE000'

        for (image in images) {
            val bufferedImage: BufferedImage = ImageIO.read(image)

            val width = bufferedImage.width
            val height = bufferedImage.height

            val jsonObject = JsonObject()

            val charArray = JsonArray()

            if (width % height != 0) {
                charArray.add(index.toString())
                index++
            } else {
                val count = width / height
                val chars = StringBuilder()

                for (i in 0 until count) {
                    chars.append(index.toString())
                    index++
                }

                charArray.add(chars.toString())
            }

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
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val lang = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/lang/en_us.json")
        lang.parentFile.mkdirs()

        val jsonObject = JsonObject()
        var index = '\uE000'

        for (image in images) {
            val bufferedImage: BufferedImage = ImageIO.read(image) ?: continue
            val width = bufferedImage.width
            val height = bufferedImage.height

            if (width % height != 0) {
                jsonObject.addProperty(image.nameWithoutExtension, index.toString())
                index++
            } else {
                val count = width / height

                if (count > 1) {
                    for (i in 0 until count) {
                        jsonObject.addProperty(image.nameWithoutExtension + "." + i, index.toString())
                        index++
                    }
                } else {
                    jsonObject.addProperty(image.nameWithoutExtension, index.toString())
                    index++
                }
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        lang.writeText(gson.toJson(jsonObject))
    }

    private fun genUVs(images: List<File>, targetDir: File) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder
        if (!dataFolder.exists()) return

        for ((coords, size, name) in uvs) {
            val image = images.firstOrNull { it.nameWithoutExtension == name }
            if (image == null) {
                AbstractParticleEmitter.INSTANCE.logger.warning("UV $name does not having a provided texture!")
                continue
            }

            val bufferedImage: BufferedImage = ImageIO.read(image)
            if (bufferedImage.width < coords.x + size.x || bufferedImage.height < coords.y + size.y) {
                AbstractParticleEmitter.INSTANCE.logger.warning("$name is too small for the specified uv size!")
                continue
            }

            val subImage = bufferedImage.getSubimage(coords.x, coords.y, size.x, size.y)!!.ensureSize()
            ImageIO.write(subImage, "png", File(targetDir, "${name}_${coords.x}_${coords.y}_${size.x}_${size.y}.png"))
        }
    }

    private fun BufferedImage.ensureSize(): BufferedImage {
        ensureFilled(0, 0)
        ensureFilled(width - 1, height - 1)
        return this
    }

    private fun BufferedImage.ensureFilled(x: Int, y: Int) {
        if (getRGB(x, y) ushr 24 == 0) setRGB(x, y, Color(255, 255, 255, 4).rgb)
    }
}