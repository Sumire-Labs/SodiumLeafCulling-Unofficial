plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter flags {
    this["expose_properties"] = false
}

stonecutter active "26.2-fabric"

stonecutter parameters {
    val embeddiumVariant = current.project.endsWith("-neoforge-embeddium")
    val baseTarget = current.project.removeSuffix("-embeddium")
    val version = baseTarget.substringBeforeLast('-')
    val loader = baseTarget.substringAfterLast('-')

    properties {
        tags(version, if (embeddiumVariant) "neoforge_embeddium" else loader)
    }

    constants {
        match(loader, "fabric", "forge", "neoforge")
    }

    val embeddiumModern = current.project == "1.21-neoforge" || embeddiumVariant
    val embeddiumLegacy = loader != "fabric" && current.parsed < "1.21"
    val fabricLegacyRenderer = (loader == "fabric" && current.parsed < "1.20.1") ||
        (loader == "forge" && current.parsed < "1.20")

    // Sodium 0.8's public configuration API was backported to 1.21.1, then
    // became the regular API again in 1.21.11. Rendering internals changed at
    // the latter boundary only.
    constants["sodium_caffeine"] = current.parsed >= "1.21" && !embeddiumModern
    constants["sodium_modern_config"] = !embeddiumModern && (current.version == "1.21.1" || current.parsed >= "1.21.11")
    constants["sodium_modern_renderer"] = current.parsed >= "1.21.11"
    constants["sodium_chunk_layer"] = current.version == "1.21.11"
    constants["sodium_frapi_chunk_layer"] = current.parsed >= "1.21.6" && current.parsed < "1.21.11"
    // Embeddium 0.3 selects a terrain Material from the Forge/NeoForge
    // RenderType, unlike legacy Sodium's direct BlockState lookup.
    constants["embeddium"] = embeddiumLegacy
    constants["embeddium_modern"] = embeddiumModern
    constants["dual_renderer"] = current.project == "1.21.1-neoforge"
    constants["fabric_legacy_renderer"] = fabricLegacyRenderer
    constants["legacy_model_buffers"] = fabricLegacyRenderer && current.parsed < "1.17"
    constants["fabric_legacy_options"] = loader == "fabric" && current.parsed < "1.17"
    constants["legacy_component"] = current.parsed < "1.19"
    constants["string_options"] = loader != "neoforge" && current.parsed < "1.17"

    replacements {
        // Both implementations are packaged together. Keep their renderer-specific
        // helpers and mixins in distinct packages.
        string(embeddiumVariant) {
            replace("com.sumirelabs.sodiumleafculling", "com.sumirelabs.sodiumleafculling.embeddium")
        }
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}

val versionBuilds = stonecutter.tasks.names("build")
val versionCollections = stonecutter.tasks.names("buildAndCollect")
tasks.register("build") {
    group = "build"
    description = "Builds all registered targets."
    dependsOn(versionBuilds.map { it.values })
}
tasks.register("buildAndCollect") {
    group = "build"
    description = "Builds and collects runtime JARs for all registered targets."
    dependsOn(versionCollections.map { it.values })
}
