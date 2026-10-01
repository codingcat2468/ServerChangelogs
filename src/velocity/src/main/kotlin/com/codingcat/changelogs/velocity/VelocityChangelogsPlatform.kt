package com.codingcat.changelogs.velocity

import com.codingcat.changelogs.base.Entrypoints
import com.codingcat.changelogs.base.compat.PacketEventsNativeItemManager
import com.codingcat.changelogs.platformapi.ChangelogsPlatform
import com.codingcat.changelogs.platformapi.Entrypoint
import com.codingcat.changelogs.velocity.command.VelocityCommandManager
import com.codingcat.changelogs.velocity.event.VelocityEventManager
import com.codingcat.changelogs.velocity.meta.VelocityPlatformMeta
import com.codingcat.changelogs.velocity.meta.VelocityPluginChangelogsMeta
import com.codingcat.changelogs.velocity.player.VelocityPlayerManager
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.PacketEventsAPI
import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer
import com.github.shynixn.mccoroutine.velocity.scope
import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPreShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path

/**
 * Velocity platform adapter and plugin implementation for ServerChangelogs.
 */
class VelocityChangelogsPlatform @Inject constructor(
    server: ProxyServer,
    private val componentLogger: ComponentLogger,
    @DataDirectory private val dataDirectory: Path,
    private val pluginContainer: PluginContainer,
    suspendingPluginContainer: SuspendingPluginContainer,
) : ChangelogsPlatform {
    init {
        suspendingPluginContainer.initialize(this)
    }

    private val entrypoint: Entrypoint = Entrypoints.create(this)

    override fun getComponentLogger(): ComponentLogger = componentLogger

    override fun getDataPath(): Path = dataDirectory

    override val coroutineScope: CoroutineScope
        get() = pluginContainer.scope

    override val platformMeta: VelocityPlatformMeta = VelocityPlatformMeta(server.version)

    override val changelogsMeta: VelocityPluginChangelogsMeta =
        VelocityPluginChangelogsMeta(pluginContainer.description)

    override val playerManager: VelocityPlayerManager = VelocityPlayerManager.apply {
        this.server = server
    }

    override val eventManager: VelocityEventManager by lazy {
        VelocityEventManager.apply {
            this.eventManager = server.eventManager
            this.platform = this@VelocityChangelogsPlatform
        }
    }

    override val nativeItemManager: PacketEventsNativeItemManager by lazy { PacketEventsNativeItemManager }

    override val commandManager: VelocityCommandManager = VelocityCommandManager.apply {
        this.commandManager = server.commandManager
        this.platform = this@VelocityChangelogsPlatform
    }

    private var earlyInitSucceeded = false

    @Subscribe(order = PostOrder.LATE)
    suspend fun onProxyInitialization(event: ProxyInitializeEvent) {
        runCatching {
            Class.forName("com.github.retrooper.packetevents.PacketEvents")
        }.onFailure {
            throw RuntimeException(
                "Unable to find PacketEvents! Please ensure you have the latest version of that plugin installed!",
                it,
            )
        }

        earlyInitSucceeded = true

        runCatching {
            entrypoint.onStart()
        }.onFailure {
            throw RuntimeException("Failed to start platform entrypoint", it)
        }
    }

    @Subscribe
    @Suppress("UnstableApiUsage")
    suspend fun onProxyPreShutdown(event: ProxyPreShutdownEvent) {
        if (!earlyInitSucceeded) return
        try {
            runCatching {
                entrypoint.onShutdown()
            }.onFailure {
                throw RuntimeException("Failed to shut down platform entrypoint", it)
            }
        } finally {
            eventManager.unregisterAll()
        }
    }

    companion object {
        val packetEventsApi: PacketEventsAPI<out Any> by lazy { PacketEvents.getAPI() }
    }
}
