plugins {
    `java-library`
    alias(libs.plugins.kotlin.serialization)
}

@Suppress("VulnerableLibrariesLocal")
dependencies {
    implementation(projects.platformapi)
    api(libs.slf4k)
    api(libs.kotaml)
    api(libs.kotlinx.serialization.core)
    api(libs.bundles.exposed)
    api(libs.sqlite.jdbc)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.mojang.brigadier)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.packetevents.api)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.adventure)
    testImplementation(libs.mojang.brigadier)
    testImplementation(libs.packetevents.api)
    testImplementation(libs.packetevents.paper)
    testImplementation(libs.netty.buffer)
    testImplementation(libs.slf4j.api)
}
