plugins {
    `kotlin-dsl`
    `version-catalog`
    kotlin("plugin.serialization") version "1.6.21"
}

java.targetCompatibility = JavaVersion.VERSION_11

repositories {
    mavenLocal()
    maven("https://repo.kotlin.link")
    mavenCentral()
    gradlePluginPortal()
}

val toolsVersion = npmlibs.versions.tools.get()
val kotlinVersion = npmlibs.versions.kotlin.asProvider().get()
val benchmarksVersion = npmlibs.versions.kotlinx.benchmark.get()

dependencies {
    api("space.kscience:gradle-tools:$toolsVersion")
    api(npmlibs.atomicfu.gradle)
    //plugins form benchmarks
    api("org.jetbrains.kotlinx:kotlinx-benchmark-plugin:$benchmarksVersion")
    api("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    //to be used inside build-script only
    implementation(npmlibs.kotlinx.serialization.json)
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.OptIn")
}
