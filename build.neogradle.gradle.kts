plugins {
    id("net.neoforged.gradle.userdev") version "7.1.39"
    id("neoforge-resources")
    `maven-publish`
}

version = "${project.property("mod.version")}+${sc.current.version}"
base.archivesName = "${project.property("mod.id")}-neoforge"
val requiredJava = if (sc.current.parsed >= "1.20.5") 21 else 17

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${project.property("deps.neo_loader")}")
    implementation("maven.modrinth:embeddium:${project.property("deps.embeddium_file")}")
    val extras = "io.github.llamalad7:mixinextras-common:${project.property("deps.mixinextras")}"
    annotationProcessor(extras)
    compileOnly(extras)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(requiredJava)
    targetCompatibility = JavaVersion.toVersion(requiredJava)
    toolchain {
        vendor.set(JvmVendorSpec.ADOPTIUM)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

runs {
    configureEach { modSource(sourceSets.main.get()) }
    named("client") {
        workingDirectory.set(rootProject.file("run/${sc.current.project}"))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(requiredJava)
    dependsOn("stonecutterGenerate")
}

tasks.jar {
    from(rootProject.file("LICENSE.md")) { rename("LICENSE\\.md", "LICENSE.md_slc_unofficial") }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.jar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}
