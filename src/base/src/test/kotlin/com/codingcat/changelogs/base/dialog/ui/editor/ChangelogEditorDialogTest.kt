package com.codingcat.changelogs.base.dialog.ui.editor

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.TestPacketEvents
import com.codingcat.changelogs.base.compat.PacketEventsNativeItemManager
import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import com.codingcat.changelogs.base.dialog.DialogSessionManager
import com.codingcat.changelogs.base.dialog.PluginDialog
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
import com.github.retrooper.packetevents.protocol.nbt.NBTByte
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTInt
import com.github.retrooper.packetevents.protocol.nbt.NBTString
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
import java.util.*
import kotlin.test.*

class ChangelogEditorDialogTest {
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
        val permGranted: Boolean = true,
    ) : PlatformPlayer {
        override val uniqueId: UUID = user.uuid
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override fun asAudience(): Audience = object : Audience {
            override fun sendMessage(message: Component) {
                messageConsumer(message)
            }
        }

        override fun asPermissionChecker(): PermissionChecker =
            PermissionChecker { if (permGranted) TriState.TRUE else TriState.FALSE }

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
        tempDir = Files.createTempDirectory("changelog_editor_test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testBuildCreateAndEditDialogs() = runBlocking {
        val user = TestUser(UUID.randomUUID(), "Admin") { sentPackets.add(it) }
        val player = TestPlayer(user, { sentMessages.add(it) })
        val platform = TestPlatform(tempDir, player)
        ServerChangelogs.platform = platform
        ServerChangelogs.onStart()

        val storage = MemoryStorage()
        val editor = ChangelogEditorDialog(storage, useFallbackPermissions = true)
        val sessionManager = DialogSessionManager

        // 1. Build Create Session
        val createDialog = editor.build(player, sessionManager)
        assertNotNull(createDialog)

        // 2. Build Edit Session
        val existingEntry = ChangelogEntry(
            uid = 5,
            lines = listOf(Component.text("Fix issue"), Component.text("New feature")),
            recordedAt = Instant.now(),
            author = Component.text("Dev"),
            playersRead = mutableSetOf(),
        )
        storage.storeEntry(existingEntry)

        sessionManager.endSession(player)
        val editSession = EditorSession.Edit(existingEntry)
        sessionManager.startSession(editor, player, editSession)

        val editDialog = editor.build(player, sessionManager)
        assertNotNull(editDialog)

        // 3. Title key and permission check tests
        assertEquals("dialog.editor.create.title", ChangelogEditorDialog.titleKey(EditorSession.Create(1)))
        assertEquals("dialog.editor.edit.title", ChangelogEditorDialog.titleKey(editSession))

        assertTrue(ChangelogEditorDialog.permissionCheck("create", player, useFallbackPermissions = true))
        assertTrue(ChangelogEditorDialog.permissionCheck("create", player, useFallbackPermissions = false))

        val noAdminPlayer = TestPlayer(user, {}, isNativeAdmin = TriState.FALSE, permGranted = false)
        assertFalse(ChangelogEditorDialog.permissionCheck("create", noAdminPlayer, useFallbackPermissions = true))
        assertFalse(ChangelogEditorDialog.permissionCheck("create", noAdminPlayer, useFallbackPermissions = false))

        // 4. Attempt destroy without saved sessions
        editor.attemptDestroy()

        ServerChangelogs.onShutdown()
    }

    @Test
    fun testActionAddEditRemoveLines() = runBlocking {
        val user = TestUser(UUID.randomUUID(), "Admin") { sentPackets.add(it) }
        val player = TestPlayer(user, { sentMessages.add(it) })
        val platform = TestPlatform(tempDir, player)
        ServerChangelogs.platform = platform
        ServerChangelogs.onStart()

        val storage = MemoryStorage()
        val editor = ChangelogEditorDialog(storage, useFallbackPermissions = true)
        val sessionManager = DialogSessionManager

        val session = EditorSession.Create(1)
        sessionManager.startSession(editor, player, session)

        // Add empty line -> should show retry dialog
        sentPackets.clear()
        val emptyData = NBTCompound().apply {
            setTag("line", NBTString(""))
            setTag("author", NBTString("Admin"))
        }
        editor.onActionTriggered("add_line", emptyData, player, sessionManager)
        assertTrue(sentPackets.isNotEmpty())

        // Add valid line
        val validData1 = NBTCompound().apply {
            setTag("line", NBTString("Line 1"))
            setTag("author", NBTString("Admin"))
        }
        editor.onActionTriggered("add_line", validData1, player, sessionManager)
        assertEquals(listOf("Line 1"), session.rawLines)
        assertEquals("", session.currentLine)

        // Add second line
        val validData2 = NBTCompound().apply {
            setTag("line", NBTString("Line 2"))
            setTag("author", NBTString("Admin"))
        }
        editor.onActionTriggered("add_line", validData2, player, sessionManager)
        assertEquals(listOf("Line 1", "Line 2"), session.rawLines)

        // Start edit line 0
        val target0 = NBTCompound().apply { setTag("target_line", NBTInt(0)) }
        editor.onActionTriggered("start_edit_line", target0, player, sessionManager)
        assertEquals(0, session.editingLineIndex)
        assertEquals("Line 1", session.currentLine)

        // Edit line with new content
        val editData = NBTCompound().apply {
            setTag("line", NBTString("Line 1 Updated"))
            setTag("author", NBTString("Admin"))
        }
        editor.onActionTriggered("edit_line", editData, player, sessionManager)
        assertEquals(listOf("Line 1 Updated", "Line 2"), session.rawLines)
        assertEquals(-1, session.editingLineIndex)

        // Start edit line 1 and edit with blank (should remove line)
        val target1 = NBTCompound().apply { setTag("target_line", NBTInt(1)) }
        editor.onActionTriggered("start_edit_line", target1, player, sessionManager)
        assertEquals(1, session.editingLineIndex)
        val blankEdit = NBTCompound().apply {
            setTag("line", NBTString("   "))
            setTag("author", NBTString("Admin"))
        }
        editor.onActionTriggered("edit_line", blankEdit, player, sessionManager)
        assertEquals(listOf("Line 1 Updated"), session.rawLines)

        // Remove line 0
        editor.onActionTriggered("remove_line", target0, player, sessionManager)
        assertTrue(session.rawLines.isEmpty())

        // Reopen action
        sentPackets.clear()
        editor.onActionTriggered("reopen", null, player, sessionManager)
        assertTrue(sentPackets.isNotEmpty())

        ServerChangelogs.onShutdown()
    }

    @Test
    fun testActionCloseAndSaveSession() = runBlocking {
        val user = TestUser(UUID.randomUUID(), "Admin") { sentPackets.add(it) }
        val player = TestPlayer(user, { sentMessages.add(it) })
        val platform = TestPlatform(tempDir, player)
        ServerChangelogs.platform = platform
        ServerChangelogs.onStart()

        val storage = MemoryStorage()
        val editor = ChangelogEditorDialog(storage, useFallbackPermissions = true)
        val sessionManager = DialogSessionManager

        // Empty session try_close -> closes immediately
        val emptySession = EditorSession.Create(1)
        sessionManager.startSession(editor, player, emptySession)
        editor.onActionTriggered("try_close", null, player, sessionManager)
        assertFalse(sessionManager.isSessionActive(player, editor))

        // Session with lines try_close -> shows confirm close dialog
        val activeSession = EditorSession.Create(2).apply { rawLines.add("Draft line") }
        sessionManager.startSession(editor, player, activeSession)
        sentPackets.clear()
        editor.onActionTriggered("try_close", null, player, sessionManager)
        assertTrue(sentPackets.isNotEmpty())

        // Close with save_session = true
        val savePayload = NBTCompound().apply { setTag("save_session", NBTByte(true)) }
        editor.onActionTriggered("close", savePayload, player, sessionManager)
        assertFalse(sessionManager.isSessionActive(player, editor))

        // Attempt destroy should now fail because a session was saved
        assertFailsWith<PluginDialog.DestroyRejectedException> {
            editor.attemptDestroy()
        }

        // Reopen dialog -> should restore saved session with restored message
        val restoredDialog = editor.build(player, sessionManager)
        assertNotNull(restoredDialog)

        // Close with save_session = false -> discarded
        val discardPayload = NBTCompound().apply { setTag("save_session", NBTByte(false)) }
        editor.onActionTriggered("close", discardPayload, player, sessionManager)
        editor.attemptDestroy() // Now passes

        ServerChangelogs.onShutdown()
    }

    @Test
    fun testActionCommit() = runBlocking {
        val user = TestUser(UUID.randomUUID(), "Admin") { sentPackets.add(it) }
        val player = TestPlayer(user, { sentMessages.add(it) })
        val platform = TestPlatform(tempDir, player)
        ServerChangelogs.platform = platform
        ServerChangelogs.onStart()

        val storage = MemoryStorage()
        val editor = ChangelogEditorDialog(storage, useFallbackPermissions = true)
        val sessionManager = DialogSessionManager

        // Commit with no lines -> retry notice
        val session = EditorSession.Create(10)
        sessionManager.startSession(editor, player, session)
        sentPackets.clear()
        val commitData = NBTCompound().apply {
            setTag("line", NBTString(""))
            setTag("author", NBTString("DevAdmin"))
        }
        editor.onActionTriggered("commit", commitData, player, sessionManager)
        assertTrue(sentPackets.isNotEmpty())

        // Commit with valid lines -> success
        session.rawLines.add("Feature A implemented")
        session.rawLines.add("Fixed bug B")
        sentPackets.clear()
        editor.onActionTriggered("commit", commitData, player, sessionManager)

        // Verify storage received entry
        val stored = storage.getByUID(10)
        assertNotNull(stored)
        assertEquals(2, stored.lines.size)

        // Commit without permission -> rejection
        val noPermPlayer = TestPlayer(user, {}, isNativeAdmin = TriState.FALSE, permGranted = false)
        val noPermSession = EditorSession.Create(20).apply { rawLines.add("Line") }
        sessionManager.startSession(editor, noPermPlayer, noPermSession)
        editor.onActionTriggered("commit", commitData, noPermPlayer, sessionManager)
        assertEquals(null, storage.getByUID(20))

        ServerChangelogs.onShutdown()
    }
}
