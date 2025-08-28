plugins {
    kotlin("jvm") version "2.2.0"
    id("io.github.goooler.shadow") version "8.1.8"
    id("co.uzzu.dotenv.gradle") version "2.0.0"
}

group = "gg.aquatic"
version = "1.13.1"

repositories {
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
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("gg.aquatic.waves:Waves:1.3.7:publish")

    compileOnly("org.openjdk.nashorn:nashorn-core:15.4")

    compileOnly("io.lumine:Mythic-Dist:5.6.1")
    implementation("net.kyori:adventure-api:4.24.0")
    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.7")
    implementation(kotlin("stdlib-jdk8"))
}

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
    }

    kotlin {
        jvmToolchain(21)
    }
}