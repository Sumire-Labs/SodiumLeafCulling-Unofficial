import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
}

// Both NeoForge backends consume the same shared metadata and Mixin templates.
tasks.named<ProcessResources>("processResources") {
    val gameVersion = project.property("mod.mc_compat").toString().trim('[', ']')
    val numbers = gameVersion.split('.').map(String::toInt)
    val javaVersion = when {
        numbers[0] >= 26 -> 25
        numbers[1] > 20 || numbers[1] == 20 && numbers.getOrElse(2) { 0 } >= 5 -> 21
        else -> 17
    }
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
        "renderer_id" to if (project.hasProperty("deps.embeddium")) "embeddium" else "sodium",
        "java" to "JAVA_$javaVersion",
        "java_version" to javaVersion,
        "icon_line" to if (javaVersion == 25) "iconFile=\"assets/${project.property("mod.namespace")}/textures/mod_logo.png\"" else "",
    )
    inputs.properties(values)
    filesMatching("mixins.sodiumleafculling.json") { expand(values) }
    val legacyDescriptor = numbers[0] == 1 && numbers[1] == 20 && numbers[2] <= 4
    val descriptor = when {
        legacyDescriptor && numbers[2] <= 3 -> "META-INF/neoforge-early.mods.toml"
        legacyDescriptor -> "META-INF/neoforge-legacy.mods.toml"
        else -> "META-INF/neoforge.mods.toml"
    }
    filesMatching("META-INF/mods.toml") { exclude() }
    filesMatching(descriptor) {
        expand(values)
        if (legacyDescriptor) path = "META-INF/mods.toml"
    }
    exclude(
        "fabric.mod.json",
        "mixins.sodiumleafculling.forge.json",
        "pack.mcmeta",
    )
    listOf("META-INF/neoforge-early.mods.toml", "META-INF/neoforge-legacy.mods.toml", "META-INF/neoforge.mods.toml")
        .filter { it != descriptor }.forEach { exclude(it) }
}
