plugins {
    id("java-library")
    id("maven-publish")
}

base {
    archivesName = "serverchangelogs-api"
}

dependencies {
    api(libs.adventure.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "serverchangelogs-api"

            pom {
                name = "ServerChangelogs API"
                description = project.property("description") as String
                url = project.property("url") as String
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/license/mit"
                    }
                }
                developers {
                    developer {
                        id = "codingcat2468"
                        name = "codingcat2468"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/codingcat2468/ServerChangelogs.git"
                    developerConnection = "scm:git:ssh://github.com/codingcat2468/ServerChangelogs.git"
                    url = "https://github.com/codingcat2468/ServerChangelogs"
                }
            }
        }
    }
}
