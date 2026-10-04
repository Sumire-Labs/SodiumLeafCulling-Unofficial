import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
}

// The 1.20.2/1.20.3 bridges feed these resources into a separate NeoGradle
// build. Expand the shared templates before handing them to that build.
tasks.named<ProcessResources>("processResources") {
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
        "renderer_id" to "embeddium",
        "java" to "JAVA_17",
        "java_version" to 17,
    )
    inputs.properties(values)
    filesMatching("mixins.sodiumleafculling.json") { expand(values) }
    filesMatching("META-INF/mods.toml") { exclude() }
    filesMatching("META-INF/neoforge-early.mods.toml") {
        expand(values)
        path = "META-INF/mods.toml"
    }
    exclude(
        "fabric.mod.json",
        "mixins.sodiumleafculling.forge.json",
        "META-INF/neoforge-legacy.mods.toml",
        "META-INF/neoforge.mods.toml",
        "pack.mcmeta",
    )
}
