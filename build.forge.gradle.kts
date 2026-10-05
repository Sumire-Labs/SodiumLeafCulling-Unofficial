import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.taumc.moddev.legacyforge") version "2.0.147-tau.1"
    id("neoforge-mutex")
    id("com.gradleup.shadow") version "9.6.1" apply false
    `maven-publish`
}

version = "${sc.properties["mod.version"]}+${sc.current.version}"
base.archivesName = "${sc.properties["mod.id"]}-forge"

val modId = sc.properties["mod.id"]
val veryLegacy = sc.current.parsed < "1.17"
val legacyExtras = configurations.create("legacyMixinExtras")
if (veryLegacy) apply(plugin = "com.gradleup.shadow")

val requiredJava = when {
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    else -> JavaVersion.VERSION_1_8
}

repositories {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter { includeGroup("maven.modrinth") }
    }
}

// Forge 1.18.2 resolves Log4j API 2.19.0 from CoreMods while Minecraft still
// supplies Log4j Core 2.17.1. Keep the development runtime on a matching pair.
if (sc.current.version == "1.18.2") {
    configurations.configureEach {
        resolutionStrategy.force(
            "org.apache.logging.log4j:log4j-api:2.17.1",
            "org.apache.logging.log4j:log4j-core:2.17.1",
        )
    }
}

dependencies {
    val rendererFile = sc.properties.getOrNull("deps.embeddium_file")
        ?: sc.properties["deps.embeddium"]
    add("modImplementation", "maven.modrinth:embeddium:$rendererFile")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    val mixinExtrasVersion = sc.properties["deps.mixinextras"]
    val mixinExtrasCommon = "io.github.llamalad7:mixinextras-common:$mixinExtrasVersion"
    annotationProcessor(mixinExtrasCommon)
    compileOnly(mixinExtrasCommon)

    if (veryLegacy) {
        add(legacyExtras.name, mixinExtrasCommon)
        runtimeOnly(mixinExtrasCommon)
    } else {
        val mixinExtrasForge = implementation("io.github.llamalad7:mixinextras-forge:$mixinExtrasVersion")!!
        jarJar(mixinExtrasForge)
    }
}

legacyForge {
    enable {
        forgeVersion = sc.properties["deps.forge"]
        if (sc.current.parsed < "1.17") isUseMojangClassNames = true
        isObfuscateJar = !veryLegacy
        isDisableRecompilation = false
    }
    validateAccessTransformers = true

    mods {
        register(sc.properties["mod.id"]) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run/${sc.current.project}")
            jvmArgument("-Dmixin.debug.export=true")
            jvmArgument("-Dsodium.checks.issue2561=false")
            client()
        }
    }
}

mixin {
    add(sourceSets.main.get(), "mixins.sodiumleafculling.refmap.json")
    config("mixins.sodiumleafculling.forge.json")
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(if (requiredJava < JavaVersion.VERSION_21) 21 else requiredJava.majorVersion.toInt())
    }
}

val productionJar = if (veryLegacy) {
    tasks.named<Jar>("jar") { archiveClassifier.set("thin") }
    val shaded = tasks.named<ShadowJar>("shadowJar") {
        configurations = listOf(legacyExtras)
        archiveClassifier.set("")
        relocate("com.llamalad7.mixinextras", "toni.sodiumleafculling.shadow.mixinextras")
    }
    extensions.getByType<ObfuscationExtension>().reobfuscate(shaded, sourceSets.main.get()).also { artifact ->
        tasks.named("assemble") { dependsOn(artifact) }
    }
} else tasks.named<AbstractArchiveTask>("reobfJar")

tasks {
    named<CreateMinecraftArtifacts>("createMinecraftArtifacts") {
        dependsOn(sourceSets.main.get().allSource)
        additionalRepositories.add("https://maven.minecraftforge.net/")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(requiredJava.majorVersion.toInt())
    }

    processResources {
        val values = mapOf(
            "id" to sc.properties["mod.id"],
            "namespace" to sc.properties["mod.namespace"],
            "name" to sc.properties["mod.name"],
            "version" to sc.properties["mod.version"],
            "description" to sc.properties["mod.description"],
            "author" to sc.properties["mod.author"],
            "contributor" to sc.properties["mod.contributor"],
            "license" to sc.properties["mod.license"],
            "github" to sc.properties["mod.github"],
            "minecraft" to sc.properties["mod.mc_compat"],
            "renderer" to sc.properties["mod.renderer_compat"],
            "loader" to sc.properties["mod.loader_compat"],
            "pack_format" to sc.properties["mod.pack_format"],
            "java" to "JAVA_${requiredJava.majorVersion}",
            "java_version" to requiredJava.majorVersion,
            "plugin_line" to if (veryLegacy) "\"plugin\": \"toni.sodiumleafculling.LeafCullingMixinPlugin\"," else "",
        )

        inputs.properties(values)
        filesMatching("META-INF/mods.toml") { expand(values) }
        filesMatching("pack.mcmeta") { expand(values) }
        filesMatching("mixins.sodiumleafculling.forge.json") { expand(values) }
        exclude(
            "fabric.mod.json",
            "mixins.sodiumleafculling.json",
            "META-INF/neoforge-legacy.mods.toml",
            "META-INF/neoforge.mods.toml",
            "META-INF/neoforge-early.mods.toml",
        )
    }

    jar {
        manifest {
            attributes["MixinConfigs"] = "mixins.sodiumleafculling.forge.json"
        }

        from(rootProject.file("LICENSE.md")) {
            rename("LICENSE\\.md", "LICENSE.md_$modId")
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds and collects the Forge jars for this target."
        inputs.property("version", sc.properties["mod.version"])
        from(productionJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${sc.properties["mod.version"]}"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = sc.properties["mod.group"]
            artifactId = "${sc.properties["mod.id"]}-forge"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
