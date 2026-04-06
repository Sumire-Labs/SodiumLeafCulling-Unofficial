plugins {
    id("dev.kikugie.stonecutter")
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
    id("dev.architectury.loom") version "1.14.473" apply false
    id("dev.architectury.loom-no-remap") version "1.14.473" apply false
    id("me.modmuss50.mod-publish-plugin") apply false
    id("systems.manifold.manifold-gradle-plugin") version "0.0.2-alpha" apply false
}

stonecutter active "26.1.1-neoforge" /* [SC] DO NOT EDIT */

stonecutter parameters {
    // Manifold handles preprocessor constants via ManifoldMC.kt
}

stonecutter tasks {
    named("build")
    named("buildAndCollect")
    named("publishMods")
}
