pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://repo.spongepowered.org/repository/maven-public/") { name = "Sponge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.taumc.org/releases") { name = "TauMC" }
        maven("https://repo.codemc.io/repository/relativitymc/") { name = "RelativityMC" }
    }
    plugins {
        id("net.fabricmc.fabric-loom") version "1.18.2"
        id("net.fabricmc.fabric-loom-remap") version "1.18.2"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val defaultTarget = "26.2-fabric"
val selectedTargets = providers.gradleProperty("build_targets").orNull
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.toSet()

stonecutter {
    create(rootProject) {
        val knownTargets = mutableSetOf<String>()
        fun target(project: String, vararg loaders: String, version: String = project, buildscript: String? = null) {
            for (loader in loaders) {
                val name = "$project-$loader"
                knownTargets += name
                if (selectedTargets == null || name in selectedTargets || name == defaultTarget) {
                    val script = buildscript ?: if (loader == "fabric" && version.startsWith("26."))
                        "build.fabric.unobfuscated.gradle.kts" else "build.$loader.gradle.kts"
                    version(name, version).buildscript(script)
                }
            }
        }

        // Historical Sodium and Embeddium releases. Keep one Fabric target per
        // Minecraft release and add Forge/NeoForge where Embeddium published
        // an artifact for that exact release.
        target("1.16.3", "fabric")
        target("1.16.4", "fabric")
        target("1.16.5", "fabric")
        target("1.16.5", "forge")
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
        target("1.20.2", "neoforge", buildscript = "build.neogradle.gradle.kts")
        target("1.20.2", "forge", buildscript = "build.neoloom.gradle.kts")
        target("1.20.3", "fabric")
        target("1.20.3", "neoforge", buildscript = "build.neogradle.gradle.kts")
        target("1.20.4", "fabric", "neoforge")
        target("1.20.5", "fabric")
        target("1.20.5", "neoforge", buildscript = "build.neogradle.gradle.kts")
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
        target("26.3", "fabric", "neoforge")

        selectedTargets?.let { targets ->
            require(knownTargets.containsAll(targets)) { "Unknown build targets: ${targets - knownTargets}" }
        }
        vcsVersion = defaultTarget
    }
}

rootProject.name = "SodiumLeafCulling-Unofficial"
