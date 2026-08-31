plugins {
    base
}

val nestedBuild = rootProject.layout.projectDirectory.dir("forge16-build")
val nestedWrapperJar = nestedBuild.file("gradle/wrapper/gradle-wrapper.jar")

// Stonecutter exposes the shared source set on every node. This bridge builds
// that source with ForgeGradle in the nested build instead of compiling it on
// the dependency-free bridge classpath.
tasks.withType<JavaCompile>().configureEach {
    enabled = false
}

val prepareForge16Wrapper by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.jar"))
    into(nestedBuild.dir("gradle/wrapper"))
}

val forge16Build by tasks.registering(JavaExec::class) {
    dependsOn("stonecutterGenerate", prepareForge16Wrapper)
    workingDir(nestedBuild)
    classpath(nestedWrapperJar)
    mainClass.set("org.gradle.wrapper.GradleWrapperMain")
    args("-PmodVersion=${project.property("mod.version")}", "reobfJar", "--no-daemon", "--max-workers=1")
}

tasks.named("build") {
    dependsOn(forge16Build)
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds Forge 1.16.5 with its Gradle 8-compatible toolchain and collects the jars."
    dependsOn(forge16Build)
    from(nestedBuild.dir("build/libs"))
    include("*.jar")
    exclude("*-sources.jar")
    into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}
