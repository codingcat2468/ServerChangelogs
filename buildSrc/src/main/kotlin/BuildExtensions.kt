import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Configures the [JavaPluginExtension] on this project.
 */
fun Project.java(configure: Action<JavaPluginExtension>): Unit =
    extensions.configure("java", configure)

/**
 * Configures the [KotlinJvmProjectExtension] on this project.
 */
fun Project.kotlin(configure: Action<KotlinJvmProjectExtension>): Unit =
    extensions.configure("kotlin", configure)

/**
 * Configures the shadowJar task when the Shadow plugin is applied.
 */
fun Project.shadowJar(configure: Action<ShadowJar>) {
    pluginManager.withPlugin("com.gradleup.shadow") {
        tasks.named("shadowJar", ShadowJar::class.java, configure)
    }
}

/**
 * Configures the shadowJar task on this [TaskContainer].
 */
fun TaskContainer.shadowJar(configure: Action<ShadowJar>): TaskProvider<ShadowJar> =
    named("shadowJar", ShadowJar::class.java, configure)

/**
 * Throws an [IllegalStateException] indicating that a required project property is missing.
 */
fun Project.missingProperty(name: String): Nothing =
    throw IllegalStateException("Property '${name}' is missing. Please define it in gradle.properties.")

/**
 * Throws an [IllegalStateException] indicating that a required property is missing.
 */
fun missingProperty(name: String): Nothing =
    throw IllegalStateException("Property '${name}' is missing. Please define it in gradle.properties.")
