plugins {
    id("com.gradleup.shadow")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory.paper.convention)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

@Suppress("VulnerableLibrariesLocal")
dependencies {
    implementation(projects.base)
    implementation(projects.platformapi)
    implementation(libs.mccoroutine.bukkit.api)
    implementation(libs.mccoroutine.bukkit.core)
    compileOnly(libs.packetevents.paper)
    compileOnly(libs.paper.api)
    testImplementation(libs.kotlin.test)
}

paperPluginYaml {
    name = "${rootProject.name}-${project.name}"
    main = "${group}.changelogs.paper.PaperChangelogsPlatform"
    website = project.findProperty("url")?.toString()
        ?: missingProperty("url")
    apiVersion = project.findProperty("minecraft_version_compat")?.toString()
        ?: missingProperty("minecraft_version_compat")
    foliaSupported = true

    authors.addAll(
        project.findProperty("authors")?.toString()?.split(",")
            ?: emptyList()
    )
    contributors.addAll(
        project.findProperty("contributors")?.toString()?.split(",")
            ?: emptyList()
    )

    dependencies.server.create("packetevents")
}

tasks {
    runServer {
        minecraftVersion(
            project.findProperty("minecraft_version")?.toString()
                ?: missingProperty("minecraft_version")
        )
        jvmArgs("-Xms1G", "-Xmx1G")
        downloadPlugins {
            modrinth(id = "packetevents", version = "m78nFxYg")
            modrinth(id = "luckperms", version = "b0mk8uS6")
        }
    }
}
