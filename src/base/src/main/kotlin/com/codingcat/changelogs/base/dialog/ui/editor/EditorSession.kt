package com.codingcat.changelogs.base.dialog.ui.editor

import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.time.Instant

/**
 * Active editing session representing the drafting or updating of a changelog entry.
 *
 * @property entryUID Target unique identifier for the changelog entry being edited.
 */
sealed class EditorSession(
    val entryUID: Int,
) {
    val rawLines: MutableList<String> = mutableListOf()
    var editingLineIndex: Int = -1
    var currentLine: String = ""
    var author: String = ""
    var showRestoredMessage: Boolean = false

    /**
     * Commits this editor session synchronously to the specified [storage].
     *
     * @throws CommitException if saving fails.
     */
    @Throws(CommitException::class)
    abstract fun commit(storage: ChangelogStorage)

    /**
     * Asynchronously commits this editor session to the specified [storage].
     *
     * @throws CommitException if saving fails.
     */
    @Throws(CommitException::class)
    open suspend fun commitAsync(storage: ChangelogStorage) {
        commit(storage)
    }

    /**
     * Unique session mode identifier used for localization and UI state.
     */
    abstract val id: String

    /**
     * Permission node required to commit or interact with this session mode.
     */
    abstract val permission: String

    /**
     * Whether this session contains unsaved state eligible for session restoration.
     */
    abstract fun canBeSaved(): Boolean

    /**
     * Deserializes raw MiniMessage line strings into [Component] instances.
     */
    fun deserializeLines(): List<Component> {
        return rawLines.map { MiniMessage.miniMessage().deserialize(it) }
    }

    /**
     * Deserializes the author string into a [Component], or `null` if blank.
     */
    fun deserializeAuthor(): Component? {
        return if (author.isNotBlank()) MiniMessage.miniMessage().deserialize(author) else null
    }

    /**
     * Editor session for authoring a new changelog entry.
     */
    class Create(entryUID: Int) : EditorSession(entryUID) {
        override val id: String = "create"
        override val permission: String = "command.create"

        override fun canBeSaved(): Boolean = rawLines.isNotEmpty() || author.isNotBlank()

        @Throws(CommitException::class)
        override fun commit(storage: ChangelogStorage) {
            val newEntry = ChangelogEntry(
                entryUID,
                this.deserializeLines(),
                Instant.now(),
                this.deserializeAuthor(),
                mutableSetOf(),
            )
            runCatching {
                storage.storeEntry(newEntry)
            }.getOrElse {
                throw CommitException("internal_error")
            }
        }

        @Throws(CommitException::class)
        override suspend fun commitAsync(storage: ChangelogStorage) {
            val newEntry = ChangelogEntry(
                entryUID,
                this.deserializeLines(),
                Instant.now(),
                this.deserializeAuthor(),
                mutableSetOf(),
            )
            runCatching {
                storage.storeEntryAsync(newEntry)
            }.getOrElse {
                throw CommitException("internal_error")
            }
        }
    }

    /**
     * Editor session for modifying an existing changelog entry.
     */
    class Edit(entry: ChangelogEntry) : EditorSession(entry.uid) {
        init {
            entry.lines.mapTo(rawLines) { MiniMessage.miniMessage().serialize(it) }
            this.author = entry.author?.let { MiniMessage.miniMessage().serialize(it) } ?: ""
        }

        override val id: String = "edit"
        override val permission: String = "manage"

        override fun canBeSaved(): Boolean = false

        @Throws(CommitException::class)
        override fun commit(storage: ChangelogStorage) {
            val currentEntry: ChangelogEntry = storage.getByUID(entryUID)
                ?: throw CommitException("entry_deleted")
            val newEntry = ChangelogEntry(
                currentEntry.uid,
                this.deserializeLines(),
                currentEntry.recordedAt,
                this.deserializeAuthor(),
                currentEntry.playersRead,
            )
            runCatching {
                storage.updateEntry(newEntry)
            }.getOrElse {
                throw CommitException("internal_error")
            }
        }

        @Throws(CommitException::class)
        override suspend fun commitAsync(storage: ChangelogStorage) {
            val currentEntry: ChangelogEntry = storage.getByUID(entryUID)
                ?: throw CommitException("entry_deleted")
            val newEntry = ChangelogEntry(
                currentEntry.uid,
                this.deserializeLines(),
                currentEntry.recordedAt,
                this.deserializeAuthor(),
                currentEntry.playersRead,
            )
            runCatching {
                storage.updateEntryAsync(newEntry)
            }.getOrElse {
                throw CommitException("internal_error")
            }
        }
    }

    /**
     * Exception raised when an [EditorSession] cannot be committed to storage.
     */
    class CommitException(val translationKeyPart: String) : Exception()
}
