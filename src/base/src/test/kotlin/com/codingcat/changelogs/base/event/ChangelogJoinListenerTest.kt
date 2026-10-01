package com.codingcat.changelogs.base.event

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.config.PluginConfig
import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import com.codingcat.changelogs.base.dialog.DialogSessionManager
import com.codingcat.changelogs.base.dialog.PluginDialog
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog
import com.codingcat.changelogs.platformapi.ChangelogsPlatform
import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import com.codingcat.changelogs.platformapi.item.NativeItemManager
import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import com.github.retrooper.packetevents.protocol.dialog.Dialog
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.util.TriState
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangelogJoinListenerTest {
    private class DummyPlayer(
        override val uniqueId: UUID = UUID.randomUUID(),
        override val isFirstJoin: Boolean = false,
    ) : PlatformPlayer {
        var shownDialog: Dialog? = null

        override val name: String = "TestPlayer"
        override val clientLocale: Locale = Locale.US
        override val isNativeAdmin: TriState = TriState.NOT_SET
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = Unit
    }

    private class TestDialog(
        storage: ChangelogStorage,
    ) : ChangelogDialog(
        storage = storage,
        holder = PluginDialog.Holder,
        dateFormatter = java.time.format.DateTimeFormatter.ISO_INSTANT,
        addHeader = false,
        headerItem = null,
        useFallbackPermissions = false,
    ) {
        override val id: String = "changelog_view"
        var showCount = 0

        override fun showTo(
            player: PlatformPlayer,
            sessionManager: DialogSessionManager,
            packetPhase: com.codingcat.changelogs.base.dialog.DialogPackets.PacketPhase,
        ) {
            showCount++
        }

        override fun onActionTriggered(
            action: String,
            data: NBTCompound?,
            source: PlatformPlayer,
            sessionManager: DialogSessionManager,
        ) {
        }
    }

    private class TestStorage(val entries: List<ChangelogEntry>) : ChangelogStorage {
        val firstSeen = mutableMapOf<UUID, Instant>()
        override val displayName: String = "MockStorage"
        override fun init() {}
        override fun shutdown() {}
        override fun storeEntry(entry: ChangelogEntry) {}
        override fun updateEntry(entry: ChangelogEntry) {}
        override fun removeEntry(uid: Int): Boolean = false
        override fun listEntries(): List<ChangelogEntry> = entries
        override fun getByUID(uid: Int): ChangelogEntry? = entries.find { it.uid == uid }
        override fun markAllAsRead(uids: Collection<Int>, player: UUID) {}
        override fun markAsRead(uid: Int, player: UUID) {}
        override fun nextUID(): Int = 1
        override fun getFirstSeenAt(player: UUID): Instant? = firstSeen[player]
        override fun recordFirstSeen(player: UUID, seenAt: Instant): Instant =
            firstSeen.computeIfAbsent(player) { seenAt }
    }

    private class TestPlatform(val dataDir: Path) : ChangelogsPlatform {
        val registeredListeners = mutableListOf<Class<*>>()

        override fun getComponentLogger(): ComponentLogger = ComponentLogger.logger("Test")
        override fun getDataPath(): Path = dataDir
        override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
        override val playerManager: PlatformPlayerManager = object : PlatformPlayerManager {
            override val onlinePlayers: ImmutableSet<PlatformPlayer> = emptySet<PlatformPlayer>().toPersistentSet()
            override fun getFromName(name: String): PlatformPlayer? = null
            override fun getFromUUID(uuid: UUID): PlatformPlayer? = null
            override fun fromNative(nativePlayer: Any): PlatformPlayer = throw UnsupportedOperationException()
        }
        override val eventManager: PlatformEventManager = object : PlatformEventManager {
            override fun <T : PlatformEvent> registerListener(eventCls: Class<T>, listener: (T) -> Unit) {
                registeredListeners.add(eventCls)
            }

            override fun unregisterListener(listener: Any) {}
            override fun unregisterIf(listenerPredicate: (Any) -> Boolean) {
                registeredListeners.clear()
            }

            override fun unregisterAll() {
                registeredListeners.clear()
            }
        }
        override val commandManager: PlatformCommandManager = object : PlatformCommandManager {
            override fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>) {}
            override fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource =
                throw UnsupportedOperationException()
        }
        override val nativeItemManager: NativeItemManager = object : NativeItemManager {
            override fun adaptToPEStack(nativeStack: Any): ItemStack = throw UnsupportedOperationException()
            override fun createNativeStack(key: Key, amount: Int): Any? = null
            override fun applyComponentStr(nativeStack: Any, componentStr: String): Any = nativeStack
        }
        override val platformMeta: PlatformMeta = object : PlatformMeta {
            override val name: Component = Component.text("MockServer", NamedTextColor.GREEN)
            override val version: String = "1.0"
        }
        override val changelogsMeta: ChangelogsMeta = object : ChangelogsMeta {
            override val name: String = "ServerChangelogs"
            override val version: String = "1.0"
            override val description: String = "Desc"
            override val authors: Set<String> = setOf("Author")
        }
    }

    @Test
    fun testCheckShowDialog() {
        val tempDir = Files.createTempDirectory("join_listener_test")
        val configFile = tempDir.resolve("config.yml")
        configFile.writeText(
            """
            dialog_phase: "PLAY"
            use_native_fallback_permissions: false
            """.trimIndent(),
        )

        val platform = TestPlatform(tempDir)
        ServerChangelogs.platform = platform
        ServerChangelogs.setConfig(PluginConfig(configFile).also { it.reload() })

        val entry = ChangelogEntry(
            uid = 1,
            lines = listOf(Component.text("Update 1")),
            recordedAt = Instant.now().minusSeconds(60),
            author = null,
            playersRead = mutableSetOf(),
        )
        val storage = TestStorage(listOf(entry))
        val storageField = ServerChangelogs::class.java.getDeclaredField("changelogStorage")
        storageField.isAccessible = true
        storageField.set(ServerChangelogs, storage)

        // Inject custom dialog into Holder
        val holderField = PluginDialog.Holder::class.java.getDeclaredField("dialogMap")
        holderField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val dialogMap = holderField.get(PluginDialog.Holder) as MutableMap<String, PluginDialog>
        val testDialog = TestDialog(storage)
        dialogMap[testDialog.id] = testDialog

        // Player on first join should NOT show dialog
        val firstJoinPlayer = DummyPlayer(isFirstJoin = true)
        assertFalse(ChangelogJoinListener.checkShowDialog(firstJoinPlayer, freeze = false))

        // Normal player with unread changelogs should show dialog
        val normalPlayer = DummyPlayer(isFirstJoin = false)
        storage.firstSeen[normalPlayer.uniqueId] = Instant.EPOCH
        assertTrue(ChangelogJoinListener.checkShowDialog(normalPlayer, freeze = true))
        assertTrue(DialogSessionManager.isFrozen(normalPlayer))
        DialogSessionManager.unfreeze(normalPlayer)

        // Player who already read all changelogs should NOT show dialog
        entry.playersRead.add(normalPlayer.uniqueId)
        assertFalse(ChangelogJoinListener.checkShowDialog(normalPlayer, freeze = false))

        // Register and unregister events
        ChangelogJoinListener.registerEvents(platform.eventManager)
        assertTrue(platform.registeredListeners.isNotEmpty())

        ChangelogJoinListener.unregisterEvents(platform.eventManager)
        assertTrue(platform.registeredListeners.isEmpty())

        tempDir.toFile().deleteRecursively()
    }
}
