plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
//    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "gg.aquatic"
version = "1.0-SNAPSHOT"

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
}

dependencies {
//    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
//    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("gg.aquatic.waves:Waves:1.0.51:publish")

    compileOnly("org.openjdk.nashorn:nashorn-core:15.4")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("Comet-${project.version}.jar")
    archiveClassifier.set("plugin")

    exclude("kotlin/**")
    exclude("org/**")
//    relocate("kotlin", "gg.aquatic.waves.shadow.kotlin")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
