plugins {
    id("org.taumc.moddev") version "2.0.147-tau.1"
    id("neoforge-mutex")
    id("neoforge-resources")
    `maven-publish`
}

version = "${sc.properties["mod.version"]}+${sc.current.version}"
base.archivesName = "${sc.properties["mod.id"]}-neoforge"

val modId = sc.properties["mod.id"]

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

val sodiumNestedPath = sc.properties.getOrNull("deps.sodium_nested_path")
val sodiumMaven = sc.properties.getOrNull("deps.sodium_maven")
val sodiumShell = configurations.create("sodiumShell") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}
val strippedSodiumShell = sodiumMaven?.let { version ->
    tasks.register<Jar>("stripSodiumShell") {
        archiveBaseName.set("sodium-neoforge-service")
        archiveVersion.set(version.replace('+', '-'))
        destinationDirectory.set(layout.buildDirectory.dir("sodium-shell-lib"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from({ sodiumShell.map(::zipTree) })
        exclude(
            "META-INF/MANIFEST.MF",
            "META-INF/neoforge.mods.toml",
            "META-INF/jarjar/**",
            "META-INF/services/net.neoforged.neoforgespi.locating.IModFileCandidateLocator",
        )
        manifest {
            attributes(
                "FMLModType" to "LIBRARY",
                "Automatic-Module-Name" to "sodium_service",
            )
        }
    }
}

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
    val embeddium = sc.properties.getOrNull("deps.embeddium")
    when {
        embeddium != null -> implementation("maven.modrinth:embeddium:${sc.properties.getOrNull("deps.embeddium_file") ?: embeddium}")
        sodiumMaven != null -> {
            // The published shell claims the sodium mod id and embeds the real
            // mod. That layout is correct for a launcher, but shadows the direct
            // implementation on ModDevGradle's development classpath. Run with
            // the implementation plus a metadata-free service shell instead.
            implementation("net.caffeinemc:sodium-neoforge-mod:$sodiumMaven")
            add(sodiumShell.name, "net.caffeinemc:sodium-neoforge:$sodiumMaven")
            runtimeOnly(files(strippedSodiumShell))
        }
        sodiumNestedPath != null -> {
            val sodium = "maven.modrinth:sodium:${sc.properties["deps.sodium"]}"
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
        else -> implementation("maven.modrinth:sodium:${sc.properties["deps.sodium"]}")
    }

    sc.properties.getOrNull("deps.forgified_api_base")?.let {
        compileOnly("org.sinytra.forgified-fabric-api:fabric-api-base:$it")
        if (sodiumMaven != null) {
            runtimeOnly("org.sinytra.forgified-fabric-api:fabric-api-base:$it")
        }
    }
    sc.properties.getOrNull("deps.forgified_renderer_api")?.let {
        compileOnly("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:$it")
        if (sodiumMaven != null) {
            runtimeOnly("org.sinytra.forgified-fabric-api:fabric-renderer-api-v1:$it")
        }
    }
    sc.properties.getOrNull("deps.forgified_rendering_data_attachment")?.let {
        runtimeOnly("org.sinytra.forgified-fabric-api:fabric-rendering-data-attachment-v1:$it")
    }
    sc.properties.getOrNull("deps.forgified_block_view_api")?.let {
        runtimeOnly("org.sinytra.forgified-fabric-api:fabric-block-view-api-v2:$it")
    }
    sc.properties.getOrNull("deps.fabric_renderer_api")?.let {
        // Sodium bundles the corresponding Forgified module at runtime. The
        // published Fabric API module exposes the same public API for javac.
        compileOnly("net.fabricmc.fabric-api:fabric-renderer-api-v1:$it")
    }

    val mixinExtras = "io.github.llamalad7:mixinextras-common:${sc.properties["deps.mixinextras"]}"
    annotationProcessor(mixinExtras)
    compileOnly(mixinExtras)
}

neoForge {
    version = sc.properties["deps.neo_loader"]

    mods {
        register(sc.properties["mod.id"]) {
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
        languageVersion = JavaLanguageVersion.of(if (requiredJava < JavaVersion.VERSION_21) 21 else requiredJava.majorVersion.toInt())
    }
}

tasks {
    named("createMinecraftArtifacts") {
        dependsOn(sourceSets.main.get().allSource)
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(requiredJava.majorVersion.toInt())
    }

    jar {
        from(rootProject.file("LICENSE.md")) {
            rename("LICENSE\\.md", "LICENSE.md_$modId")
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds and collects the NeoForge jars for this target."
        inputs.property("version", sc.properties["mod.version"])
        from(jar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${sc.properties["mod.version"]}"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = sc.properties["mod.group"]
            artifactId = "${sc.properties["mod.id"]}-neoforge"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
