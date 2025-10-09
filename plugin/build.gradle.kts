plugins {
    kotlin("jvm")
}

version = parent!!.version

dependencies {
    implementation(project(":api"))
    implementation(files("gradle/build/libs/Mengshe-0.0.3.23.jar"))
    compileOnly(files("gradle/build/libs/Udar-0.2.0.jar"))
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    build {
        dependsOn(shadowJar)
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("Comet-${project.version}.jar")
    archiveClassifier.set("plugin")

    exclude("kotlin/**")
    exclude("org/**")
    relocate("kotlin", "gg.aquatic.waves.libs.kotlin")
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("plugin.yml") {
        expand(getProperties())
        expand(mutableMapOf("version" to project.version))
    }
}
