import dev.kikugie.stonecutter.StonecutterSettings

pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/")
		maven("https://maven.architectury.dev")
		maven("https://maven.minecraftforge.net")
		maven("https://maven.neoforged.net/releases/")
		maven("https://maven.kikugie.dev/snapshots")
		maven("https://maven.kikugie.dev/releases")
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.5-alpha.4"
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

extensions.configure<StonecutterSettings> {
	kotlinController = true
	centralScript = "build.gradle.kts"
	shared {
		fun mc(version: String, vararg loaders: String) {
			for (it in loaders) vers("$version-$it", version)
		}

		mc("1.20.1", "fabric", "forge")
		mc("1.20.4", "fabric", "neoforge")
		mc("1.21.1", "fabric", "neoforge")
		mc("1.21.4", "fabric", "neoforge")
		mc("1.21.11", "fabric", "neoforge")
		//mc("26.1.1", "neoforge") // Requires Gradle 9.4 + Loom 1.15 - build separately
	}
	create(rootProject)
}

rootProject.name = "SodiumLeafCulling"