val settings = object : TxniTemplateSettings {

	// -------------------- Dependencies ---------------------- //
	override val depsHandler: DependencyHandler get() = object : DependencyHandler {
		override fun addGlobal(deps: DependencyHandlerScope) {

		}

		override fun addFabric(deps: DependencyHandlerScope) {
			deps.include(deps.implementation(deps.annotationProcessor("com.bawnorton.mixinsquared:mixinsquared-fabric:0.2.0-beta.6")!!)!!)

			val sodiumSlug = when (mcVersion) {
				"1.21.11" -> "mc1.21.11-0.8.7-fabric"
				"1.21.4" -> "mc1.21.4-0.6.13-fabric"
				"1.21.1" -> "mc1.21.1-0.8.12-beta.2-fabric"
				"1.20.4" -> "mc1.20.4-0.5.8"
				else -> "mc1.20.1-0.5.13-fabric"
			}
			deps.modImplementation(modrinth("sodium", sodiumSlug))

			if (mcVersion == "1.20.1") {
				deps.modImplementation(modrinth("indium", "1.0.34+mc1.20.1"))
			}
		}

		override fun addForge(deps: DependencyHandlerScope) {
			deps.modImplementation(modrinth("embeddium", "0.3.31+mc1.20.1"))
			deps.compileOnly(deps.annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")!!)
			deps.include(deps.implementation("io.github.llamalad7:mixinextras-forge:0.3.5")!!)
		}

		override fun addNeo(deps: DependencyHandlerScope) {
			val sodiumSlug = when (mcVersion) {
				"1.21.11" -> "mc1.21.11-0.8.7-neoforge"
				"1.21.4" -> "mc1.21.4-0.6.13-neoforge"
				"1.21.1" -> "mc1.21.1-0.8.12-beta.2-neoforge"
				"1.20.4" -> null // Use Embeddium for 1.20.4 NeoForge
				else -> null
			}
			if (sodiumSlug != null) {
				if (mcVersion == "1.21.1") {
					// Sodium 0.8.x ships its real classes as a Jar-in-Jar; modImplementation makes
					// Loom extract the nested mod and put the actual classes on the classpath.
					deps.modImplementation(modrinth("sodium", sodiumSlug))
				} else {
					deps.implementation(modrinth("sodium", sodiumSlug))
				}
			}
			if (mcVersion == "1.20.4") {
				deps.modImplementation(modrinth("embeddium", "0.3.25+mc1.20.4"))
			}
			if (mcVersion == "1.21.1") {
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
	id("dev.kikugie.j52j") version "1.0"
	id("dev.architectury.loom")
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

val loader = loom.platform.get().name.lowercase()
val isFabric = loader == "fabric"
val isForge = loader == "forge"
val isNeo = loader == "neoforge"

// Fabric は Sodium が mod id "sodiumleafculling" を breaks 指定しているため、Fabric のみ別 id にして回避する。
// NeoForge/Forge は mod id にハイフンを使えず、かつ Sodium の breaks 対象外なので元の id を維持する。
val modId = if (isFabric) "${mod.id}-unofficial" else mod.id

// 1.21.1 は Sodium 0.8 対応の大規模変更のため 2.0.0 にバンプ（他バージョンは mod.version のまま）
val modVersion = if (mcVersion == "1.21.1") "2.0.0" else mod.version

version = "$modVersion-$mcVersion"
group = mod.group
base { archivesName.set("${modId}-$loader") }

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

	@Suppress("UnstableApiUsage")
	mappings(loom.layered {
		officialMojangMappings()
		val parchmentVersion = when (mcVersion) {
			"1.18.2" -> "1.18.2:2022.11.06"
			"1.19.2" -> "1.19.2:2022.11.27"
			"1.20.1" -> "1.20.1:2023.09.03"
			"1.20.4" -> "1.20.4:2024.04.14"
			"1.21.1" -> "1.21:2024.07.28"
			"1.21.4" -> "1.21.4:2024.12.07"
			"1.21.11" -> "1.21.4:2024.12.07" // Use 1.21.4 parchment as fallback
			else -> ""
		}
		if (parchmentVersion.isNotEmpty()) {
			parchment("org.parchmentmc.data:parchment-$parchmentVersion@zip")
		}
	})

	settings.depsHandler.addGlobal(this)

	if (isFabric) {
		settings.depsHandler.addFabric(this)
		modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fapi")}")
		modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	}

	if (isForge) {
		settings.depsHandler.addForge(this)
		"forge"("net.minecraftforge:forge:${mcVersion}-${property("deps.fml")}")
	}

	if (isNeo) {
		settings.depsHandler.addNeo(this)
		"neoForge"("net.neoforged:neoforge:${property("deps.fml")}")
	}

	vineflowerDecompilerClasspath("org.vineflower:vineflower:1.10.1")
}

// NeoForge distributes Sodium 0.8.x as a thin bootstrap jar with the real mod bundled as a
// Jar-in-Jar. Loom remaps only the outer jar and does not expose nested NeoForge jars to the
// compile classpath, so extract the nested mod jar ourselves and add it as compileOnly.
if (isNeo && mcVersion == "1.21.1") {
	val sodiumNeoOuter = configurations.detachedConfiguration(
		dependencies.create("maven.modrinth:sodium:mc1.21.1-0.8.12-beta.2-neoforge")
	).apply { isTransitive = false }
	dependencies.add("compileOnly", zipTree(sodiumNeoOuter.singleFile).matching {
		include("META-INF/jarjar/net.caffeinemc.sodium-neoforge-*.jar")
	})
}

// Loom config
loom {
	try {
		accessWidenerPath.set(rootProject.file("src/main/resources/${mod.namespace}.accesswidener"))
	}
	catch (_: Exception) {
		println("Could not set accesswidener!")
	}


	if (loader == "forge") forge {
		convertAccessWideners.set(true)
		mixinConfigs("mixins.${mod.namespace}.json")
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
		ManifoldMC.setupPreprocessor(options.compilerArgs, loader, projectDir, mcVersion, stonecutter.active.project == stonecutter.current.project, false)
	}
}

project.tasks.register("setupManifoldPreprocessors") {
	ManifoldMC.setupPreprocessor(ArrayList(), loader, projectDir, mcVersion, stonecutter.active.project == stonecutter.current.project, true)
}

tasks.setupChiseledBuild { finalizedBy("setupManifoldPreprocessors") }

tasks.register<RenameExampleMod>("renameExampleMod", rootDir, mod.id, mod.name, mod.displayName, mod.namespace, mod.group).configure {
	group = "build helpers"
	description = "Renames the example mod to match the mod ID, name, and display name in gradle.properties"
}




val buildAndCollect = tasks.register<Copy>("buildAndCollect") {
	group = "build"
	from(tasks.remapJar.get().archiveFile)
	into(rootProject.layout.buildDirectory.file("libs/${modVersion}"))
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
	val map = mapOf(
		"version" to modVersion,
		"mc" to mod.mcDep,
		"id" to modId,
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

	// メタデータ(version/name/display_name 等)を入力として追跡する。
	// これが無いと値を変えても processResources が up-to-date でスキップされ、jar に古い値が残る。
	inputs.properties(map)

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
	file = tasks.remapJar.get().archiveFile
	additionalFiles.from(tasks.remapSourcesJar.get().archiveFile)
	displayName = "${mod.name} ${loader.replaceFirstChar { it.uppercase() }} ${modVersion} for ${property("mod.mc_title")}"
	version = modVersion
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
			artifactId = modVersion
			version = mcVersion

			from(components["java"])
		}
	}
}