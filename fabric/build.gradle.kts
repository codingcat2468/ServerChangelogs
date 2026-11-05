plugins {
    java
    alias(libs.plugins.lombok)
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("fabric_minecraft_version")}")
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    // Mods and libraries nested into the final jar, so that only Fabric API has to be installed
    implementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
    // Brings Adventure, MiniMessage and the slf4j component logger with it (7.1.x is the 26.2 line)
    implementation(libs.adventure.platform.fabric)
    include(libs.adventure.platform.fabric)
    implementation(libs.snakeyaml)
    include(libs.snakeyaml)

    implementation(project(":base"))
    implementation(project(":platformapi"))
    include(project(":base"))
    include(project(":platformapi"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)
    filesMatching("fabric.mod.json") { expand("version" to modVersion) }
}
