plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.144"
    id("neoforge-mutex")
    `maven-publish`
}

version = "${project.property("mod.version")}+${sc.current.version}"
base.archivesName = "${project.property("mod.id")}-forge"

val modId = project.property("mod.id").toString()

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
    add("modImplementation", "maven.modrinth:embeddium:${project.property("deps.embeddium")}")

    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    val mixinExtrasVersion = project.property("deps.mixinextras").toString()
    val mixinExtrasCommon = "io.github.llamalad7:mixinextras-common:$mixinExtrasVersion"
    annotationProcessor(mixinExtrasCommon)
    compileOnly(mixinExtrasCommon)

    val mixinExtrasForge = implementation("io.github.llamalad7:mixinextras-forge:$mixinExtrasVersion")!!
    jarJar(mixinExtrasForge)
}

legacyForge {
    version = project.property("deps.forge") as String
    validateAccessTransformers = true

    mods {
        register(project.property("mod.id") as String) {
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

tasks {
    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(requiredJava.majorVersion.toInt())
    }

    processResources {
        val values = mapOf(
            "id" to project.property("mod.id"),
            "namespace" to project.property("mod.namespace"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "description" to project.property("mod.description"),
            "author" to project.property("mod.author"),
            "contributor" to project.property("mod.contributor"),
            "license" to project.property("mod.license"),
            "github" to project.property("mod.github"),
            "minecraft" to project.property("mod.mc_compat"),
            "renderer" to project.property("mod.renderer_compat"),
            "loader" to project.property("mod.loader_compat"),
            "pack_format" to project.property("mod.pack_format"),
            "java" to "JAVA_${requiredJava.majorVersion}",
            "java_version" to requiredJava.majorVersion,
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
        inputs.property("version", project.property("mod.version"))
        from(named<Jar>("reobfJar").flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.property("mod.group").toString()
            artifactId = "${project.property("mod.id")}-forge"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
