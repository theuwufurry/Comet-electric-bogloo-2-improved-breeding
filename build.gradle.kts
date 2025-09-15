plugins {
    kotlin("jvm") version "2.2.0"
    id("io.github.goooler.shadow") version "8.1.8"
    id("co.uzzu.dotenv.gradle") version "2.0.0"
}

group = "gg.aquatic"
version = "1.14.0"

kotlin {
    jvmToolchain(21)
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "io.github.goooler.shadow")

    repositories {
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenCentral()
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven { url = uri("https://repo.nekroplex.com/releases") }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
        }
        maven("https://oss.sonatype.org/content/groups/public/") {
            name = "sonatype"
        }
        maven("https://mvn.lumine.io/repository/maven-public/")
    }

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
        compileOnly("gg.aquatic.waves:Waves:1.3.6:publish")

        compileOnly("org.openjdk.nashorn:nashorn-core:15.4")

        compileOnly("io.lumine:Mythic-Dist:5.6.1")
        compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.7")

        compileOnly("com.github.retrooper:packetevents-spigot:2.9.5")
    }

    kotlin {
        jvmToolchain(21)
    }
}