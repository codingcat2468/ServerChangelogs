package com.codingcat.changelogs.base

import com.codingcat.changelogs.base.compat.PacketEventsNativeItemManager
import com.codingcat.changelogs.base.dialog.PluginDialog
import com.codingcat.changelogs.base.dialog.ui.editor.ChangelogEditorDialog
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
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.kyori.adventure.util.TriState
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.writeText
import kotlin.test.*

class ServerChangelogsTest {
    private lateinit var tempDir: Path
    private val sentMessages = mutableListOf<Component>()

    private class DummyUser(
        uuid: UUID = UUID.randomUUID(),
        name: String = "AdminPlayer",
    ) : com.github.retrooper.packetevents.protocol.player.User(
        null,
        com.github.retrooper.packetevents.protocol.ConnectionState.PLAY,
        com.github.retrooper.packetevents.protocol.player.ClientVersion.V_1_21,
        com.github.retrooper.packetevents.protocol.player.UserProfile(uuid, name),
    ) {
        override fun sendPacket(packet: com.github.retrooper.packetevents.wrapper.PacketWrapper<*>) {}
    }

    private class TestPlayer(
        override val name: String = "AdminPlayer",
        val dummyUser: DummyUser = DummyUser(),
    ) : PlatformPlayer {
        override val uniqueId: UUID = dummyUser.uuid
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override val isNativeAdmin: TriState = TriState.TRUE
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = dummyUser
    }

    private class TestCommandSource(
        val messages: MutableList<Component>,
        override val executingPlayer: PlatformPlayer? = null,
    ) : PlatformCommandSource {
        override fun asAudience(): Audience = object : Audience {
            override fun sendMessage(message: Component) {
                messages.add(message)
            }
        }

        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
    }

    private class TestCommandManager : PlatformCommandManager {
        val registeredNodes = mutableListOf<LiteralCommandNode<*>>()
        val dispatcher = CommandDispatcher<Any>()

        override fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>) {
            @Suppress("UNCHECKED_CAST")
            val anyNode = node as LiteralCommandNode<Any>
            registeredNodes.add(anyNode)
            dispatcher.root.addChild(anyNode)
            for (alias in aliases) {
                dispatcher.register(
                    com.mojang.brigadier.builder.LiteralArgumentBuilder.literal<Any>(alias)
                        .redirect(anyNode),
                )
            }
        }

        override fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource {
            return platformCommandSource as PlatformCommandSource
        }
    }

    private class TestEventManager : PlatformEventManager {
        val listeners = mutableListOf<Any>()
        override fun <T : PlatformEvent> registerListener(eventCls: Class<T>, listener: (T) -> Unit) {
            listeners.add(listener)
        }

        override fun unregisterListener(listener: Any) {
            listeners.remove(listener)
        }

        override fun unregisterIf(listenerPredicate: (Any) -> Boolean) {
            listeners.removeAll(listenerPredicate)
        }

        override fun unregisterAll() {
            listeners.clear()
        }
    }

    private class TestPlatform(
        val dir: Path,
        override val commandManager: TestCommandManager = TestCommandManager(),
        override val eventManager: TestEventManager = TestEventManager(),
    ) : ChangelogsPlatform {
        override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
        override val nativeItemManager: NativeItemManager = PacketEventsNativeItemManager
        override val playerManager: PlatformPlayerManager = object : PlatformPlayerManager {
            val player = TestPlayer()
            override val onlinePlayers: ImmutableSet<PlatformPlayer> = setOf<PlatformPlayer>(player).toPersistentSet()
            override fun getFromName(name: String): PlatformPlayer? = if (name == player.name) player else null
            override fun getFromUUID(uuid: UUID): PlatformPlayer? = if (uuid == player.uniqueId) player else null
            override fun fromNative(nativePlayer: Any): PlatformPlayer = player
        }
        override val platformMeta: PlatformMeta = object : PlatformMeta {
            override val name: Component = Component.text("TestServer", NamedTextColor.BLUE)
            override val version: String = "1.0.0"
        }
        override val changelogsMeta: ChangelogsMeta = object : ChangelogsMeta {
            override val name: String = "ServerChangelogs"
            override val version: String = "2.0.0"
            override val description: String = "Test Changelogs"
            override val authors: Set<String> = setOf("GuavaDealer")
        }

        override fun getComponentLogger(): ComponentLogger = ComponentLogger.logger("TestServer")
        override fun getDataPath(): Path = dir
    }

    @BeforeTest
    fun setUp() {
        TestPacketEvents.setup()
        tempDir = Files.createTempDirectory("server_changelogs_test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testLifecycleStartReloadShutdown() = runBlocking {
        val platform = TestPlatform(tempDir)
        ServerChangelogs.platform = platform

        ServerChangelogs.onStart()

        assertNotNull(ServerChangelogs.changelogStorage)
        assertNotNull(ServerChangelogs.logger)
        assertNotNull(ServerChangelogs.pluginConfig())

        // Logging helper tests
        ServerChangelogs.info("console.startup")
        ServerChangelogs.warn("console.startup")
        ServerChangelogs.error("console.startup", RuntimeException("test error"))

        // Key helper tests
        val key = ServerChangelogs.key("sample")
        assertEquals(ServerChangelogs.NAMESPACE, key.namespace())
        assertEquals("sample", key.value())
        val generatedKey = ServerChangelogs.KEY_GENERATOR("sample2")
        assertEquals("sample2", generatedKey.value())

        // Reload
        ServerChangelogs.reload(force = false)
        ServerChangelogs.reload(force = true)

        // Reload failure on invalid config
        tempDir.resolve("config.yml").writeText("invalid: [yaml: broken\n")
        assertFailsWith<Exception> {
            ServerChangelogs.reload(force = false)
        }

        // Restore valid config
        tempDir.resolve("config.yml").writeText("use_native_fallback_permissions: false\n")
        ServerChangelogs.reload(force = true)

        // Shutdown
        ServerChangelogs.onShutdown()
    }

    @Test
    fun testCommandsExecution() = runBlocking {
        val platform = TestPlatform(tempDir)
        ServerChangelogs.platform = platform

        ServerChangelogs.onStart()

        val cmdManager = platform.commandManager
        val source = TestCommandSource(sentMessages)

        // Execute root command
        val rootResult = cmdManager.dispatcher.execute("server_changelogs", source)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, rootResult)
        assertTrue(sentMessages.isNotEmpty())

        // Execute info subcommand
        val infoResult = cmdManager.dispatcher.execute("server_changelogs info", source)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, infoResult)

        // Execute reload subcommand
        val reloadResult = cmdManager.dispatcher.execute("server_changelogs reload", source)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, reloadResult)

        // Execute reload --force subcommand
        val reloadForceResult = cmdManager.dispatcher.execute("server_changelogs reload --force", source)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, reloadForceResult)

        // Execute aliases
        val sclResult = cmdManager.dispatcher.execute("scl info", source)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, sclResult)

        val changelogsResult = cmdManager.dispatcher.execute("changelogs info", source)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, changelogsResult)

        // Dedicated command and player subcommands
        val player = platform.playerManager.onlinePlayers.first()
        val playerSource = TestCommandSource(sentMessages, executingPlayer = player)

        val viewResult = cmdManager.dispatcher.execute("server_changelogs view", playerSource)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, viewResult)

        val createResult = cmdManager.dispatcher.execute("server_changelogs create", playerSource)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, createResult)

        val dedicatedResult = cmdManager.dispatcher.execute("changelog", playerSource)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, dedicatedResult)

        ServerChangelogs.onShutdown()
    }

    @Test
    fun testReloadRejectionWhenEditorActive() = runBlocking {
        val platform = TestPlatform(tempDir)
        ServerChangelogs.platform = platform

        ServerChangelogs.onStart()

        // Simulate active saved session in editor dialog to trigger DestroyRejectedException
        val editor = ServerChangelogs.dialogHolder.getFromType<ChangelogEditorDialog>()
        // Reflection to inject saved session into ChangelogEditorDialog.savedSessions
        val savedSessionsField = ChangelogEditorDialog::class.java.getDeclaredField("savedSessions")
        savedSessionsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val savedSessions = savedSessionsField.get(editor) as MutableMap<UUID, Any>
        savedSessions[UUID.randomUUID()] = com.codingcat.changelogs.base.dialog.ui.editor.EditorSession.Create(1)

        // reload(force = false) should fail
        assertFailsWith<PluginDialog.DestroyRejectedException> {
            ServerChangelogs.reload(force = false)
        }

        // reload(force = true) should succeed
        ServerChangelogs.reload(force = true)

        ServerChangelogs.onShutdown()
    }
}
