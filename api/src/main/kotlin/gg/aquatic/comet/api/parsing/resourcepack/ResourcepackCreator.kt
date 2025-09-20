package gg.aquatic.comet.api.parsing.resourcepack

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.api.parsing.resourcepack.packages.PackageManager
import gg.aquatic.waves.Waves
import gg.aquatic.waves.util.version.ServerVersion
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileReader
import javax.imageio.ImageIO

object ResourcepackCreator {
    private const val RP_NAME = "Particle Creator"
    private const val RP_IMAGE = "pack.png"
    private const val RP_META = "pack.mcmeta"
    private const val ITEM = "amethyst_shard"
    private val ITEM_TYPE = Material.AMETHYST_SHARD
    const val NAMESPACE = "p"
    const val FONT_NAME = "d"
    private const val PARTICLES_PNG = "particles.png"

    val modelMap: MutableMap<String, ModelData> = mutableMapOf()
    val charMap: MutableMap<String, Char> = mutableMapOf()

    fun stack(id: String, color: Int): ItemStack? {
        val md = modelMap[id] ?: return null
        return createStack(md.index, color)
    }

    private fun createStack(index: Int, color: Int?): ItemStack {
        val stack = ItemStack(ITEM_TYPE)
        stack.editMeta { im ->
            im.setCustomModelData(index)
            color?.let {
                if (!ServerVersion.ofAquatic(Waves.INSTANCE)!!.isOlder(ServerVersion.V_1_21_4)) {
                    // TODO: Check if it is really ARGB
                    im.customModelDataComponent.floats = listOf(index.toFloat())
                    im.customModelDataComponent.colors = listOf(org.bukkit.Color.fromARGB(it))
                }
            }
        }

        return stack
    }

    val uvs: MutableList<UVData> = mutableListOf()

    /*
    look through provided sprites for something of matching name
    generate imgs with that label
    label images with uv coord and size
    img_${x_coord}_${y_coord}_${x_size}_${y_size}
     */
    fun initTextures(texturesFolder: File) {
        val defaultParticles = File(texturesFolder, PARTICLES_PNG)
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

        val images = mutableListOf<Texture>()

        val oldRPFolder = File(dataFolder.path + "/output/$RP_NAME")
        if (oldRPFolder.exists()) oldRPFolder.deleteRecursively()

        if (texturesFolder.exists()) {
            for (file in texturesFolder.listFiles()!!) {
                if (file.extension != "png") continue
                images += Texture(null, file)
            }

            if (uvs.isNotEmpty()) {
                val tempDir = File(dataFolder, ".temp/")
                tempDir.mkdirs()

                genUVs(images, tempDir)

                images += tempDir.listFiles()!!.filter { it.extension == "png" }.map { Texture(null, it) }
            }
        }

        images += PackageManager.packages.flatMap { it.textures }

        images.sortBy { it.name }

        genSprites(images)

        val tempDir = File(dataFolder, ".temp/")
        tempDir.deleteRecursively()

        val modelFolder = File(dataFolder.path + "/models")
        val models: MutableList<Model> = mutableListOf()

        if (modelFolder.exists()) {
            for (file in modelFolder.listFiles()!!) {
                if (file.isDirectory) models += Model(null, file)
            }
        }

        models += PackageManager.packages.flatMap { it.models }

        models.sortBy { it.name }

        genModels(models)

        uvs.clear()

        val soundsFolder = File(dataFolder.path + "/sounds")

        if (soundsFolder.exists()) {
            val soundFiles: MutableList<File> = mutableListOf()
            for (file in soundsFolder.listFiles()!!) {
                soundFiles += file
            }

            soundFiles.sortBy { it.name }

            genSounds(soundFiles)
        }
    }

    private fun genModels(files: List<Model>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val itemFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/items")
        itemFolder.mkdirs()

        val modelItemFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/models/item")
        modelItemFolder.mkdirs()

        val modelFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/models/item/$NAMESPACE")
        modelFolder.mkdirs()

        val texturesFolder = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/textures/item/$NAMESPACE")
        texturesFolder.mkdirs()

        val gson = GsonBuilder().setPrettyPrinting().create()

        val texturesObj = JsonObject()

        val itemObj = JsonObject()

        val mObj = JsonObject()

        mObj.addProperty("type", "range_dispatch")
        mObj.addProperty("property", "custom_model_data")

        val fallback = JsonObject()
        fallback.addProperty("type", "model")
        fallback.addProperty("model", "item/$ITEM")

        mObj.add("fallback", fallback)

        val entries = JsonArray()

        mObj.add("entries", entries)

        itemObj.add("model", mObj)

        val tints = JsonArray()

        val modelDataTint = JsonObject()
        modelDataTint.addProperty("type", "custom_model_data")
        modelDataTint.addProperty("index", 0)
        modelDataTint.addProperty("default", 16777215)

        tints.add(modelDataTint)

        val modelItemsObj = JsonObject()
        modelItemsObj.addProperty("parent", "item/generated")
        texturesObj.addProperty("layer0", "item/$ITEM")
        modelItemsObj.add("textures", texturesObj)

        val overridesArr = JsonArray()

        var count = 100

        for (file in files) {
            val model = File(file.file.path + "/" + file.modelName + ".json")
            if (!model.exists()) continue

            val newModel = File(modelFolder.path + "/" + file.name + ".json")
            model.copyTo(newModel)

            val rootObject = JsonParser.parseReader(FileReader(newModel)).asJsonObject
            val texturesObject = rootObject.getAsJsonObject("textures")

            val newTexturesObj = JsonObject()
            for ((key, value) in texturesObject.entrySet()) {
                val asStr = value.asStringOrNull() ?: continue
                newTexturesObj.addProperty(key, "item/$NAMESPACE/${file.nameWithoutExtension}_$asStr")

                val tex = File(file.file.path + "/" + asStr + ".png")
                if (tex.exists()) {
                    val newFileLoc = File(texturesFolder.path, "${file.nameWithoutExtension}_$asStr.png")
                    if (!newFileLoc.exists()) {
                        tex.copyTo(newFileLoc)
                    }
                }
            }

            rootObject.remove("textures")
            rootObject.add("textures", newTexturesObj)

            newModel.writeText(gson.toJson(rootObject))

            val override = JsonObject()
            val predicate = JsonObject()
            val index = count++

            val stack = ItemStack(ITEM_TYPE)
            stack.editMeta { im ->
                im.setCustomModelData(index)
                if (!ServerVersion.ofAquatic(Waves.INSTANCE)!!.isOlder(ServerVersion.V_1_21_4)) {
                    im.customModelDataComponent.floats = listOf(index.toFloat())
                }
            }

            val iObj = JsonObject()
            iObj.addProperty("threshold", index)

            val miObj = JsonObject()

            miObj.addProperty("type", "model")
            miObj.addProperty("model", "item/$NAMESPACE/${file.nameWithoutExtension}")
            miObj.add("tints", tints)

            iObj.add("model", miObj)

            entries.add(iObj)

            modelMap += file.nameWithoutExtension to ModelData(index = index)

            predicate.addProperty("custom_model_data", index)
            override.add("predicate", predicate)
            override.addProperty("model", "item/$NAMESPACE/${file.nameWithoutExtension}")
            overridesArr.add(override)
        }

        modelItemsObj.add("overrides", overridesArr)

        val modelItemTarget = File(modelItemFolder.path, "$ITEM.json")
        modelItemTarget.writeText(gson.toJson(modelItemsObj))

        val itemTarget = File(itemFolder.path, "$ITEM.json")
        itemTarget.writeText(gson.toJson(itemObj))
    }

    private fun genSprites(images: List<Texture>) {
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

    private fun genImages(images: List<Texture>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val texturesFolder = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/textures/font")
        texturesFolder.mkdirs()

        for (image in images) {
            val imageFile = File(texturesFolder.path, "${image.name}")
            val bufferedImage: BufferedImage = ImageIO.read(image.file)
            val argbImage = BufferedImage(bufferedImage.width, bufferedImage.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = argbImage.createGraphics()
            graphics.drawImage(bufferedImage, 0, 0, null)
            graphics.dispose()
            argbImage.ensureGrid(bufferedImage.height)
            argbImage.ensureSize()

            ImageIO.write(argbImage, "png", imageFile)
        }
    }

    private fun genFont(images: List<Texture>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val font = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/font/$FONT_NAME.json")
        font.parentFile.mkdirs()

        val providers = JsonArray()
        var index = '\uE000'

        for (image in images) {
            val bufferedImage: BufferedImage = ImageIO.read(image.file)

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

                repeat(count) {
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
        val s = gson.toJson(completeObject)
        font.writeText(s)

        val mcFont = File(dataFolder.path + "/output/$RP_NAME/assets/minecraft/font/default.json")
        mcFont.parentFile.mkdirs()
        mcFont.writeText(s)
    }

    private fun genLang(images: List<Texture>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val lang = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/lang/en_us.json")
        lang.parentFile.mkdirs()

        val jsonObject = JsonObject()
        var index = '\uE000'

        for (image in images) {
            val bufferedImage: BufferedImage = ImageIO.read(image.file) ?: continue
            val width = bufferedImage.width
            val height = bufferedImage.height

            if (width % height != 0) {
                jsonObject.addProperty(image.nameWithoutExtension, index.toString())
                charMap[image.nameWithoutExtension] = index
                index++
            } else {
                val count = width / height

                if (count > 1) {
                    for (i in 0 until count) {
                        jsonObject.addProperty(image.nameWithoutExtension + "." + i, index.toString())
                        charMap[image.nameWithoutExtension + "." + i] = index
                        index++
                    }
                } else {
                    jsonObject.addProperty(image.nameWithoutExtension, index.toString())
                    charMap[image.nameWithoutExtension] = index
                    index++
                }
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        lang.writeText(gson.toJson(jsonObject))
    }

    private fun genUVs(images: List<Texture>, targetDir: File) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder
        if (!dataFolder.exists()) return

        for ((coords, size, name) in uvs) {
            val image = images.firstOrNull { it.namespace == null && it.file.nameWithoutExtension == name }
            if (image == null) {
                AbstractParticleEmitter.INSTANCE.logger.warning("UV $name does not having a provided texture!")
                continue
            }

            val bufferedImage: BufferedImage = ImageIO.read(image.file)
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

    private fun BufferedImage.ensureGrid(interval: Int): BufferedImage {
        if (interval < 0) throw IllegalArgumentException("Can't accept an interval < 0")
        for (x in 0..<width step interval) {
            for (y in 0..<height step interval) {
                ensureFilled(x, y)
            }
        }

        for (x in (interval - 1)..<width step interval) {
            for (y in (interval - 1)..<height step interval) {
                ensureFilled(x, y)
            }
        }

        return this
    }

    private fun BufferedImage.ensureFilled(x: Int, y: Int) {
        if (getRGB(x, y) ushr 24 == 0) setRGB(x, y, Color(255, 0, 255, 4).rgb)
    }

    private fun genSounds(soundFiles: List<File>) {
        val dataFolder = AbstractParticleEmitter.INSTANCE.dataFolder

        val soundsFolder = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/sounds/")
        soundsFolder.mkdirs()

        val jsonObject = JsonObject()

        for (soundFile in soundFiles) {
            val soundObj = JsonObject()
            soundObj.addProperty("category", "master")
            val soundsArr = JsonArray()
            soundsArr.add(NAMESPACE + ":" + soundFile.nameWithoutExtension)
            soundObj.add("sounds", soundsArr)
            jsonObject.add(soundFile.nameWithoutExtension, soundObj)

            val newSoundFile = File(soundsFolder, soundFile.name)
            soundFile.copyTo(newSoundFile)
        }

        val soundTarget = File(dataFolder.path + "/output/$RP_NAME/assets/$NAMESPACE/sounds.json")
        val gson = GsonBuilder().setPrettyPrinting().create()
        soundTarget.writeText(gson.toJson(jsonObject))
    }
}