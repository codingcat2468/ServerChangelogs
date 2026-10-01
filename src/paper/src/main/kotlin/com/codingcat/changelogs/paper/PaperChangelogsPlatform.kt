package com.codingcat.changelogs.paper

import com.codingcat.changelogs.base.Entrypoints
import com.codingcat.changelogs.paper.command.PaperCommandManager
import com.codingcat.changelogs.paper.event.PaperEventManager
import com.codingcat.changelogs.paper.item.PaperNativeItemManager
import com.codingcat.changelogs.paper.meta.PaperPlatformMeta
import com.codingcat.changelogs.paper.meta.PaperPluginChangelogsMeta
import com.codingcat.changelogs.paper.player.PaperPlayerManager
import com.codingcat.changelogs.platformapi.ChangelogsPlatform
import com.codingcat.changelogs.platformapi.Entrypoint
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.scope
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path

/**
 * Paper platform adapter and plugin implementation for ServerChangelogs.
 */
class PaperChangelogsPlatform : SuspendingJavaPlugin(), ChangelogsPlatform {
    companion object {
        lateinit var instance: PaperChangelogsPlatform
            private set
    }

    init {
        instance = this
    }

    private val entrypoint: Entrypoint by lazy { Entrypoints.create(this) }

    override val platformMeta: PaperPlatformMeta = PaperPlatformMeta

    override val changelogsMeta: PaperPluginChangelogsMeta by lazy { PaperPluginChangelogsMeta(pluginMeta) }

    override val nativeItemManager: PaperNativeItemManager = PaperNativeItemManager

    override fun getComponentLogger(): ComponentLogger = super.getComponentLogger()

    override fun getDataPath(): Path = super.getDataPath()

    override val coroutineScope: CoroutineScope
        get() = this.scope

    override val playerManager: PaperPlayerManager = PaperPlayerManager

    override val eventManager: PaperEventManager = PaperEventManager

    override val commandManager: PaperCommandManager = PaperCommandManager

    private var earlyInitSucceeded = false

    override suspend fun onEnableAsync() {
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

        commandManager.registerEvents(lifecycleManager)
    }

    override suspend fun onDisableAsync() {
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
}
