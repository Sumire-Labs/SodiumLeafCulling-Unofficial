pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun target(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) {
                version("$project-$loader", version)
                    .buildscript("build.$loader.gradle.kts")
            }
        }

        // Existing legacy targets retained during the build-system migration.
        target("1.20.1", "fabric", "forge")
        target("1.20.4", "fabric", "neoforge")

        // Every 1.21 patch release for which Sodium publishes the requested loader.
        for (patch in 1..8) target("1.21.$patch", "fabric", "neoforge")
        target("1.21.9", "fabric") // Sodium has no NeoForge artifact for Minecraft 1.21.9.
        for (patch in 10..11) target("1.21.$patch", "fabric", "neoforge")

        target("26.1", "fabric", "neoforge")
        target("26.1.1", "fabric", "neoforge")
        target("26.1.2", "fabric", "neoforge")
        target("26.2", "fabric", "neoforge")

        vcsVersion = "26.2-fabric"
    }
}

rootProject.name = "SodiumLeafCulling-Unofficial"
