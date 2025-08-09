plugins {
    kotlin("jvm")
    `maven-publish`
}

version = parent!!.version

val maven_username = if (env.isPresent("MAVEN_USERNAME")) env.fetch("MAVEN_USERNAME") else ""
val maven_password = if (env.isPresent("MAVEN_PASSWORD")) env.fetch("MAVEN_PASSWORD") else ""

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

publishing {
    repositories {
        maven {
            name = "aquaticRepository"
            url = uri("https://repo.nekroplex.com/releases")

            credentials {
                username = maven_username
                password = maven_password
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "gg.aquatic.comet"
            artifactId = "Comet-API"
            version = "${project.version}"
            from(components["java"])
            /*
            artifact(tasks["shadowJarPublish"]) {
                classifier = "publish"
            }
            artifact(tasks["shadowJarPlugin"]) {
                classifier = "plugin"
            }
             */
        }
    }
}