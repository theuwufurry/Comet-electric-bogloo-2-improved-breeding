import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

version = parent!!.version

dependencies {
    implementation(project(":api"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
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

tasks.withType<ShadowJar> {
    archiveFileName.set("Comet-${project.version}.jar")
    archiveClassifier.set("plugin")

    // relocate dependencies
    relocate("com.github.retrooper.packetevents", "gg.aquatic.comet.shadow.packetevents")
    relocate("kotlin", "gg.aquatic.comet.libs.kotlin")

    // DO NOT exclude kotlin — that’s why Intrinsics crashes
    // exclude("kotlin/**")
    // exclude("org/**")
    relocate("kotlin", "gg.aquatic.comet.libs.kotlin")
    mergeServiceFiles() // always safe for META-INF services
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("plugin.yml") {
        expand(getProperties())
        expand(mutableMapOf("version" to project.version))
    }
}
