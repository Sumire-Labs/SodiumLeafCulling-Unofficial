pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://repo.spongepowered.org/repository/maven-public/") { name = "Sponge" }
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

        // Historical Sodium and Embeddium releases. Keep one Fabric target per
        // Minecraft release and add Forge/NeoForge where Embeddium published
        // an artifact for that exact release.
        target("1.16.3", "fabric")
        target("1.16.4", "fabric")
        target("1.16.5", "fabric")
        version("1.16.5-forge", "1.16.5").buildscript("build.forge16.bridge.gradle.kts")
        target("1.17", "fabric")
        target("1.17.1", "fabric")
        target("1.18", "fabric")
        target("1.18.1", "fabric")
        target("1.18.2", "fabric", "forge")
        target("1.19", "fabric")
        target("1.19.1", "fabric")
        target("1.19.2", "fabric", "forge")
        target("1.19.3", "fabric")
        target("1.19.4", "fabric")
        target("1.20", "fabric")

        // Existing legacy targets retained during the build-system migration.
        target("1.20.1", "fabric", "forge")
        target("1.20.2", "fabric")
        version("1.20.2-neoforge", "1.20.2").buildscript("build.neoforge202.bridge.gradle.kts")
        version("1.20.2-forge", "1.20.2").buildscript("build.forge20.bridge.gradle.kts")
        target("1.20.3", "fabric")
        version("1.20.3-neoforge", "1.20.3").buildscript("build.neoforge203.bridge.gradle.kts")
        target("1.20.4", "fabric", "neoforge")
        target("1.20.5", "fabric")
        version("1.20.5-neoforge", "1.20.5").buildscript("build.neoforge205.bridge.gradle.kts")
        target("1.20.6", "fabric", "neoforge")

        // Every 1.21 patch release for which Sodium publishes the requested loader.
        target("1.21", "fabric", "neoforge")
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
