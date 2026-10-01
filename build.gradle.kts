plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("com.gradleup.shadow") apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover)
}

group = project.findProperty("group")?.toString() ?: missingProperty("group")
version = project.findProperty("version")?.toString() ?: missingProperty("version")
val javaVersion: Int = 25

val cleanFinalArtifacts = tasks.register<Delete>("cleanFinalArtifacts") {
    description = "Cleans the final directory with a backup of the previous state"
    val finalFile = layout.projectDirectory.dir("final").asFile
    val backupFile = layout.projectDirectory.dir("final_bak").asFile

    doFirst {
        if (finalFile.listFiles()?.isEmpty() != false) return@doFirst

        backupFile.deleteRecursively()
        finalFile.copyRecursively(target = backupFile, overwrite = true)
    }
    delete(finalFile)
}

repositories {
    mavenCentral()
}

@Suppress("AvoidApplyPluginMethod")
subprojects {
    version = rootProject.version
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    java {
        toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    kotlin {
        jvmToolchain(javaVersion)
    }

    shadowJar {
        mergeServiceFiles()
        archiveFileName = "${rootProject.name}-${project.name}-${project.version}.jar"
        destinationDirectory = rootProject.layout.projectDirectory.dir("final")
        mustRunAfter(cleanFinalArtifacts)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    repositories {
        mavenCentral()
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://libraries.minecraft.net")
    }

    tasks.named("build") {
        dependsOn(cleanFinalArtifacts)
        pluginManager.withPlugin("com.gradleup.shadow") {
            dependsOn(tasks.named("shadowJar"))
        }
    }

    tasks.withType<Test>().configureEach {
        outputs.upToDateWhen { false }
        useJUnitPlatform()
    }
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        val args = options.forkOptions.jvmArgs ?: mutableListOf()
        if ("--enable-native-access=ALL-UNNAMED" !in args) {
            args.add("--enable-native-access=ALL-UNNAMED")
        }
        options.forkOptions.jvmArgs = args
    }
}

@Suppress("unused")
tasks {
    val clean = register<Delete>("clean") {
        description = "Deletes all build directories"
        delete(rootProject.layout.buildDirectory)

        subprojects.forEach { subproject ->
            delete(subproject.layout.buildDirectory)
        }
    }
}

dependencies {
    kover(project(":base"))
    kover(project(":platformapi"))
}

kover {
    reports {
        filters {
            excludes {
                packages("com.codingcat.changelogs.paper.*", "com.codingcat.changelogs.velocity.*")
            }
        }
        total {
            verify {
                onCheck = true
                rule {
                    minBound(75)
                }
            }
        }
    }
}
