plugins {
    kotlin("jvm")
}

group = "gg.aquatic"
version = "1.2.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("Comet-${project.version}.jar")
    archiveClassifier.set("plugin")

    exclude("kotlin/**")
    exclude("org/**")
    relocate("kotlin", "gg.aquatic.waves.shadow.kotlin")
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("plugin.yml") {
        expand(getProperties())
        expand(mutableMapOf("version" to project.version))
    }
}
