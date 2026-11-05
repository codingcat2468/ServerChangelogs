plugins {
    `java-library`
    alias(libs.plugins.lombok)
    // Only used to get the (unobfuscated) Minecraft classes on the compile classpath
    alias(libs.plugins.fabric.loom)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("fabric_minecraft_version")}")
    implementation(project(":platformapi"))
    // Since SnakeYAML is not part of Minecraft, it is bundled by the fabric module
    compileOnly(libs.snakeyaml)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.slf4j)
    compileOnly(libs.mojang.brigadier)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.gson)

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.gson)
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}
