plugins {
    base
}

val nestedBuild = rootProject.layout.projectDirectory.dir("neoforge202-build")
val nestedWrapperJar = nestedBuild.file("gradle/wrapper/gradle-wrapper.jar")

tasks.withType<JavaCompile>().configureEach {
    enabled = false
}

val prepareNeoForge202Wrapper by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.jar"))
    into(nestedBuild.dir("gradle/wrapper"))
}

val neoForge202Build by tasks.registering(JavaExec::class) {
    dependsOn("stonecutterGenerate", prepareNeoForge202Wrapper)
    workingDir(nestedBuild)
    classpath(nestedWrapperJar)
    mainClass.set("org.gradle.wrapper.GradleWrapperMain")
    args("build", "--no-daemon", "--max-workers=1")
}

tasks.named("build") {
    dependsOn(neoForge202Build)
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn(neoForge202Build)
    from(nestedBuild.dir("build/libs"))
    include("*.jar")
    exclude("*-sources.jar")
    into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}
