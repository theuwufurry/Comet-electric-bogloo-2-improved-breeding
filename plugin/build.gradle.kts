plugins {
    kotlin("jvm")
}

version = parent!!.version

dependencies {
    implementation(project(":api"))
    implementation(files("gradle/build/libs/Mengshe-0.0.3.23.jar"))
    implementation("org.graalvm.polyglot:polyglot:25.0.0")
    implementation("org.graalvm.polyglot:js:25.0.0")
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

    transform(com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer::class.java)

    manifest {
        attributes(
            "Multi-Release" to "true"
        )
    }

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    exclude("kotlin/**")
    exclude { elem ->
        val path = elem.path
        path.startsWith("org") &&
        !path.startsWith("org/graalvm")
    }
    relocate("kotlin", "gg.aquatic.waves.libs.kotlin")
}

tasks.processResources {
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("plugin.yml") {
        expand(getProperties())
        expand(mutableMapOf("version" to project.version))
    }
}