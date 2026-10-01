package com.codingcat.changelogs.base.command

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.config.PluginConfig
import com.codingcat.changelogs.platformapi.ChangelogsPlatform
import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.item.NativeItemManager
import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
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
import java.util.*
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbstractCommandTest {
    private class DummyPlayer(
        override val name: String = "TestPlayer",
        override val isNativeAdmin: TriState = TriState.NOT_SET,
    ) : PlatformPlayer {
        override val uniqueId: UUID = UUID.randomUUID()
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = Unit
    }

    private class DummyCommandSource(
        override val executingPlayer: PlatformPlayer?,
    ) : PlatformCommandSource {
        val messages = mutableListOf<Component>()
        override fun asAudience(): Audience = object : Audience {
            override fun sendMessage(message: Component) {
                messages.add(message)
            }
        }

        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
    }

    private class DummyCommandManager : PlatformCommandManager {
        override fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>) {}
        override fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource {
            return platformCommandSource as PlatformCommandSource
        }
    }

    private class DummyPlatform(
        val dataDir: Path,
        override val playerManager: PlatformPlayerManager,
        override val commandManager: PlatformCommandManager = DummyCommandManager(),
    ) : ChangelogsPlatform {
        override fun getComponentLogger(): ComponentLogger = ComponentLogger.logger("Test")
        override fun getDataPath(): Path = dataDir
        override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
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

    private class TestCommand(
        featureEnabled: Boolean = true,
    ) : AbstractCommand(
        name = "testcmd",
        description = "Test description",
        aliases = listOf("tcmd"),
        permission = null,
        featureEnabled = featureEnabled,
    ) {
        var playerRan = false
        var sourceRan = false
        var suspendPlayerRan = false
        var suspendSourceRan = false

        override fun buildCommand(builder: LiteralArgumentBuilder<Any>) {
            builder.executesSource { _, _ -> sourceRan = true }

            builder.literal("playeronly") {
                executesPlayer { _, _ -> playerRan = true }
            }

            builder.literal("playersuspend") {
                executesPlayerSuspend { _, _ -> suspendPlayerRan = true }
            }

            builder.literal("sourcesuspend") {
                executesSourceSuspend { _, _ -> suspendSourceRan = true }
            }

            builder.literal("witharg") {
                argument("str", StringArgumentType.word()) {
                    executes { com.mojang.brigadier.Command.SINGLE_SUCCESS }
                }
            }

            builder.literal("targetplayer") {
                player("target") {
                    executes { com.mojang.brigadier.Command.SINGLE_SUCCESS }
                }
            }
        }
    }

    @Test
    fun testCommandExecutionAndDsl() {
        val tempDir = Files.createTempDirectory("abstract_cmd_test")
        val configFile = tempDir.resolve("config.yml")
        configFile.writeText("use_native_fallback_permissions: false\n")

        val player = DummyPlayer("Alex")
        val playerManager = object : PlatformPlayerManager {
            override val onlinePlayers: ImmutableSet<PlatformPlayer> = setOf<PlatformPlayer>(player).toPersistentSet()
            override fun getFromName(name: String): PlatformPlayer? = if (name == "Alex") player else null
            override fun getFromUUID(uuid: UUID): PlatformPlayer? = if (uuid == player.uniqueId) player else null
            override fun fromNative(nativePlayer: Any): PlatformPlayer = player
        }

        val platform = DummyPlatform(tempDir, playerManager)
        ServerChangelogs.platform = platform
        ServerChangelogs.setConfig(PluginConfig(configFile).also { it.reload() })

        val cmd = TestCommand(featureEnabled = true)
        val node = cmd.build(ServerChangelogs)
        assertEquals("testcmd", node.name)
        assertEquals(listOf("tcmd"), cmd.aliases)
        assertEquals("Test description", cmd.description)

        val dispatcher = CommandDispatcher<Any>()
        dispatcher.root.addChild(node)

        val playerSource = DummyCommandSource(executingPlayer = player)
        val consoleSource = DummyCommandSource(executingPlayer = null)

        // Execute root source
        dispatcher.execute("testcmd", playerSource)
        assertTrue(cmd.sourceRan)

        // Execute player-only from player
        dispatcher.execute("testcmd playeronly", playerSource)
        assertTrue(cmd.playerRan)

        // Execute player-only from console -> should be rejected and send message
        dispatcher.execute("testcmd playeronly", consoleSource)
        assertEquals(1, consoleSource.messages.size)

        // Execute player suspend
        dispatcher.execute("testcmd playersuspend", playerSource)
        assertTrue(cmd.suspendPlayerRan)

        // Execute source suspend
        dispatcher.execute("testcmd sourcesuspend", consoleSource)
        assertTrue(cmd.suspendSourceRan)

        // Execute with arg
        val argRes = dispatcher.execute("testcmd witharg hello", playerSource)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, argRes)

        // Execute targetplayer
        val playerRes = dispatcher.execute("testcmd targetplayer Alex", playerSource)
        assertEquals(com.mojang.brigadier.Command.SINGLE_SUCCESS, playerRes)

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testDisabledFeatureCommand() {
        val tempDir = Files.createTempDirectory("abstract_cmd_disabled_test")
        val configFile = tempDir.resolve("config.yml")
        configFile.writeText("use_native_fallback_permissions: false\n")

        val platform = DummyPlatform(
            tempDir,
            object : PlatformPlayerManager {
                override val onlinePlayers: ImmutableSet<PlatformPlayer> = emptySet<PlatformPlayer>().toPersistentSet()
                override fun getFromName(name: String): PlatformPlayer? = null
                override fun getFromUUID(uuid: UUID): PlatformPlayer? = null
                override fun fromNative(nativePlayer: Any): PlatformPlayer = throw UnsupportedOperationException()
            },
        )
        ServerChangelogs.platform = platform

        val cmd = TestCommand(featureEnabled = false)
        val node = cmd.build(ServerChangelogs)

        val dispatcher = CommandDispatcher<Any>()
        dispatcher.root.addChild(node)

        val source = DummyCommandSource(executingPlayer = null)
        dispatcher.execute("testcmd", source)
        assertEquals(1, source.messages.size)

        tempDir.toFile().deleteRecursively()
    }
}
