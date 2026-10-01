package com.codingcat.changelogs.base.dialog.ui

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.TestPacketEvents
import com.codingcat.changelogs.base.compat.PacketEventsNativeItemManager
import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import com.codingcat.changelogs.base.dialog.DialogPackets
import com.codingcat.changelogs.base.dialog.DialogSessionManager
import com.codingcat.changelogs.platformapi.ChangelogsPlatform
import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.item.NativeItemManager
import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import com.github.retrooper.packetevents.protocol.ConnectionState
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTInt
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.util.TriState
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.test.*

class ChangelogDialogTest {
    private lateinit var tempDir: Path
    private val sentPackets = mutableListOf<PacketWrapper<*>>()
    private val sentMessages = mutableListOf<Component>()

    private class TestUser(
        uuid: UUID,
        name: String,
        val packetConsumer: (PacketWrapper<*>) -> Unit,
    ) : User(null, ConnectionState.PLAY, ClientVersion.V_1_21, UserProfile(uuid, name)) {
        override fun sendPacket(packet: PacketWrapper<*>) {
            packetConsumer(packet)
        }
    }

    private class TestPlayer(
        val user: TestUser,
        val messageConsumer: (Component) -> Unit,
        override val name: String = user.name ?: "Admin",
        override val isNativeAdmin: TriState = TriState.TRUE,
    ) : PlatformPlayer {
        override val uniqueId: UUID = user.uuid
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override fun asAudience(): Audience = object : Audience {
            override fun sendMessage(message: Component) {
                messageConsumer(message)
            }
        }

        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = user
    }

    private class MemoryStorage : ChangelogStorage {
        val entries = mutableListOf<ChangelogEntry>()
        override val displayName: String = "Memory"
        override fun init() {}
        override fun shutdown() {}
        override fun nextUID(): Int = (entries.maxOfOrNull { it.uid } ?: 0) + 1
        override fun getByUID(uid: Int): ChangelogEntry? = entries.find { it.uid == uid }
        override fun listEntries(): List<ChangelogEntry> = entries.toList()
        override fun iterator(): Iterator<ChangelogEntry> = entries.iterator()
        override fun storeEntry(entry: ChangelogEntry) {
            entries.removeAll { it.uid == entry.uid }
            entries.add(entry)
        }

        override fun updateEntry(entry: ChangelogEntry) {
            storeEntry(entry)
        }

        override fun removeEntry(uid: Int): Boolean = entries.removeAll { it.uid == uid }
        override fun markAllAsRead(uids: Collection<Int>, player: UUID) {
            uids.forEach { uid ->
                getByUID(uid)?.playersRead?.add(player)
            }
        }
        val firstSeen = mutableMapOf<UUID, Instant>()
        override fun getFirstSeenAt(player: UUID): Instant? = firstSeen[player]
        override fun recordFirstSeen(player: UUID, seenAt: Instant): Instant =
            firstSeen.computeIfAbsent(player) { seenAt }
    }

    private class TestPlatform(
        val dir: Path,
        val testPlayer: TestPlayer,
    ) : ChangelogsPlatform {
        override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
        override val nativeItemManager: NativeItemManager = PacketEventsNativeItemManager
        override val playerManager: PlatformPlayerManager = object : PlatformPlayerManager {
            override val onlinePlayers: ImmutableSet<PlatformPlayer> =
                setOf<PlatformPlayer>(testPlayer).toPersistentSet()

            override fun getFromName(name: String): PlatformPlayer? = if (name == testPlayer.name) testPlayer else null
            override fun getFromUUID(uuid: UUID): PlatformPlayer? =
                if (uuid == testPlayer.uniqueId) testPlayer else null

            override fun fromNative(nativePlayer: Any): PlatformPlayer = testPlayer
        }
        override val commandManager: PlatformCommandManager = object : PlatformCommandManager {
            override fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>) {}
            override fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource =
                platformCommandSource as PlatformCommandSource
        }
        override val eventManager: PlatformEventManager = object : PlatformEventManager {
            override fun <T : com.codingcat.changelogs.platformapi.event.impl.PlatformEvent> registerListener(
                eventCls: Class<T>,
                listener: (T) -> Unit,
            ) {
            }

            override fun unregisterListener(listener: Any) {}
            override fun unregisterIf(listenerPredicate: (Any) -> Boolean) {}
            override fun unregisterAll() {}
        }
        override val platformMeta: PlatformMeta = object : PlatformMeta {
            override val name: Component = Component.text("TestServer")
            override val version: String = "1.0.0"
        }
        override val changelogsMeta: ChangelogsMeta = object : ChangelogsMeta {
            override val name: String = "ServerChangelogs"
            override val version: String = "1.0.0"
            override val description: String = "Desc"
            override val authors: Set<String> = setOf("Author")
        }

        override fun getComponentLogger(): ComponentLogger = ComponentLogger.logger("Test")
        override fun getDataPath(): Path = dir
    }

    @BeforeTest
    fun setUp() {
        TestPacketEvents.setup()
        tempDir = Files.createTempDirectory("changelog_dialog_test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testBuildEmptyAndNonEmptyDialog() = runBlocking {
        val user = TestUser(UUID.randomUUID(), "Alex") { sentPackets.add(it) }
        val player = TestPlayer(user, { sentMessages.add(it) })
        val platform = TestPlatform(tempDir, player)
        ServerChangelogs.platform = platform
        ServerChangelogs.onStart()

        val memoryStorage = MemoryStorage()
        val headerItem = ItemStack.builder().type(ItemTypes.PAPER).build()
        val dialog = ChangelogDialog(
            storage = memoryStorage,
            holder = ServerChangelogs.dialogHolder,
            dateFormatter = DateTimeFormatter.ISO_INSTANT,
            addHeader = true,
            headerItem = headerItem,
            useFallbackPermissions = true,
        )

        // Empty dialog build
        val sessionManager = DialogSessionManager
        val emptyBuilt = dialog.build(player, sessionManager)
        assertNotNull(emptyBuilt)

        // Non-empty dialog with unread entry
        val entry = ChangelogEntry(
            uid = 1,
            lines = listOf(Component.text("Bug fix 1"), Component.text("Feature 2")),
            recordedAt = Instant.now(),
            author = Component.text("Dev"),
            playersRead = mutableSetOf(),
        )
        memoryStorage.storeEntry(entry)

        val builtWithEntry = dialog.build(player, sessionManager)
        assertNotNull(builtWithEntry)

        // Non-empty dialog without header
        val dialogNoHeader = ChangelogDialog(
            storage = memoryStorage,
            holder = ServerChangelogs.dialogHolder,
            dateFormatter = DateTimeFormatter.ISO_INSTANT,
            addHeader = false,
            headerItem = null,
            useFallbackPermissions = true,
        )
        val builtNoHeader = dialogNoHeader.build(player, sessionManager)
        assertNotNull(builtNoHeader)

        // showDialogView
        dialog.showDialogView(player, sessionManager, DialogPackets.PacketPhase.PLAY)
        assertTrue(sentPackets.isNotEmpty())

        ServerChangelogs.onShutdown()
    }

    @Test
    fun testActionsTriggered() = runBlocking {
        val user = TestUser(UUID.randomUUID(), "Alex") { sentPackets.add(it) }
        val player = TestPlayer(user, { sentMessages.add(it) })
        val platform = TestPlatform(tempDir, player)
        ServerChangelogs.platform = platform
        ServerChangelogs.onStart()

        val memoryStorage = MemoryStorage()
        val entry = ChangelogEntry(
            uid = 10,
            lines = listOf(Component.text("Line 1")),
            recordedAt = Instant.now(),
            author = Component.text("Author1"),
            playersRead = mutableSetOf(),
        )
        memoryStorage.storeEntry(entry)
        memoryStorage.recordFirstSeen(player.uniqueId, Instant.EPOCH)

        val dialog = ChangelogDialog(
            storage = memoryStorage,
            holder = ServerChangelogs.dialogHolder,
            dateFormatter = DateTimeFormatter.ISO_INSTANT,
            addHeader = false,
            headerItem = null,
            useFallbackPermissions = true,
        )
        val sessionManager = DialogSessionManager

        // 1. Confirm read
        assertFalse(entry.hasRead(player))
        dialog.onActionTriggered("confirm_read", null, player, sessionManager)
        assertTrue(entry.hasRead(player))
        assertTrue(sentMessages.isNotEmpty())

        // 2. Reopen
        sentPackets.clear()
        dialog.onActionTriggered("reopen", null, player, sessionManager)
        assertTrue(sentPackets.isNotEmpty())

        // 3. Request delete
        sentPackets.clear()
        val payload = NBTCompound().apply { setTag("uid", NBTInt(10)) }
        dialog.onActionTriggered("request_delete", payload, player, sessionManager)
        assertTrue(sentPackets.isNotEmpty())

        // 4. Confirm delete
        dialog.onActionTriggered("confirm_delete", payload, player, sessionManager)
        assertEquals(null, memoryStorage.getByUID(10))

        // 5. Start editing
        val entry2 = ChangelogEntry(
            uid = 20,
            lines = listOf(Component.text("Line 2")),
            recordedAt = Instant.now(),
            author = null,
            playersRead = mutableSetOf(),
        )
        memoryStorage.storeEntry(entry2)
        val payload2 = NBTCompound().apply { setTag("uid", NBTInt(20)) }
        dialog.onActionTriggered("start_editing", payload2, player, sessionManager)

        // 6. Test createLinesComponent
        val lines = listOf(Component.text("Hello"), Component.text("World"))
        val linesComp = ChangelogDialog.createLinesComponent(player, { c, _ -> c }, lines)
        assertNotNull(linesComp)

        ServerChangelogs.onShutdown()
    }
}
