package com.codingcat.changelogs.base.dialog.ui.editor

import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.*
import kotlin.test.*

class EditorSessionTest {
    private class InMemoryStorage : ChangelogStorage {
        val entries = mutableMapOf<Int, ChangelogEntry>()
        var shouldFail = false

        override val displayName: String = "InMemory"
        override fun init() {}
        override fun shutdown() {}
        override fun storeEntry(entry: ChangelogEntry) {
            if (shouldFail) throw RuntimeException("Storage failure")
            entries[entry.uid] = entry
        }

        override fun updateEntry(entry: ChangelogEntry) {
            if (shouldFail) throw RuntimeException("Storage failure")
            entries[entry.uid] = entry
        }

        override fun removeEntry(uid: Int): Boolean = entries.remove(uid) != null
        override fun listEntries(): List<ChangelogEntry> = entries.values.toList()
        override fun getByUID(uid: Int): ChangelogEntry? = entries[uid]
        override fun markAllAsRead(uids: Collection<Int>, player: UUID) {}
        override fun markAsRead(uid: Int, player: UUID) {}
        override fun nextUID(): Int = (entries.keys.maxOrNull() ?: 0) + 1
        val firstSeen = mutableMapOf<UUID, Instant>()
        override fun getFirstSeenAt(player: UUID): Instant? = firstSeen[player]
        override fun recordFirstSeen(player: UUID, seenAt: Instant): Instant =
            firstSeen.computeIfAbsent(player) { seenAt }
    }

    @Test
    fun testCreateSession() {
        val session = EditorSession.Create(entryUID = 10)
        assertEquals("create", session.id)
        assertEquals("command.create", session.permission)
        assertFalse(session.canBeSaved())

        session.author = "<gold>Author</gold>"
        session.rawLines.add("<green>Line 1</green>")
        assertTrue(session.canBeSaved())

        assertEquals(1, session.deserializeLines().size)
        assertNotNull(session.deserializeAuthor())

        val storage = InMemoryStorage()
        session.commit(storage)

        val stored = storage.getByUID(10)
        assertNotNull(stored)
        assertEquals(10, stored.uid)

        // Test commit error
        storage.shouldFail = true
        val ex = assertFailsWith<EditorSession.CommitException> {
            session.commit(storage)
        }
        assertEquals("internal_error", ex.translationKeyPart)
    }

    @Test
    fun testCreateSessionAsync() {
        runBlocking {
            val session = EditorSession.Create(entryUID = 11)
            session.author = "AsyncAuthor"
            session.rawLines.add("AsyncLine")

            val storage = InMemoryStorage()
            session.commitAsync(storage)
            assertNotNull(storage.getByUID(11))
        }
    }

    @Test
    fun testEditSession() {
        val original = ChangelogEntry(
            uid = 20,
            lines = listOf(Component.text("Old line")),
            recordedAt = Instant.now(),
            author = Component.text("OldAuthor"),
            playersRead = mutableSetOf(UUID.randomUUID()),
        )
        val storage = InMemoryStorage()
        storage.storeEntry(original)

        val session = EditorSession.Edit(original)
        assertEquals("edit", session.id)
        assertEquals("manage", session.permission)
        assertFalse(session.canBeSaved())
        assertEquals(1, session.rawLines.size)

        session.rawLines.clear()
        session.rawLines.add("<yellow>New line</yellow>")
        session.commit(storage)

        val updated = storage.getByUID(20)
        assertNotNull(updated)
        assertEquals(1, updated.playersRead.size)

        // Test commit when entry was deleted from storage
        storage.removeEntry(20)
        val exDeleted = assertFailsWith<EditorSession.CommitException> {
            session.commit(storage)
        }
        assertEquals("entry_deleted", exDeleted.translationKeyPart)
    }

    @Test
    fun testEditSessionAsync() {
        runBlocking {
            val original = ChangelogEntry(
                uid = 30,
                lines = listOf(Component.text("AsyncOld")),
                recordedAt = Instant.now(),
                author = null,
                playersRead = mutableSetOf(),
            )
            val storage = InMemoryStorage()
            storage.storeEntry(original)

            val session = EditorSession.Edit(original)
            session.rawLines.add("AsyncNew")
            session.commitAsync(storage)

            assertNotNull(storage.getByUID(30))
        }
    }
}
