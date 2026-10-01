@file:Suppress("UnstableApiUsage")

// -------===={ Project Configuration }====-------

rootProject.name = "ServerChangelogs"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
enableFeaturePreview("NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS")

// -------===={ Subprojects }====-------

include("base")
include("platformapi")
include("paper")
include("velocity")

rootProject.children.forEach { it.projectDir = file("src/${it.name}") }

// -------===={ Plugin Management }====-------

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
    }
}

// -------===={ Plugins }====-------

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.6.0"
}

// -------===={ Plugin Configuration }====-------

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
        publishing.onlyIf { true }
    }
}
