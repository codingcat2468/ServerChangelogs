package com.codingcat.changelogs.base.data.storage

import com.codingcat.changelogs.base.config.DatabaseConfig
import com.codingcat.changelogs.base.data.ChangelogEntry
import net.kyori.adventure.text.Component
import org.slf4j.LoggerFactory
import org.slf4j.kotlin.KLogger
import java.nio.file.Files
import java.time.Instant
import java.util.*
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExposedChangelogStorageTest {
    private val logger = KLogger(LoggerFactory.getLogger("TestLogger"))

    @Test
    fun testStoreAndRetrieveEntries() {
        val tempDir = Files.createTempDirectory("changelogs_exposed_test")
        val config = DatabaseConfig(type = "sqlite")
        val storage = ExposedChangelogStorage(tempDir, config, logger)

        storage.init()

        val playerUuid = UUID.randomUUID()
        val entry = ChangelogEntry(
            uid = 1,
            lines = listOf(Component.text("Line 1"), Component.text("Line 2")),
            recordedAt = Instant.now(),
            author = Component.text("Admin"),
            playersRead = mutableSetOf(),
        )

        storage.storeEntry(entry)
        assertEquals(1, storage.listEntries().size)
        assertEquals(1, storage.getByUID(1)?.uid)

        storage.markAsRead(1, playerUuid)
        val updated = storage.getByUID(1)
        assertNotNull(updated)
        assertTrue(updated.playersRead.contains(playerUuid))

        // Create new storage instance pointing to same DB file to ensure persistence
        val reloadedStorage = ExposedChangelogStorage(tempDir, config, logger)
        reloadedStorage.init()
        assertEquals(1, reloadedStorage.listEntries().size)
        val reloadedEntry = reloadedStorage.getByUID(1)
        assertNotNull(reloadedEntry)
        assertTrue(reloadedEntry.playersRead.contains(playerUuid))

        // Test delete
        assertTrue(reloadedStorage.removeEntry(1))
        assertEquals(0, reloadedStorage.listEntries().size)

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testAutoMigrationFromYaml() {
        val tempDir = Files.createTempDirectory("changelogs_migration_test")
        val yamlFile = tempDir.resolve("_data.yml")
        val yamlStorage = YamlChangelogStorage(yamlFile)
        yamlStorage.init()

        val playerUuid = UUID.randomUUID()
        val entry = ChangelogEntry(
            uid = 42,
            lines = listOf(Component.text("Migrated Line")),
            recordedAt = Instant.now(),
            author = Component.text("Migrator"),
            playersRead = mutableSetOf(playerUuid),
        )
        yamlStorage.storeEntry(entry)
        assertTrue(yamlFile.exists())

        // Initialize Exposed storage with autoMigrateYaml = true
        val config = DatabaseConfig(type = "sqlite", autoMigrateYaml = true)
        val exposedStorage = ExposedChangelogStorage(tempDir, config, logger)
        exposedStorage.init()

        // Verify entry migrated
        val migrated = exposedStorage.getByUID(42)
        assertNotNull(migrated)
        assertEquals("Migrator", (migrated.author as? net.kyori.adventure.text.TextComponent)?.content())
        assertTrue(migrated.playersRead.contains(playerUuid))
        assertTrue(tempDir.resolve("_data.yml.bak").exists())

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testAsyncOperations() {
        kotlinx.coroutines.runBlocking {
            val tempDir = Files.createTempDirectory("changelogs_async_test")
            val config = DatabaseConfig(type = "sqlite")
            val storage = ExposedChangelogStorage(tempDir, config, logger)

            storage.initAsync()

            val playerUuid = UUID.randomUUID()
            val entry = ChangelogEntry(
                uid = 10,
                lines = listOf(Component.text("Async Line")),
                recordedAt = Instant.now(),
                author = Component.text("AsyncAuthor"),
                playersRead = mutableSetOf(),
            )

            storage.storeEntryAsync(entry)
            assertEquals(1, storage.listEntries().size)

            storage.markAllAsReadAsync(listOf(10), playerUuid)
            val readEntry = storage.getByUID(10)
            assertNotNull(readEntry)
            assertTrue(readEntry.playersRead.contains(playerUuid))

            assertTrue(storage.removeEntryAsync(10))
            assertEquals(0, storage.listEntries().size)

            storage.shutdownAsync()
            tempDir.toFile().deleteRecursively()
        }
    }
}
