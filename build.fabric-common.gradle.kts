import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.language.jvm.tasks.ProcessResources

val sc = extensions.getByType<StonecutterBuildExtension>()
val artifactName = if (sc.current.parsed >= "26.1") "jar" else "remapJar"
val modDependency = if (sc.current.parsed >= "26.1") "implementation" else "modImplementation"

version = "${project.property("mod.version")}+${sc.current.version}"
configure<BasePluginExtension> { archivesName = "${project.property("mod.fabric_id")}-fabric" }

val modId = project.property("mod.fabric_id").toString()

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    sc.current.parsed >= "1.16" -> JavaVersion.VERSION_1_8
    else -> JavaVersion.VERSION_17
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }

    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    strictMaven("https://maven.bawnorton.com/releases", "Bawnorton", "com.github.bawnorton.mixinsquared")
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${sc.current.version}")

    add(modDependency, "net.fabricmc:fabric-loader:${project.property("deps.fabric_loader")}")
    add(modDependency, "net.fabricmc.fabric-api:fabric-api:${project.property("deps.fabric_api")}")
    add(modDependency, "maven.modrinth:sodium:${project.property("deps.sodium")}")

    // Loom does not expose non-mod JARs nested in legacy Sodium releases to the
    // development runtime. Production Sodium JARs already bundle this library.
    sc.properties.getOrNull<String>("deps.sodium_nested_joml")?.let {
        add("runtimeOnly", "org.joml:joml:$it")
    }

    sc.properties.getOrNull<String>("deps.indium")?.let {
        add(modDependency, "maven.modrinth:indium:$it")
    }

    val mixinSquared = "com.github.bawnorton.mixinsquared:mixinsquared-fabric:${project.property("deps.mixinsquared")}"
    add("annotationProcessor", mixinSquared)
    add(modDependency, mixinSquared)
    add("include", mixinSquared)

    val mixinExtras = "io.github.llamalad7:mixinextras-common:${project.property("deps.mixinextras")}"
    add("annotationProcessor", mixinExtras)
    add("compileOnly", mixinExtras)
}


configure<JavaPluginExtension> {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(if (requiredJava < JavaVersion.VERSION_21) 21 else requiredJava.majorVersion.toInt())
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(requiredJava.majorVersion.toInt())
    }

    named<ProcessResources>("processResources") {
        val values = mapOf(
            "id" to project.property("mod.fabric_id"),
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
            "fabric_api_id" to (sc.properties.getOrNull<String>("deps.fabric_api_id") ?: "fabric-api"),
            "java" to "JAVA_${requiredJava.majorVersion}",
            "java_version" to requiredJava.majorVersion,
        )

        inputs.properties(values)
        filesMatching("fabric.mod.json") { expand(values) }
        filesMatching("mixins.sodiumleafculling.json") { expand(values) }
        exclude(
            "mixins.sodiumleafculling.forge.json",
            "META-INF/mods.toml",
            "META-INF/neoforge-early.mods.toml",
            "META-INF/neoforge-legacy.mods.toml",
            "META-INF/neoforge.mods.toml",
            "pack.mcmeta",
        )
    }

    named<Jar>("jar") {
        from(rootProject.file("LICENSE.md")) {
            rename("LICENSE\\.md", "LICENSE.md_$modId")
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds and collects the Fabric jars for this target."
        inputs.property("version", project.property("mod.version"))
        from(named<AbstractArchiveTask>(artifactName).flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
    }
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.property("mod.group").toString()
            artifactId = "${project.property("mod.fabric_id")}-fabric"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
