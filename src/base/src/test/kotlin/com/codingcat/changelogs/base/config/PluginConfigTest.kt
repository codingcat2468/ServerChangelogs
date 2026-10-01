package com.codingcat.changelogs.base.config

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginConfigTest {
    @Test
    fun testDefaultConfigLoadAndSaveWithKotaml() {
        val tempFile = Files.createTempFile("test_config", ".yml")
        val sampleYaml = """
            changelog_storage: sqlite
            database:
              type: sqlite
              host: "127.0.0.1"
              port: 3306
              database: "changelogs_test"
              auto_migrate_yaml: true
            date_format: "yyyy-MM-dd"
            date_timezone: "UTC"
            register_dedicated_command: true
            use_native_fallback_permissions: false
            dialog_phase: PLAY
            dialog_header: false
            dialog_header_item: null
            enable_manual_workarounds: []
        """.trimIndent()

        tempFile.writeText(sampleYaml)

        val config = PluginConfig(tempFile)
        config.tryReload()

        assertEquals("sqlite", config.changelogStorageType)
        assertEquals("sqlite", config.databaseConfig.type)
        assertEquals("127.0.0.1", config.databaseConfig.host)
        assertEquals(3306, config.databaseConfig.port)
        assertTrue(config.databaseConfig.autoMigrateYaml)
        assertFalse(config.showChangelogHeader())

        // Test saving back with Kotaml
        config.save()
        val reloaded = PluginConfig(tempFile)
        reloaded.tryReload()

        assertEquals("sqlite", reloaded.changelogStorageType)
        assertEquals("sqlite", reloaded.databaseConfig.type)

        Files.deleteIfExists(tempFile)
    }
}
