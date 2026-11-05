pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

rootProject.name = "ServerChangelogs"
include("base", "platformapi", "fabric")