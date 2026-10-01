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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrigadierCommandNodeTest {
    private class DummyPlayer(
        override val name: String = "TestPlayer",
        override val isNativeAdmin: TriState = TriState.NOT_SET,
        private val permissions: Map<String, TriState> = emptyMap(),
    ) : PlatformPlayer {
        override val uniqueId: UUID = UUID.randomUUID()
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker =
            PermissionChecker { permissions[it] ?: TriState.NOT_SET }

        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = Unit
    }

    private class DummyCommandSource(
        override val executingPlayer: PlatformPlayer?,
        private val permissions: Map<String, TriState> = emptyMap(),
    ) : PlatformCommandSource {
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker =
            PermissionChecker { permissions[it] ?: TriState.NOT_SET }
    }

    private class DummyCommandManager : PlatformCommandManager {
        override fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>) {}
        override fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource {
            return platformCommandSource as PlatformCommandSource
        }
    }

    private class DummyPlatform(
        val dataDir: Path,
        override val commandManager: PlatformCommandManager = DummyCommandManager(),
    ) : ChangelogsPlatform {
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

    @Test
    fun testRequirePermissionStandard() {
        val tempDir = Files.createTempDirectory("cmd_perm_test")
        val configFile = tempDir.resolve("config.yml")
        configFile.writeText("use_native_fallback_permissions: false\n")

        val platform = DummyPlatform(tempDir)
        ServerChangelogs.platform = platform
        ServerChangelogs.setConfig(PluginConfig(configFile).also { it.reload() })

        val predicate = BrigadierCommandNode.requirePermission("test.perm", ServerChangelogs, false)

        val grantedSource = DummyCommandSource(
            executingPlayer = null,
            permissions = mapOf("server_changelogs.test.perm" to TriState.TRUE),
        )
        assertTrue(predicate(grantedSource))

        val deniedSource = DummyCommandSource(
            executingPlayer = null,
            permissions = mapOf("server_changelogs.test.perm" to TriState.FALSE),
        )
        assertFalse(predicate(deniedSource))

        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun testRequirePermissionNativeFallback() {
        val tempDir = Files.createTempDirectory("cmd_native_perm_test")
        val configFile = tempDir.resolve("config.yml")
        configFile.writeText("use_native_fallback_permissions: true\n")

        val platform = DummyPlatform(tempDir)
        ServerChangelogs.platform = platform
        ServerChangelogs.setConfig(PluginConfig(configFile).also { it.reload() })

        val predicate = BrigadierCommandNode.requirePermission("test.perm", ServerChangelogs, false)

        val adminPlayer = DummyPlayer(isNativeAdmin = TriState.TRUE)
        val adminSource = DummyCommandSource(executingPlayer = adminPlayer)
        assertTrue(predicate(adminSource))

        val nonAdminPlayer = DummyPlayer(isNativeAdmin = TriState.FALSE)
        val nonAdminSource = DummyCommandSource(executingPlayer = nonAdminPlayer)
        assertFalse(predicate(nonAdminSource))

        val consoleSource = DummyCommandSource(executingPlayer = null)
        assertTrue(predicate(consoleSource))

        tempDir.toFile().deleteRecursively()
    }
}
