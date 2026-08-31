plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "forge", "neoforge")
    }

    val embeddiumModern = current.project == "1.21-neoforge"
    val embeddiumLegacy = loader != "fabric" && current.parsed < "1.21"
    val fabricLegacyRenderer = (loader == "fabric" && current.parsed < "1.20.2") ||
        (loader == "forge" && current.parsed < "1.20")

    // Sodium 0.8's public configuration API was backported to 1.21.1, then
    // became the regular API again in 1.21.11. Rendering internals changed at
    // the latter boundary only.
    constants["sodium_caffeine"] = current.parsed >= "1.21" && !embeddiumModern
    constants["sodium_modern_config"] = current.version == "1.21.1" || current.parsed >= "1.21.11"
    constants["sodium_modern_renderer"] = current.parsed >= "1.21.11"
    constants["sodium_chunk_layer"] = current.version == "1.21.11"
    constants["sodium_frapi_chunk_layer"] = current.parsed >= "1.21.6" && current.parsed < "1.21.11"
    // Embeddium 0.3 selects a terrain Material from the Forge/NeoForge
    // RenderType, unlike legacy Sodium's direct BlockState lookup.
    constants["embeddium"] = embeddiumLegacy
    constants["embeddium_modern"] = embeddiumModern
    constants["fabric_legacy_renderer"] = fabricLegacyRenderer
    constants["legacy_model_buffers"] = fabricLegacyRenderer && current.parsed < "1.17"
    constants["fabric_legacy_options"] = loader == "fabric" && current.parsed < "1.17"
    constants["legacy_component"] = current.parsed < "1.19"
    constants["string_options"] = loader == "fabric" && current.parsed < "1.17"

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}

stonecutter tasks {
    named("build")
    named("buildAndCollect")
}
