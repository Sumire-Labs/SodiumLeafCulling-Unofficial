plugins {
    id("net.fabricmc.fabric-loom")
    `maven-publish`
}
apply(from = rootProject.file("build.fabric-common.gradle.kts"))

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run/${project.name}")
        jvmArguments.addAll(
            "-Dmixin.debug.export=true",
            "-Dsodium.checks.issue2561=false",
        )
    }
}
