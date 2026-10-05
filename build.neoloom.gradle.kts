import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import net.fabricmc.loom.api.mappings.layered.MappingsNamespace
import net.fabricmc.loom.api.processor.MinecraftJarProcessor
import net.fabricmc.loom.api.processor.ProcessorContext
import net.fabricmc.loom.api.processor.SpecContext
import net.fabricmc.tinyremapper.OutputConsumerPath

// NeoLoom 1.18.5's Forge 1.20.2 binary input retains SRG member names even
// though its raw-jar namespace is marked Mojang. Repair the development jar;
// the normal remapJar task still converts our mod back to production names.
open class ForgeSrgMembers : MinecraftJarProcessor<ForgeSrgMembers.Spec> {
    data class Spec(val revision: Int = 1) : MinecraftJarProcessor.Spec
    override fun getName() = "forge-srg-members"
    override fun buildSpec(context: SpecContext) = Spec()
    override fun processJar(jar: Path, spec: Spec, context: ProcessorContext) {
        val output = Files.createTempFile(jar.parent, "named-members-", ".jar")
        Files.delete(output)
        try {
            context.createRemapper(MappingsNamespace.SRG, MappingsNamespace.NAMED).use { lazy ->
                val remapper = lazy.get()
                OutputConsumerPath.Builder(output).build().use { consumer ->
                    consumer.addNonClassFiles(jar)
                    remapper.readInputs(jar)
                    remapper.apply(consumer)
                }
            }
            Files.move(output, jar, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(output) }
    }
}

plugins {
    id("org.relativitymc.neo-loom-remap") version "1.18.5"
    `maven-publish`
}

version = "${sc.properties["mod.version"]}+${sc.current.version}"
base.archivesName = "${sc.properties["mod.id"]}-forge"

repositories {
    maven("https://maven.minecraftforge.net/")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings(loom.officialMojangMappings())
    add("forgeUserdev", "net.minecraftforge:forge:${sc.properties["deps.forge"]}:userdev")
    modImplementation("maven.modrinth:embeddium:${sc.properties["deps.embeddium_file"]}")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    val extras = sc.properties["deps.mixinextras"]
    annotationProcessor("io.github.llamalad7:mixinextras-common:$extras")
    compileOnly("io.github.llamalad7:mixinextras-common:$extras")
    include("io.github.llamalad7:mixinextras-forge:$extras")
    modImplementation("io.github.llamalad7:mixinextras-forge:$extras")
}

loom {
    useIntermediateMappings.set(true)
    forgeExtraMixinConfigs.add("mixins.sodiumleafculling.forge.json")
    addMinecraftJarProcessor(ForgeSrgMembers::class.java)
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName.set("mixins.sodiumleafculling.refmap.json")
    }
    runConfigs.all { runDirectory = rootProject.file("run/${sc.current.project}") }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        vendor.set(JvmVendorSpec.ADOPTIUM)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.processResources {
    val values = mapOf(
        "id" to sc.properties["mod.id"], "namespace" to sc.properties["mod.namespace"],
        "name" to sc.properties["mod.name"], "version" to sc.properties["mod.version"],
        "description" to sc.properties["mod.description"], "author" to sc.properties["mod.author"],
        "contributor" to sc.properties["mod.contributor"], "license" to sc.properties["mod.license"],
        "github" to sc.properties["mod.github"], "minecraft" to sc.properties["mod.mc_compat"],
        "renderer" to sc.properties["mod.renderer_compat"], "loader" to sc.properties["mod.loader_compat"],
        "pack_format" to sc.properties["mod.pack_format"], "java" to "JAVA_17", "java_version" to 17,
        "plugin_line" to "",
    )
    inputs.properties(values)
    filesMatching("META-INF/mods.toml") { expand(values) }
    filesMatching("pack.mcmeta") { expand(values) }
    filesMatching("mixins.sodiumleafculling.forge.json") { expand(values) }
    exclude("fabric.mod.json", "mixins.sodiumleafculling.json", "META-INF/neoforge-early.mods.toml",
        "META-INF/neoforge-legacy.mods.toml", "META-INF/neoforge.mods.toml")
}
tasks.jar {
    manifest.attributes["MixinConfigs"] = "mixins.sodiumleafculling.forge.json"
    from(rootProject.file("LICENSE.md")) { rename("LICENSE\\.md", "LICENSE.md_slc_unofficial") }
}
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/${sc.properties["mod.version"]}"))
}
