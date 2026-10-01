import xyz.jpenilla.resourcefactory.velocity.VelocityPluginJson

plugins {
    id("com.gradleup.shadow")
    alias(libs.plugins.run.velocity)
    alias(libs.plugins.resource.factory.velocity.convention)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

@Suppress("VulnerableLibrariesLocal")
dependencies {
    implementation(projects.base)
    implementation(projects.platformapi)
    implementation(libs.mccoroutine.velocity.api)
    implementation(libs.mccoroutine.velocity.core)
    compileOnly(libs.packetevents.velocity)
    compileOnly(libs.velocity.api)
    testImplementation(libs.kotlin.test)
}

velocityPluginJson {
    id = "server-changelogs"
    name = "${rootProject.name}-${project.name}"
    url = project.findProperty("url")?.toString()
        ?: missingProperty("url")

    main = "${group}.changelogs.velocity.VelocityChangelogsPlatform"
    authors.addAll(
        project.findProperty("authors")?.toString()?.split(",")
            ?: emptyList()
    )
    dependencies.addAll(VelocityPluginJson.Dependency("packetevents", false))
}

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.api.get())
        jvmArgs("-Xms512M", "-Xmx512M")
        downloadPlugins {
            modrinth(id = "packetevents", version = "p0asH9aC")
            modrinth(id = "luckperms", version = "tamnmXad")
        }
    }
}
