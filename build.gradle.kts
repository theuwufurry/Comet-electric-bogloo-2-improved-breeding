plugins {
    kotlin("jvm") version "2.0.21"
    id("io.github.goooler.shadow") version "8.1.8"
    id("co.uzzu.dotenv.gradle") version "2.0.0"
}

group = "gg.aquatic"
version = "1.2.0"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    mavenCentral()
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven {
        url = uri("https://repo.nekroplex.com/releases")
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("gg.aquatic.waves:Waves:1.1.9:publish")

    compileOnly("org.openjdk.nashorn:nashorn-core:15.4")

    compileOnly("io.lumine:Mythic-Dist:5.6.1")
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.7")
}

kotlin {
    jvmToolchain(17)
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "io.github.goooler.shadow")

    repositories {
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenCentral()
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven {
            url = uri("https://repo.nekroplex.com/releases")
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
        }
        maven("https://oss.sonatype.org/content/groups/public/") {
            name = "sonatype"
        }
        maven("https://mvn.lumine.io/repository/maven-public/")
    }

    dependencies {
        compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
        compileOnly("gg.aquatic.waves:Waves:1.1.9:publish")

        compileOnly("org.openjdk.nashorn:nashorn-core:15.4")

        compileOnly("io.lumine:Mythic-Dist:5.6.1")
        compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.7")
    }

    kotlin {
        jvmToolchain(17)
    }
}