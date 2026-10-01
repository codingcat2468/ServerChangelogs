plugins {
    `java-library`
}

@Suppress("VulnerableLibrariesLocal", "RedundantSuppression")
dependencies {
    api(libs.kotlinx.immutable)
    api(libs.kotlinx.coroutines.core)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.packetevents.api)
    compileOnly(libs.mojang.brigadier)
    compileOnly(libs.slf4j.api)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.bundles.adventure)
    testImplementation(libs.mojang.brigadier)
}
