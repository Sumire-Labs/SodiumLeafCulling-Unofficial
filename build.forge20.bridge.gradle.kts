plugins {
    base
}

val nestedBuild = rootProject.layout.projectDirectory.dir("forge20-build")
val nestedWrapperJar = nestedBuild.file("gradle/wrapper/gradle-wrapper.jar")

tasks.withType<JavaCompile>().configureEach {
    enabled = false
}

val prepareForge20Wrapper by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.jar"))
    into(nestedBuild.dir("gradle/wrapper"))
}

val forge20Build by tasks.registering(JavaExec::class) {
    dependsOn("stonecutterGenerate", prepareForge20Wrapper)
    // Gradle 8 / legacy loader plugins need their own Java 21 launcher even
    // when the root build runs on Java 25 for modern Loom.
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    })
    workingDir(nestedBuild)
    classpath(nestedWrapperJar)
    mainClass.set("org.gradle.wrapper.GradleWrapperMain")
    args("-PmodVersion=${project.property("mod.version")}", "reobfJar", "--no-daemon", "--max-workers=1")
}

tasks.named("build") {
    dependsOn(forge20Build)
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds Forge 1.20.2 with ForgeGradle and collects the jars."
    dependsOn(forge20Build)
    from(nestedBuild.dir("build/libs"))
    include("${project.property("mod.id")}-forge-${project.property("mod.version")}+${sc.current.version}.jar")
    exclude("*-sources.jar")
    into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}
