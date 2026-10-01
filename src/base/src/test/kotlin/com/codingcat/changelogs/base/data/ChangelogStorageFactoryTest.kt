package com.codingcat.changelogs.base.data

import com.codingcat.changelogs.base.config.PluginConfig
import com.codingcat.changelogs.base.data.storage.ExposedChangelogStorage
import com.codingcat.changelogs.base.data.storage.YamlChangelogStorage
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChangelogStorageFactoryTest {
    @Test
    fun testCreateYamlStorage() {
        val tempDir = Files.createTempDirectory("storage_factory_yaml")
        val configFile = tempDir.resolve("config.yml")
        configFile.writeText("changelog_storage: \"yaml\"\n")

        val config = PluginConfig(configFile)
        config.reload()
        val storage = ChangelogStorage.create(config, tempDir)

        assertTrue(storage is YamlChangelogStorage)
        assertEquals("YAML File", storage.displayName)

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testCreateExposedStorageVariants() {
        val tempDir = Files.createTempDirectory("storage_factory_exposed")
        val types = listOf("sqlite", "h2", "mysql", "mariadb", "postgresql", "postgres", "exposed", "jdbc")

        for (type in types) {
            val configFile = tempDir.resolve("config_${type}.yml")
            configFile.writeText(
                """
                changelog_storage: "${type}"
                database:
                  type: "sqlite"
                """.trimIndent(),
            )
            val config = PluginConfig(configFile)
            config.reload()
            val storage = ChangelogStorage.create(config, tempDir)
            assertTrue(storage is ExposedChangelogStorage)
        }

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testUnknownStorageThrows() {
        val tempDir = Files.createTempDirectory("storage_factory_err")
        val configFile = tempDir.resolve("config_bad.yml")
        configFile.writeText("changelog_storage: \"redis\"\n")

        val config = PluginConfig(configFile)
        config.reload()

        assertFailsWith<IllegalArgumentException> {
            ChangelogStorage.create(config, tempDir)
        }

        tempDir.toFile().deleteRecursively()
    }
}
