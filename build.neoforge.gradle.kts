plugins {
    id("net.neoforged.moddev") version "2.0.144"
    id("neoforge-mutex")
    `maven-publish`
}

version = "${project.property("mod.version")}+${sc.current.version}"
base.archivesName = "${project.property("mod.id")}-neoforge"

val modId = project.property("mod.id").toString()

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }

    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.caffeinemc.net/releases", "CaffeineMC", "net.caffeinemc")
    strictMaven("https://maven.su5ed.dev/releases", "Sinytra", "org.sinytra.forgified-fabric-api")
    strictMaven("https://maven.fabricmc.net/", "FabricMC", "net.fabricmc.fabric-api")
}

val sodiumNestedPath = sc.properties.getOrNull<String>("deps.sodium_nested_path")
if (sodiumNestedPath != null) {
    dependencies.registerTransform(ExtractNestedModJar::class) {
        from.attribute(
            org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE,
        )
        to.attribute(
            org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            "sodium-nested-mod-jar",
        )
        parameters.nestedPath.set(sodiumNestedPath)
    }
}

dependencies {
    val embeddium = sc.properties.getOrNull<String>("deps.embeddium")
    val sodiumMaven = sc.properties.getOrNull<String>("deps.sodium_maven")

    when {
        embeddium != null -> implementation("maven.modrinth:embeddium:$embeddium")
        sodiumMaven != null -> {
            // Sodium 0.8+ publishes a lightweight outer mod that embeds its
            // implementation jar. Compile against the implementation, but run
            // with the outer distribution so NeoForge sees the intended layout.
            compileOnly("net.caffeinemc:sodium-neoforge-mod:$sodiumMaven")
            runtimeOnly("net.caffeinemc:sodium-neoforge:$sodiumMaven")
        }
        sodiumNestedPath != null -> {
            val sodium = "maven.modrinth:sodium:${project.property("deps.sodium")}"
            runtimeOnly(sodium)

            val outer = configurations.detachedConfiguration(create(sodium)).apply {
                isTransitive = false
            }
            val nested = outer.incoming.artifactView {
                attributes.attribute(
                    org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                    "sodium-nested-mod-jar",
                )
            }.files
            compileOnly(nested)
        }
        else -> implementation("maven.modrinth:sodium:${project.property("deps.sodium")}")
    }

    sc.properties.getOrNull<String>("deps.forgified_api_base")?.let {
        compileOnly("org.sinytra.forgified-fabric-api:fabric-api-base:$it")
    }
    sc.properties.getOrNull<String>("deps.forgified_renderer_api")?.let {
        compileOnly("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:$it")
    }
    sc.properties.getOrNull<String>("deps.fabric_renderer_api")?.let {
        // Sodium bundles the corresponding Forgified module at runtime. The
        // published Fabric API module exposes the same public API for javac.
        compileOnly("net.fabricmc.fabric-api:fabric-renderer-api-v1:$it")
    }

    val mixinExtras = "io.github.llamalad7:mixinextras-common:${project.property("deps.mixinextras")}"
    annotationProcessor(mixinExtras)
    compileOnly(mixinExtras)
}

neoForge {
    version = project.property("deps.neo_loader") as String

    mods {
        register(project.property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run/${sc.current.project}")
            jvmArguments.addAll(
                "-Dmixin.debug.export=true",
                "-Dsodium.checks.issue2561=false",
            )
            client()
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
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
            "license" to project.property("mod.license"),
            "github" to project.property("mod.github"),
            "minecraft" to project.property("mod.mc_compat"),
            "renderer" to project.property("mod.renderer_compat"),
            "java" to "JAVA_${requiredJava.majorVersion}",
            "java_version" to requiredJava.majorVersion,
        )

        inputs.properties(values)
        filesMatching("mixins.sodiumleafculling.json") { expand(values) }

        if (sc.current.version == "1.20.4") {
            // Exclude the Forge descriptor before renaming the NeoForge 1.20.4
            // descriptor to the legacy file name expected by that loader.
            filesMatching("META-INF/mods.toml") { exclude() }
            filesMatching("META-INF/neoforge-legacy.mods.toml") {
                expand(values)
                path = "META-INF/mods.toml"
            }
            exclude(
                "fabric.mod.json",
                "mixins.sodiumleafculling.forge.json",
                "META-INF/neoforge.mods.toml",
            )
        } else {
            filesMatching("META-INF/neoforge.mods.toml") { expand(values) }
            exclude(
                "fabric.mod.json",
                "mixins.sodiumleafculling.forge.json",
                "META-INF/mods.toml",
                "META-INF/neoforge-legacy.mods.toml",
            )
        }
    }

    jar {
        from(rootProject.file("LICENSE.md")) {
            rename("LICENSE\\.md", "LICENSE.md_$modId")
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds and collects the NeoForge jars for this target."
        inputs.property("version", project.property("mod.version"))
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.property("mod.group").toString()
            artifactId = "${project.property("mod.id")}-neoforge"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
