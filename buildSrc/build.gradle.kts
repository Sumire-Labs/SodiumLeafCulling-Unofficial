plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven("https://maven.kikugie.dev/snapshots")
    maven("https://maven.kikugie.dev/releases")
}

dependencies {
    implementation("dev.kikugie:stonecutter:0.10-alpha.12")
}
