package com.codingcat.changelogs.base.data.storage

import com.codingcat.changelogs.base.config.DatabaseConfig
import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.kotlin.KLogger
import org.slf4j.kotlin.info
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.moveTo

/**
 * Exposed database table representing stored changelog entries.
 */
object ChangelogEntriesTable : Table("server_changelogs_entries") {
    val uid = integer("uid")
    val lines = text("lines")
    val recordedAt = timestamp("recorded_at")
    val author = text("author").nullable()

    override val primaryKey = PrimaryKey(uid)
}

/**
 * Exposed database table tracking player read receipts per changelog entry.
 */
object ChangelogReadsTable : Table("server_changelogs_reads") {
    val changelogUid = integer("changelog_uid").references(
        ChangelogEntriesTable.uid,
        onDelete = ReferenceOption.CASCADE,
    )
    val playerUuid = varchar("player_uuid", 36)

    override val primaryKey = PrimaryKey(changelogUid, playerUuid)
}

/**
 * Exposed database table tracking player first-seen connection timestamps.
 */
object PlayerFirstSeenTable : Table("server_changelogs_first_seen") {
    val playerUuid = varchar("player_uuid", 36)
    val firstSeenAt = timestamp("first_seen_at")

    override val primaryKey = PrimaryKey(playerUuid)
}

/**
 * Relational database storage engine powered by JetBrains Exposed.
 */
class ExposedChangelogStorage(
    private val dataPath: Path,
    private val config: DatabaseConfig,
    private val logger: KLogger,
) : ChangelogStorage {
    private val lock = Any()
    private val gson = Gson()
    private var cache: MutableList<ChangelogEntry> = mutableListOf()
    private val playerFirstSeen: MutableMap<UUID, Instant> = java.util.concurrent.ConcurrentHashMap()
    private lateinit var database: Database

    override val displayName: String = "Exposed Database (${config.type})"

    override fun init() {
        this.database = createDatabase()
        transaction(this.database) {
            SchemaUtils.create(ChangelogEntriesTable, ChangelogReadsTable, PlayerFirstSeenTable)
        }
        if (config.autoMigrateYaml) {
            checkAutoMigrate()
        }
        synchronized(lock) {
            loadFromDatabaseLocked()
        }
    }

    override fun shutdown() {
    }

    override fun getFirstSeenAt(player: UUID): Instant? = playerFirstSeen[player]

    override fun recordFirstSeen(player: UUID, seenAt: Instant): Instant {
        val existing = playerFirstSeen[player]
        if (existing != null) return existing
        synchronized(lock) {
            val doubleCheck = playerFirstSeen[player]
            if (doubleCheck != null) return doubleCheck
            playerFirstSeen[player] = seenAt
            transaction(this.database) {
                PlayerFirstSeenTable.batchInsert(listOf(player to seenAt), ignore = true) { (p, t) ->
                    this[PlayerFirstSeenTable.playerUuid] = p.toString()
                    this[PlayerFirstSeenTable.firstSeenAt] = t
                }
            }
            return seenAt
        }
    }

    override suspend fun initAsync() = withContext(Dispatchers.IO) {
        init()
    }

    override suspend fun shutdownAsync() = withContext(Dispatchers.IO) {
        shutdown()
    }

    override fun storeEntry(entry: ChangelogEntry) {
        synchronized(lock) {
            this.cache.removeIf { it.uid == entry.uid }
            this.cache.add(entry)
        }
        saveEntryToDb(entry)
    }

    override fun updateEntry(entry: ChangelogEntry) {
        synchronized(lock) {
            val idx = this.cache.indexOfFirst { it.uid == entry.uid }
            if (idx >= 0) {
                this.cache[idx] = entry
            } else {
                this.cache.add(entry)
            }
        }
        saveEntryToDb(entry)
    }

    override fun removeEntry(uid: Int): Boolean {
        val removed: Boolean
        synchronized(lock) {
            removed = this.cache.removeIf { it.uid == uid }
        }
        if (removed) {
            transaction(this.database) {
                ChangelogEntriesTable.deleteWhere { ChangelogEntriesTable.uid eq uid }
            }
        }
        return removed
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
        val toInsert = mutableListOf<Int>()
        synchronized(lock) {
            for (uid in uids) {
                val entry = this.cache.find { it.uid == uid } ?: continue
                if (entry.playersRead.add(player)) {
                    toInsert.add(uid)
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            val playerStr = player.toString()
            transaction(this.database) {
                ChangelogReadsTable.batchInsert(toInsert, ignore = true) { uid ->
                    this[ChangelogReadsTable.changelogUid] = uid
                    this[ChangelogReadsTable.playerUuid] = playerStr
                }
            }
        }
    }

    override fun markAsRead(uid: Int, player: UUID) {
        markAllAsRead(listOf(uid), player)
    }

    override suspend fun markAsReadAsync(uid: Int, player: UUID) = withContext(Dispatchers.IO) {
        markAsRead(uid, player)
    }

    override suspend fun markAllAsReadAsync(uids: Collection<Int>, player: UUID) = withContext(Dispatchers.IO) {
        markAllAsRead(uids, player)
    }

    override fun nextUID(): Int {
        synchronized(lock) {
            return (this.cache.maxOfOrNull { it.uid } ?: -1) + 1
        }
    }

    private fun saveEntryToDb(entry: ChangelogEntry) {
        val serializedLines = entry.lines.map { GsonComponentSerializer.gson().serialize(it) }
        val jsonLines = gson.toJson(serializedLines)
        val serializedAuthor = entry.author?.let { GsonComponentSerializer.gson().serialize(it) }
        val playerStrings = entry.playersRead.map { it.toString() }

        transaction(this.database) {
            ChangelogEntriesTable.deleteWhere { uid eq entry.uid }
            ChangelogEntriesTable.insert { row ->
                row[uid] = entry.uid
                row[lines] = jsonLines
                row[recordedAt] = entry.recordedAt
                row[author] = serializedAuthor
            }
            if (playerStrings.isNotEmpty()) {
                ChangelogReadsTable.batchInsert(playerStrings, ignore = true) { pStr ->
                    this[ChangelogReadsTable.changelogUid] = entry.uid
                    this[ChangelogReadsTable.playerUuid] = pStr
                }
            }
        }
    }

    private fun loadFromDatabaseLocked() {
        val entries = transaction(this.database) {
            val readsMap = mutableMapOf<Int, MutableSet<UUID>>()
            ChangelogReadsTable.selectAll().forEach { row ->
                val cUid = row[ChangelogReadsTable.changelogUid]
                val pUuid = runCatching { UUID.fromString(row[ChangelogReadsTable.playerUuid]) }.getOrNull()
                if (pUuid != null) {
                    readsMap.computeIfAbsent(cUid) { mutableSetOf() }.add(pUuid)
                }
            }

            PlayerFirstSeenTable.selectAll().forEach { row ->
                val pUuid = runCatching { UUID.fromString(row[PlayerFirstSeenTable.playerUuid]) }.getOrNull()
                if (pUuid != null) {
                    playerFirstSeen[pUuid] = row[PlayerFirstSeenTable.firstSeenAt]
                }
            }

            ChangelogEntriesTable.selectAll().map { row ->
                val uid = row[ChangelogEntriesTable.uid]
                val linesRaw: List<String> = runCatching {
                    gson.fromJson(row[ChangelogEntriesTable.lines], Array<String>::class.java)?.toList()
                }.getOrNull() ?: emptyList()
                val lines: List<Component> = linesRaw.map { GsonComponentSerializer.gson().deserialize(it) }
                val recordedAt: Instant = row[ChangelogEntriesTable.recordedAt]
                val author: Component? = row[ChangelogEntriesTable.author]?.let {
                    GsonComponentSerializer.gson().deserialize(it)
                }
                val playersRead: MutableSet<UUID> = readsMap[uid] ?: mutableSetOf()
                ChangelogEntry(uid, lines, recordedAt, author, playersRead)
            }
        }
        this.cache = entries.sortedBy { it.uid }.toMutableList()
    }

    private fun checkAutoMigrate() {
        val yamlFile = dataPath.resolve("_data.yml")
        if (!yamlFile.exists()) return

        val isEmpty = transaction(this.database) {
            ChangelogEntriesTable.selectAll().empty()
        }
        if (!isEmpty) return

        logger.info { "Found existing _data.yml with empty database, starting auto-migration..." }
        val yamlStorage = YamlChangelogStorage(yamlFile)
        yamlStorage.init()
        val yamlEntries = yamlStorage.listEntries()
        if (yamlEntries.isEmpty()) return

        transaction(this.database) {
            for ((uid, lines, recordedAt, author, playersRead) in yamlEntries) {
                val serializedLines = lines.map { GsonComponentSerializer.gson().serialize(it) }
                val jsonLines = gson.toJson(serializedLines)
                val serializedAuthor = author?.let { GsonComponentSerializer.gson().serialize(it) }
                ChangelogEntriesTable.insert { row ->
                    row[this@insert.uid] = uid
                    row[this@insert.lines] = jsonLines
                    row[this@insert.recordedAt] = recordedAt
                    row[this@insert.author] = serializedAuthor
                }
                if (playersRead.isNotEmpty()) {
                    val pStrings = playersRead.map { it.toString() }
                    ChangelogReadsTable.batchInsert(pStrings, ignore = true) { pStr ->
                        this[ChangelogReadsTable.changelogUid] = uid
                        this[ChangelogReadsTable.playerUuid] = pStr
                    }
                }
            }
        }
        logger.info { "Successfully migrated ${yamlEntries.size} entries from _data.yml to Exposed database." }
        val backupFile = dataPath.resolve("_data.yml.bak")
        runCatching { yamlFile.moveTo(backupFile, overwrite = true) }
    }

    private fun createDatabase(): Database {
        val jdbcUrl = config.jdbcUrl ?: when (config.type.lowercase()) {
            "sqlite" -> {
                val dbFile = dataPath.resolve("changelogs.db").toAbsolutePath()
                "jdbc:sqlite:${dbFile}"
            }

            "h2" -> {
                val dbFile = dataPath.resolve("changelogs").toAbsolutePath()
                "jdbc:h2:${dbFile};DB_CLOSE_DELAY=-1"
            }

            "mysql" -> {
                "jdbc:mysql://${config.host}:${config.port}/${config.database}" +
                        "?useSSL=false&allowPublicKeyRetrieval=true"
            }

            "mariadb" -> {
                "jdbc:mariadb://${config.host}:${config.port}/${config.database}" +
                        "?useSSL=false&allowPublicKeyRetrieval=true"
            }

            "postgresql", "postgres" -> {
                "jdbc:postgresql://${config.host}:${config.port}/${config.database}"
            }

            else -> {
                val dbFile = dataPath.resolve("changelogs.db").toAbsolutePath()
                "jdbc:sqlite:${dbFile}"
            }
        }

        val driver = when {
            jdbcUrl.startsWith("jdbc:sqlite:") -> "org.sqlite.JDBC"
            jdbcUrl.startsWith("jdbc:h2:") -> "org.h2.Driver"
            jdbcUrl.startsWith("jdbc:mysql:") -> "com.mysql.cj.jdbc.Driver"
            jdbcUrl.startsWith("jdbc:mariadb:") -> "org.mariadb.jdbc.Driver"
            jdbcUrl.startsWith("jdbc:postgresql:") -> "org.postgresql.Driver"
            else -> null
        }

        return if (driver != null) {
            Database.connect(
                url = jdbcUrl,
                driver = driver,
                user = config.username,
                password = config.password,
            )
        } else {
            Database.connect(
                url = jdbcUrl,
                user = config.username,
                password = config.password,
            )
        }
    }
}
