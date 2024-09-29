plugins {
    kotlin("jvm") version "2.0.10"
}

group = "gg.aquatic.particleemitter"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven {
        url = uri("https://repo.nekroplex.com/releases")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("gg.aquatic.waves:Waves:1.0.1")
}

val targetJavaVersion = 17
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(targetJavaVersion)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}