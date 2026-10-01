package com.codingcat.changelogs.base.data

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.config.PluginConfig
import com.codingcat.changelogs.base.data.storage.ExposedChangelogStorage
import com.codingcat.changelogs.base.data.storage.YamlChangelogStorage
import org.slf4j.LoggerFactory
import org.slf4j.kotlin.KLogger
import java.nio.file.Path
import java.time.Instant
import java.util.*

/**
 * Persistence abstraction for storing, updating, reading, and indexing [ChangelogEntry] items.
 */
interface ChangelogStorage : Iterable<ChangelogEntry> {
    /**
     * Initializes storage structures, loading records into memory.
     */
    fun init()

    /**
     * Flushes modified records to persistent storage and closes resources.
     */
    fun shutdown()

    /**
     * Persists a new changelog [entry].
     */
    fun storeEntry(entry: ChangelogEntry)

    /**
     * Updates an existing changelog [entry] matching its unique ID.
     */
    fun updateEntry(entry: ChangelogEntry)

    /**
     * Deletes the changelog entry identified by [uid]. Returns true if an entry was removed.
     */
    fun removeEntry(uid: Int): Boolean

    /**
     * Returns an ordered snapshot list of all stored changelog entries.
     */
    fun listEntries(): List<ChangelogEntry>

    /**
     * Finds a changelog entry by its [uid], returning `null` if not found.
     */
    fun getByUID(uid: Int): ChangelogEntry?

    /**
     * Marks the entries with [uids] as acknowledged/read by [player] in batch.
     */
    fun markAllAsRead(uids: Collection<Int>, player: UUID)

    /**
     * Marks the entry with [uid] as acknowledged/read by [player].
     */
    fun markAsRead(uid: Int, player: UUID) {
        markAllAsRead(listOf(uid), player)
    }

    /**
     * Retrieves the instant [player] was first observed connecting to the server.
     */
    fun getFirstSeenAt(player: UUID): Instant?

    /**
     * Records [player]'s first observed connection timestamp.
     * If already recorded, returns the existing timestamp without modifying.
     */
    fun recordFirstSeen(player: UUID, seenAt: Instant = Instant.now()): Instant

    /**
     * Determines whether [entry] should be treated as unread for [player].
     * An entry is only unread if the player has not yet read it and it was published
     * after the player was first seen on the server.
     */
    fun isUnreadFor(entry: ChangelogEntry, player: UUID): Boolean {
        val firstSeen = getFirstSeenAt(player) ?: return false
        return !entry.hasRead(player) && entry.recordedAt.isAfter(firstSeen)
    }

    /**
     * Asynchronously initializes storage structures, loading records into memory.
     */
    suspend fun initAsync() {
        init()
    }

    /**
     * Asynchronously flushes modified records and closes resources.
     */
    suspend fun shutdownAsync() {
        shutdown()
    }

    /**
     * Asynchronously persists a new changelog [entry].
     */
    suspend fun storeEntryAsync(entry: ChangelogEntry) {
        storeEntry(entry)
    }

    /**
     * Asynchronously updates an existing changelog [entry] matching its unique ID.
     */
    suspend fun updateEntryAsync(entry: ChangelogEntry) {
        updateEntry(entry)
    }

    /**
     * Asynchronously deletes the changelog entry identified by [uid]. Returns true if an entry was removed.
     */
    suspend fun removeEntryAsync(uid: Int): Boolean = removeEntry(uid)

    /**
     * Asynchronously marks the entry with [uid] as acknowledged/read by [player].
     */
    suspend fun markAsReadAsync(uid: Int, player: UUID) {
        markAsRead(uid, player)
    }

    /**
     * Asynchronously marks the entries with [uids] as acknowledged/read by [player] in batch.
     */
    suspend fun markAllAsReadAsync(uids: Collection<Int>, player: UUID) {
        markAllAsRead(uids, player)
    }

    /**
     * Computes the next available unique identifier for a new entry.
     */
    fun nextUID(): Int

    /**
     * User-facing display name of this storage engine.
     */
    val displayName: String

    override fun iterator(): Iterator<ChangelogEntry> = listEntries().iterator()

    /**
     * Index operator lookup by [uid].
     */
    operator fun get(uid: Int): ChangelogEntry? = getByUID(uid)

    /**
     * Checks if an entry with [uid] exists in storage.
     */
    operator fun contains(uid: Int): Boolean = getByUID(uid) != null

    /**
     * Appends an [entry] using `+=` operator syntax.
     */
    operator fun plusAssign(entry: ChangelogEntry) {
        storeEntry(entry)
    }

    companion object {
        /**
         * Resolves and instantiates a [ChangelogStorage] backend for the requested [config].
         *
         * @throws IllegalArgumentException if the storage type identifier is unsupported.
         */
        @Throws(IllegalArgumentException::class)
        fun create(
            config: PluginConfig,
            dataPath: Path = runCatching { ServerChangelogs.platform.getDataPath() }
                .getOrElse { Path.of(".") },
        ): ChangelogStorage {
            return when (val identifier = config.changelogStorageType.lowercase()) {
                "yaml" -> YamlChangelogStorage(dataPath.resolve("_data.yml"))
                "sqlite", "h2", "mysql", "mariadb", "postgresql", "postgres", "exposed", "jdbc" -> {
                    val logger = runCatching { ServerChangelogs.logger }
                        .getOrElse { KLogger(LoggerFactory.getLogger("ChangelogsStorage")) }
                    ExposedChangelogStorage(
                        dataPath = dataPath,
                        config = config.databaseConfig.copy(
                            type = if (identifier in setOf("exposed", "jdbc")) {
                                config.databaseConfig.type
                            } else {
                                identifier
                            },
                        ),
                        logger = logger,
                    )
                }

                else -> throw IllegalArgumentException("Unknown changelog storage type \"${identifier}\"")
            }
        }

        /**
         * Resolves and instantiates a [ChangelogStorage] backend for the requested [identifier].
         *
         * @throws IllegalArgumentException if the storage type identifier is unsupported.
         */
        @Throws(IllegalArgumentException::class)
        fun create(identifier: String): ChangelogStorage {
            require(identifier == "yaml") { "Unknown changelog storage type \"${identifier}\"" }
            return YamlChangelogStorage(ServerChangelogs.platform.getDataPath().resolve("_data.yml"))
        }
    }
}
