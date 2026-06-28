val settings = object : TxniTemplateSettings {

	// -------------------- Dependencies ---------------------- //
	override val depsHandler: DependencyHandler get() = object : DependencyHandler {
		override fun addGlobal(deps: DependencyHandlerScope) {

		}

		override fun addFabric(deps: DependencyHandlerScope) {
			deps.include(deps.implementation(deps.annotationProcessor("com.bawnorton.mixinsquared:mixinsquared-fabric:0.2.0-beta.6")!!)!!)

			if (mcVersion.startsWith("26."))
			{
				val sodiumSlug = when (mcVersion) {
					"26.2" -> "mc26.2-0.9.0-fabric"
					else -> "mc26.1.1-0.8.9-fabric"
				}
				deps.implementation(modrinth("sodium", sodiumSlug))
			}
			else if (mcVersion == "1.21.1")
			{
				deps.implementation(modrinth("sodium", "mc1.21.1-0.6.13-fabric"))
				deps.runtimeOnly(modrinth("moreculling", "0.27.1"))
			}
			else
			{
				deps.implementation(modrinth("sodium", "mc1.20.1-0.5.11"))
				deps.runtimeOnly(modrinth("moreculling", "0.24.0"))
				deps.implementation(modrinth("indium", "1.0.34+mc1.20.1"))
			}
		}

		override fun addForge(deps: DependencyHandlerScope) {
			deps.implementation(modrinth("embeddium", "0.3.31+mc1.20.1"))
			deps.compileOnly(deps.annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")!!)
			deps.include(deps.implementation("io.github.llamalad7:mixinextras-forge:0.3.5")!!)
		}

		override fun addNeo(deps: DependencyHandlerScope) {
			if (mcVersion.startsWith("26.")) {
				// Sodium(unobfuscated 26.x) は dependencies ブロックで compileOnly 追加（JarJar nesting なし）
			} else {
				deps.implementation(modrinth("sodium", "mc1.21.1-0.6.13-neoforge"))

				deps.compileOnly("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308dedd1")
				deps.compileOnly("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:3.4.0+acb05a39d1")
			}
		}
	}


	// ---------- Curseforge/Modrinth Configuration ----------- //
	// For configuring the dependecies that will show up on your mod page.
	override val publishHandler: PublishDependencyHandler get() = object : PublishDependencyHandler {
		override fun addShared(deps: DependencyContainer) {
			if (isFabric) {
				deps.requires("fabric-api")
				if (mcVersion == "1.20.1")
					deps.requires("indium") 
			}

			if (isForge)
				deps.requires("embeddium")
			else
				deps.requires("sodium")
		}

		override fun addCurseForge(deps: DependencyContainer) {

		}

		override fun addModrinth(deps: DependencyContainer) {

		}
	}
}


// ---------------TxniTemplate Build Script---------------- //
//   (only edit below this if you know what you're doing)
// -------------------------------------------------------- //

plugins {
	`maven-publish`
	kotlin("jvm")
	kotlin("plugin.serialization")
	//id("dev.kikugie.j52j") version "1.0" // Incompatible with Gradle 9.x
	id("dev.architectury.loom-no-remap")
	id("me.modmuss50.mod-publish-plugin")
	id("systems.manifold.manifold-gradle-plugin")
}

// The manifold Gradle plugin version. Update this if you update your IntelliJ Plugin!
manifold { manifoldVersion = "2024.1.30" }

// Variables
class ModData {
	val id = property("mod.id").toString()
	val name = property("mod.name").toString()
	val version = property("mod.version").toString()
	val group = property("mod.group").toString()
	val author = property("mod.author").toString()
	val namespace = property("mod.namespace").toString()
	val displayName = property("mod.display_name").toString()
	val description = property("mod.description").toString()
	val mcDep = property("mod.mc_dep").toString()
	val license = property("mod.license").toString()
	val github = property("mod.github").toString()
}

val mod = ModData()

val mcVersion = stonecutter.current.project.substringBeforeLast('-')
val isUnobfuscated = mcVersion.startsWith("26.")

val loader = loom.platform.get().name.lowercase()
val isFabric = loader == "fabric"
val isForge = loader == "forge"
val isNeo = loader == "neoforge"

version = "${mod.version}-$mcVersion"
group = mod.group
base { archivesName.set("${mod.id}-$loader") }

// Dependencies
repositories {
	fun strictMaven(url: String, vararg groups: String) = exclusiveContent {
		forRepository { maven(url) }
		filter { groups.forEach(::includeGroup) }
	}
	strictMaven("https://www.cursemaven.com", "curse.maven")
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
	strictMaven("https://thedarkcolour.github.io/KotlinForForge/", "thedarkcolour")
	maven("https://maven.kikugie.dev/releases")
	maven("https://jitpack.io")
	maven("https://maven.neoforged.net/releases/")
	maven("https://maven.terraformersmc.com/releases/")
	maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
	maven("https://maven.parchmentmc.org")
	maven("https://maven.su5ed.dev/releases")
	maven("https://maven.bawnorton.com/releases")
}

dependencies {
	minecraft("com.mojang:minecraft:${mcVersion}")

	// apply the Manifold processor, do not remove this unless you want to swap back to Stonecutter preprocessor
	implementation(annotationProcessor("systems.manifold:manifold-preprocessor:${manifold.manifoldVersion.get()}")!!)

	// 26.1.1+ is unobfuscated - no mappings needed with loom-no-remap

	settings.depsHandler.addGlobal(this)

	if (isFabric) {
		settings.depsHandler.addFabric(this)
		implementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fapi")}")
		implementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	}

	if (isForge) {
		settings.depsHandler.addForge(this)
		"forge"("net.minecraftforge:forge:${mcVersion}-${property("deps.fml")}")
	}

	if (isNeo) {
		settings.depsHandler.addNeo(this)
		"neoForge"("net.neoforged:neoforge:${property("deps.fml")}")
		// Sodium (unobfuscated 26.x): Fabric jar をコンパイルに使う（クラスが flat で JarJar nesting なし）
		val sodiumCompile = when (mcVersion) {
			"26.1.1" -> "mc26.1.1-0.8.9-fabric"
			"26.2" -> "mc26.2-0.9.0-fabric"
			else -> null
		}
		if (sodiumCompile != null) {
			compileOnly("maven.modrinth:sodium:$sodiumCompile")
		}
	}

	vineflowerDecompilerClasspath("org.vineflower:vineflower:1.10.1")
}

// Loom config
loom {
	if (!isUnobfuscated) {
		try {
			accessWidenerPath.set(rootProject.file("src/main/resources/${mod.id}.accesswidener"))
		}
		catch (_: Exception) {
			println("Could not set accesswidener!")
		}
	}


	if (loader == "forge") forge {
		convertAccessWideners.set(true)
		mixinConfigs("mixins.${mod.id}.json")
	} else if (loader == "neoforge") neoForge {

	}

	runConfigs["client"].apply {
		ideConfigGenerated(true)
		vmArgs("-Dmixin.debug.export=true", "-Dsodium.checks.issue2561=false")
		programArgs("--username=nthxny") // Mom look I'm in the codebase!
		runDir = "../../run/${stonecutter.current.project}/"
	}

	decompilers {
		get("vineflower").apply {
			options.put("mark-corresponding-synthetics", "1")
		}
	}
}

// Tasks
tasks.withType<JavaCompile>() {
	options.compilerArgs.add("-Xplugin:Manifold")
	// modify the JavaCompile task and inject our auto-generated Manifold symbols
	if(!this.name.startsWith("_")) { // check the name, so we don't inject into Forge internal compilation
		ManifoldMC.setupPreprocessor(options.compilerArgs, loader, projectDir, mcVersion, stonecutter.active?.project == stonecutter.current.project, false)
	}
}

project.tasks.register("setupManifoldPreprocessors") {
	ManifoldMC.setupPreprocessor(ArrayList(), loader, projectDir, mcVersion, stonecutter.active?.project == stonecutter.current.project, true)
}

// Run Manifold preprocessor setup after stonecutter switches
tasks.matching { it.name == "setupChiseledBuild" }.configureEach { finalizedBy("setupManifoldPreprocessors") }
// Also run on first build if the task doesn't exist
tasks.named("compileJava") { dependsOn("setupManifoldPreprocessors") }

tasks.register<RenameExampleMod>("renameExampleMod", rootDir, mod.id, mod.name, mod.displayName, mod.namespace, mod.group).configure {
	group = "build helpers"
	description = "Renames the example mod to match the mod ID, name, and display name in gradle.properties"
}




val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
	group = "build"
	from(tasks.jar.get().archiveFile)
	into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))
	dependsOn("build")
}

if (stonecutter.current.isActive) {
	rootProject.tasks.register("buildActive") {
		group = "project"
		dependsOn(buildAndCollect)
	}

	rootProject.tasks.register("runActive") {
		group = "project"
		dependsOn(tasks.named("runClient"))
	}
}

// Resources
tasks.processResources {
//	inputs.property("version", mod.version)
//	inputs.property("mc", mod.mcDep)

	val map = mapOf(
		"version" to mod.version,
		"mc" to mod.mcDep,
		"id" to mod.id,
		"group" to mod.group,
		"author" to mod.author,
		"namespace" to mod.namespace,
		"description" to mod.description,
		"name" to mod.name,
		"license" to mod.license,
		"github" to mod.github,
		"display_name" to mod.displayName,
		"fml" to if (loader == "neoforge") "1" else "45",
		"mnd" to if (loader == "neoforge") "" else "mandatory = true"
	)

	filesMatching("fabric.mod.json") { expand(map) }
	filesMatching("META-INF/mods.toml") { expand(map) }
	filesMatching("META-INF/neoforge.mods.toml") { expand(map) }
}

stonecutter {
	val j25 = mcVersion.startsWith("26.")
	val j21 = if (j25) true else eval(mcVersion, ">=1.20.6")
	val jvmVer = if (j25) 25 else if (j21) 21 else 17

	java {
		withSourcesJar()
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(jvmVer))
		}
	}

	kotlin {
		jvmToolchain(jvmVer)
	}
}

// Publishing
publishMods {
	file = tasks.jar.get().archiveFile
	additionalFiles.from(tasks.named<Jar>("sourcesJar").get().archiveFile)
	displayName = "${mod.name} ${loader.replaceFirstChar { it.uppercase() }} ${mod.version} for ${property("mod.mc_title")}"
	version = mod.version
	changelog = rootProject.file("CHANGELOG.md").readText()
	type = STABLE
	modLoaders.add(loader)

	val targets = property("mod.mc_targets").toString().split(' ')

	dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null ||
			providers.environmentVariable("CURSEFORGE_TOKEN").getOrNull() == null

	modrinth {
		projectId = property("publish.modrinth").toString()
		accessToken = providers.environmentVariable("MODRINTH_TOKEN")
		targets.forEach(minecraftVersions::add)
		val deps = DependencyContainer(null, this)
 		settings.publishHandler.addModrinth(deps)
		settings.publishHandler.addShared(deps)
	}

	curseforge {
		projectId = property("publish.curseforge").toString()
		accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
		targets.forEach(minecraftVersions::add)
		val deps = DependencyContainer(this, null)
		settings.publishHandler.addCurseForge(deps)
		settings.publishHandler.addShared(deps)
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			groupId = "${property("mod.group")}.${mod.id}"
			artifactId = mod.version
			version = mcVersion

			from(components["java"])
		}
	}
}