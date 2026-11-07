import org.gradle.language.jvm.tasks.ProcessResources
import dev.kikugie.stonecutter.build.StonecutterBuildExtension

plugins {
    java
}

// Both NeoForge backends consume the same shared metadata and Mixin templates.
tasks.named<ProcessResources>("processResources") {
    val stonecutter = project.extensions.getByType<StonecutterBuildExtension>()
    val metadata = stonecutter.properties
    val dualRenderer = stonecutter.current.project == "1.21.1-neoforge"
    val rootLogo = dualRenderer || stonecutter.current.project == "1.21-neoforge"
    val logoPath = "assets/${metadata["mod.namespace"]}/textures/mod_logo.png"
    val gameVersion = metadata["mod.mc_compat"].trim('[', ']')
    val numbers = gameVersion.split('.').map(String::toInt)
    val javaVersion = when {
        numbers[0] >= 26 -> 25
        numbers[1] > 20 || numbers[1] == 20 && numbers.getOrElse(2) { 0 } >= 5 -> 21
        else -> 17
    }
    val values = mapOf(
        "id" to metadata["mod.id"],
        "namespace" to metadata["mod.namespace"],
        "logo_path" to if (rootLogo) "logo.png" else logoPath,
        "name" to metadata["mod.name"],
        "version" to metadata["mod.version"],
        "description" to metadata["mod.description"],
        "author" to metadata["mod.author"],
        "contributor" to metadata["mod.contributor"],
        "license" to metadata["mod.license"],
        "github" to metadata["mod.github"],
        "minecraft" to metadata["mod.mc_compat"],
        "renderer" to metadata["mod.renderer_compat"],
        "renderer_id" to if ("deps.embeddium" in metadata) "embeddium" else "sodium",
        "renderer_dependency_type" to if (dualRenderer) "optional" else "required",
        "mixin_config" to if (dualRenderer) "mixins.sodiumleafculling.compat.sodium.json" else "mixins.${metadata["mod.namespace"]}.json",
        "extra_mixin_config" to if (dualRenderer) "[[mixins]]\nconfig=\"mixins.sodiumleafculling.compat.embeddium.json\"" else "",
        "extra_renderer_dependency" to if (dualRenderer) """
            [[dependencies."${metadata["mod.id"]}"]]
            modId="embeddium"
            type="optional"
            versionRange="[1.0.15+mc1.21.1,)"
            ordering="AFTER"
            side="CLIENT"
        """.trimIndent() else "",
        "java" to "JAVA_$javaVersion",
        "java_version" to javaVersion,
        "renderer_context_mixin" to if ("deps.embeddium" !in metadata &&
            (numbers[0] >= 26 || numbers[1] >= 21)) "\"AbstractBlockRenderContextAccessor\"," else "",
        "icon_line" to if (javaVersion == 25) "iconFile=\"assets/${metadata["mod.namespace"]}/textures/mod_logo.png\"" else "",
    )
    inputs.properties(values)
    if (rootLogo) {
        // Embeddium's custom tab header requests the logo as one root path segment.
        from(rootProject.file("src/main/resources/$logoPath")) {
            rename { "logo.png" }
        }
    }
    filesMatching("mixins.sodiumleafculling.json") {
        if (dualRenderer) exclude() else expand(values)
    }
    if (dualRenderer) {
        for ((renderer, mixinPackage) in mapOf(
            "sodium" to "com.sumirelabs.sodiumleafculling.mixins",
            "embeddium" to "com.sumirelabs.sodiumleafculling.embeddium.mixins",
        )) {
            from(rootProject.file("src/compat/renderer.mixins.json")) {
                rename { "mixins.sodiumleafculling.compat.$renderer.json" }
                expand("mixin_package" to mixinPackage)
            }
        }
    }
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
