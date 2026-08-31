plugins {
    base
}

val nestedBuild = rootProject.layout.projectDirectory.dir("neoforge205-build")
val nestedWrapperJar = nestedBuild.file("gradle/wrapper/gradle-wrapper.jar")

val prepareNeoForge205Wrapper by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.jar"))
    into(nestedBuild.dir("gradle/wrapper"))
}

val neoForge205Build by tasks.registering(JavaExec::class) {
    dependsOn("stonecutterGenerate", prepareNeoForge205Wrapper)
    workingDir(nestedBuild)
    classpath(nestedWrapperJar)
    mainClass.set("org.gradle.wrapper.GradleWrapperMain")
    args("jar", "--no-daemon", "--max-workers=1")
}

tasks.named("build") {
    dependsOn(neoForge205Build)
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds NeoForge 1.20.5 with NeoGradle and collects the jars."
    dependsOn(neoForge205Build)
    from(nestedBuild.dir("build/libs"))
    include("*.jar")
    exclude("*-sources.jar")
    into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}
