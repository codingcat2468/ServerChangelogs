plugins {
    java
    alias(libs.plugins.lombok)
    alias(libs.plugins.shadow)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory.paper.convention)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":base"))
    implementation(project(":platformapi"))
    // Gives access to the vanilla classes the shared dialog layer is written against
    paperweight.paperDevBundle(libs.versions.paper.dev.bundle)
}

paperPluginYaml {
    name = rootProject.name
    main = "${group}.changelogs.paper.PaperChangelogsPlatform"
    website = project.property("url") as String
    apiVersion = project.property("minecraft_version_compat").toString()
    foliaSupported = true

    authors.addAll("codingcat2468")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        minecraftVersion(project.property("minecraft_version").toString())
        jvmArgs("-Xms1G", "-Xmx1G")
    }
}
