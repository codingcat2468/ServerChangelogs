package com.codingcat.changelogs.base.data.storage

import com.codingcat.changelogs.base.data.ChangelogEntry
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import java.nio.file.Files
import java.time.Instant
import java.util.*
import kotlin.io.path.writeText
import kotlin.test.*

class YamlChangelogStorageTest {
    @Test
    fun testInitCreatesEmptyFile() {
        val tempDir = Files.createTempDirectory("yaml_storage_test")
        val yamlFile = tempDir.resolve("_data.yml")
        val storage = YamlChangelogStorage(yamlFile)

        assertEquals("YAML File", storage.displayName)
        storage.init()
        assertTrue(Files.exists(yamlFile))
        assertEquals(0, storage.listEntries().size)
        assertEquals(0, storage.nextUID())
        storage.shutdown()

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testCrudAndPersistence() {
        val tempDir = Files.createTempDirectory("yaml_crud_test")
        val yamlFile = tempDir.resolve("_data.yml")
        val storage = YamlChangelogStorage(yamlFile)
        storage.init()

        val playerUuid = UUID.randomUUID()
        val entry1 = ChangelogEntry(
            uid = 1,
            lines = listOf(Component.text("Line 1")),
            recordedAt = Instant.now(),
            author = Component.text("Author 1"),
            playersRead = mutableSetOf(),
        )

        storage.storeEntry(entry1)
        assertEquals(1, storage.listEntries().size)
        assertEquals(2, storage.nextUID())

        val entry2 = ChangelogEntry(
            uid = 1,
            lines = listOf(Component.text("Updated Line 1")),
            recordedAt = entry1.recordedAt,
            author = Component.text("Author 1"),
            playersRead = mutableSetOf(),
        )
        storage.updateEntry(entry2)
        assertEquals(1, storage.listEntries().size)
        assertEquals(
            "Updated Line 1",
            (storage.getByUID(1)?.lines?.firstOrNull() as? net.kyori.adventure.text.TextComponent)?.content(),
        )

        storage.markAsRead(1, playerUuid)
        assertTrue(storage.getByUID(1)?.playersRead?.contains(playerUuid) == true)

        // Reload from file to ensure persistence
        val storage2 = YamlChangelogStorage(yamlFile)
        storage2.init()
        val loaded = storage2.getByUID(1)
        assertNotNull(loaded)
        assertTrue(loaded.playersRead.contains(playerUuid))

        assertTrue(storage2.removeEntry(1))
        assertFalse(storage2.removeEntry(999))
        assertNull(storage2.getByUID(1))

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testAsyncOperations() {
        runBlocking {
            val tempDir = Files.createTempDirectory("yaml_async_test")
            val yamlFile = tempDir.resolve("_data.yml")
            val storage = YamlChangelogStorage(yamlFile)

            storage.initAsync()

            val entry = ChangelogEntry(
                uid = 5,
                lines = listOf(Component.text("Async line")),
                recordedAt = Instant.now(),
                author = null,
                playersRead = mutableSetOf(),
            )

            storage.storeEntryAsync(entry)
            assertEquals(1, storage.listEntries().size)
            assertEquals(6, storage.nextUID())

            val uuid = UUID.randomUUID()
            storage.markAllAsReadAsync(listOf(5), uuid)
            assertTrue(storage.getByUID(5)?.playersRead?.contains(uuid) == true)

            storage.saveAsync()

            assertTrue(storage.removeEntryAsync(5))
            storage.shutdownAsync()

            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testCorruptFileThrows() {
        val tempDir = Files.createTempDirectory("yaml_corrupt_test")
        val yamlFile = tempDir.resolve("_data.yml")
        yamlFile.writeText("invalid: yaml: [broken")
        val storage = YamlChangelogStorage(yamlFile)

        assertFailsWith<RuntimeException> {
            storage.init()
        }

        tempDir.toFile().deleteRecursively()
    }
}
