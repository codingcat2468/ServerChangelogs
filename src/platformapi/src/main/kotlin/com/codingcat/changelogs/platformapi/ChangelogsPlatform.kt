package com.codingcat.changelogs.platformapi

import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.item.NativeItemManager
import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.nio.file.Path

/**
 * Platform abstraction contract hosting server-specific services, lifecycle hooks, and metadata.
 */
interface ChangelogsPlatform {
    /**
     * Platform logger supporting Adventure component rendering.
     * Defined as a method to match Paper's [org.bukkit.plugin.Plugin.getComponentLogger]
     * and avoid JVM accidental override errors.
     */
    fun getComponentLogger(): ComponentLogger

    /**
     * Resolves the root data directory path dedicated to storing plugin configuration and changelogs.
     * Defined as a method to match Paper's [org.bukkit.plugin.Plugin.getDataPath]
     * and avoid JVM accidental override errors.
     */
    fun getDataPath(): Path

    /**
     * Coroutine scope bound to the platform plugin lifecycle.
     */
    val coroutineScope: CoroutineScope

    /**
     * Manager handling player lookups and adapting native player instances to platform representations.
     */
    val playerManager: PlatformPlayerManager

    /**
     * Manager handling subscription and dispatch of cross-platform events.
     */
    val eventManager: PlatformEventManager

    /**
     * Manager responsible for registering Brigadier command nodes with the underlying platform.
     */
    val commandManager: PlatformCommandManager

    /**
     * Manager providing native item stack conversion and validation services.
     */
    val nativeItemManager: NativeItemManager

    /**
     * Metadata describing the host server environment.
     */
    val platformMeta: PlatformMeta

    /**
     * Metadata describing this plugin distribution instance.
     */
    val changelogsMeta: ChangelogsMeta
}
