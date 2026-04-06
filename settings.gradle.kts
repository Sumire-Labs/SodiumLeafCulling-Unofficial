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
	id("dev.kikugie.stonecutter") version "0.9.1-beta.3"
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

stonecutter {
	kotlinController = true
	centralScript = "build.gradle.kts"
	shared {
		fun mc(mcVersion: String, vararg loaders: String) {
			for (loader in loaders) version("$mcVersion-$loader", mcVersion)
		}

		mc("26.1.1", "neoforge")
	}
	create(rootProject)
}

rootProject.name = "SodiumLeafCulling"
