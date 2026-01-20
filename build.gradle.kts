plugins {
    kotlin("jvm") version "2.2.0"
    id ("com.gradleup.shadow") version "9.3.0"
    id("co.uzzu.dotenv.gradle") version "2.0.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"

}

group = "gg.aquatic"
version = "1.17.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "io.papermc.paperweight.userdev")

    repositories {
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenCentral()
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven { url = uri("https://repo.nekroplex.com/releases") }
        maven("https://repo.papermc.io/repository/maven-public/")

        maven("https://oss.sonatype.org/content/groups/public/") {
            name = "sonatype"
        }
        maven("https://mvn.lumine.io/repository/maven-public/")
    }

    dependencies {
        paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
        compileOnly("org.openjdk.nashorn:nashorn-core:15.4")

//        compileOnly("io.lumine:Mythic-Dist:5.6.1")
//        compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.7")

        implementation("com.github.retrooper:packetevents-spigot:2.11.1")
    }

    kotlin {
        jvmToolchain(21)
    }
}