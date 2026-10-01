package com.codingcat.changelogs.base.data.storage

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Storage representation of a serialized changelog entry in Kotaml YAML.
 */
@Serializable
data class YamlStoredChangelogEntry(
    val uid: Int,
    val serializedLines: List<String> = emptyList(),
    val recordedAt: String,
    val author: String? = null,
    val playersRead: List<String> = emptyList(),
)

/**
 * Root YAML document structure holding changelog entries.
 */
@Serializable
data class YamlChangelogDocument(
    val entries: List<YamlStoredChangelogEntry> = emptyList(),
    val playerFirstSeen: Map<String, String> = emptyMap(),
)

class YamlChangelogStorage(
    private val filePath: Path,
) : ChangelogStorage {
    private val lock = Any()
    private val yaml: Yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
    private var cache: MutableList<ChangelogEntry> = mutableListOf()
    private var playerFirstSeen: MutableMap<UUID, Instant> = ConcurrentHashMap()

    override val displayName: String = "YAML File"

    override fun init() {
        synchronized(lock) {
            if (!filePath.exists()) {
                this.cache = mutableListOf()
                this.playerFirstSeen = ConcurrentHashMap()
                this.saveLocked()
                return
            }
            try {
                val text = filePath.readText()
                val doc = if (text.isBlank()) {
                    YamlChangelogDocument()
                } else {
                    yaml.decodeFromString(YamlChangelogDocument.serializer(), text)
                }
                this.cache = doc.entries.mapIndexedTo(mutableListOf()) { index, entry ->
                    deserializeEntry(index, entry)
                }
                this.playerFirstSeen = doc.playerFirstSeen.mapNotNull { (uuidStr, timeStr) ->
                    runCatching {
                        UUID.fromString(uuidStr) to DateTimeFormatter.ISO_INSTANT.parse(timeStr, Instant::from)
                    }.getOrNull()
                }.toMap(ConcurrentHashMap())
            } catch (e: Exception) {
                throw RuntimeException("Failed to load changelog data from ${filePath}", e)
            }
        }
    }

    override fun shutdown() {
    }

    override fun getFirstSeenAt(player: UUID): Instant? = playerFirstSeen[player]

    override fun recordFirstSeen(player: UUID, seenAt: Instant): Instant {
        synchronized(lock) {
            val existing = playerFirstSeen[player]
            if (existing != null) return existing
            playerFirstSeen[player] = seenAt
            saveLocked()
            return seenAt
        }
    }

    override fun storeEntry(entry: ChangelogEntry) {
        synchronized(lock) {
            this.cache.removeIf { it.uid == entry.uid }
            this.cache.add(entry)
            this.saveLocked()
        }
    }

    override fun updateEntry(entry: ChangelogEntry) {
        synchronized(lock) {
            val idx = this.cache.indexOfFirst { it.uid == entry.uid }
            if (idx >= 0) {
                this.cache[idx] = entry
            } else {
                this.cache.add(entry)
            }
            this.saveLocked()
        }
    }

    override fun removeEntry(uid: Int): Boolean {
        synchronized(lock) {
            val success = this.cache.removeIf { it.uid == uid }
            if (success) {
                this.saveLocked()
            }
            return success
        }
    }

    private fun saveLocked() {
        val storedEntries = this.cache.map { serializeEntry(it) }
        val serializedFirstSeen = this.playerFirstSeen.mapKeys { it.key.toString() }
            .mapValues { DateTimeFormatter.ISO_INSTANT.format(it.value) }
        val doc = YamlChangelogDocument(
            entries = storedEntries,
            playerFirstSeen = serializedFirstSeen,
        )
        val text = yaml.encodeToString(YamlChangelogDocument.serializer(), doc)
        val tempPath = filePath.resolveSibling("${filePath.fileName}.tmp")
        try {
            tempPath.writeText(text)
            try {
                Files.move(
                    tempPath,
                    filePath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to save changelog data to ${filePath}", e)
        }
    }

    private fun deserializeEntry(defaultIndex: Int, entry: YamlStoredChangelogEntry): ChangelogEntry {
        val uid = entry.uid
        val lines = entry.serializedLines.map { GsonComponentSerializer.gson().deserialize(it) }
        val recordedAt = DateTimeFormatter.ISO_INSTANT.parse(entry.recordedAt, Instant::from)
        val author = entry.author?.let { GsonComponentSerializer.gson().deserialize(it) }
        val playersRead = entry.playersRead.mapTo(mutableSetOf()) { UUID.fromString(it) }
        return ChangelogEntry(uid, lines, recordedAt, author, playersRead)
    }

    private fun serializeEntry(entry: ChangelogEntry): YamlStoredChangelogEntry {
        val serializedLines = entry.lines.map { GsonComponentSerializer.gson().serialize(it) }
        val recordedAtRaw = DateTimeFormatter.ISO_INSTANT.format(entry.recordedAt)
        val serializedAuthor = entry.author?.let { GsonComponentSerializer.gson().serialize(it) }
        val rawPlayersRead = entry.playersRead.map { it.toString() }
        return YamlStoredChangelogEntry(
            uid = entry.uid,
            serializedLines = serializedLines,
            recordedAt = recordedAtRaw,
            author = serializedAuthor,
            playersRead = rawPlayersRead,
        )
    }

    suspend fun saveAsync() = withContext(Dispatchers.IO) {
        synchronized(lock) {
            saveLocked()
        }
    }

    override suspend fun initAsync() = withContext(Dispatchers.IO) {
        init()
    }

    override suspend fun shutdownAsync() = withContext(Dispatchers.IO) {
        shutdown()
    }

    override suspend fun storeEntryAsync(entry: ChangelogEntry) = withContext(Dispatchers.IO) {
        storeEntry(entry)
    }

    override suspend fun updateEntryAsync(entry: ChangelogEntry) = withContext(Dispatchers.IO) {
        updateEntry(entry)
    }

    override suspend fun removeEntryAsync(uid: Int): Boolean = withContext(Dispatchers.IO) {
        removeEntry(uid)
    }

    override suspend fun markAsReadAsync(uid: Int, player: UUID) = withContext(Dispatchers.IO) {
        markAsRead(uid, player)
    }

    override suspend fun markAllAsReadAsync(uids: Collection<Int>, player: UUID) = withContext(Dispatchers.IO) {
        markAllAsRead(uids, player)
    }

    override fun listEntries(): List<ChangelogEntry> {
        synchronized(lock) {
            return this.cache.toList()
        }
    }

    override fun getByUID(uid: Int): ChangelogEntry? {
        synchronized(lock) {
            return this.cache.find { it.uid == uid }
        }
    }

    override fun markAllAsRead(uids: Collection<Int>, player: UUID) {
        synchronized(lock) {
            var changed = false
            for (uid in uids) {
                val entry = this.cache.find { it.uid == uid } ?: continue
                if (entry.playersRead.add(player)) {
                    changed = true
                }
            }
            if (changed) {
                this.saveLocked()
            }
        }
    }

    override fun markAsRead(uid: Int, player: UUID) {
        markAllAsRead(listOf(uid), player)
    }

    override fun nextUID(): Int {
        synchronized(lock) {
            return (this.cache.maxOfOrNull { it.uid } ?: -1) + 1
        }
    }
}
