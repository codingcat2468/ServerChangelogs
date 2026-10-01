package com.codingcat.changelogs.base.event

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.dialog.DialogPackets
import com.codingcat.changelogs.base.dialog.DialogSessionManager
import com.codingcat.changelogs.base.dialog.PluginDialog
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog
import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.event.impl.PlayerEnterConfigurationPhaseEvent
import com.codingcat.changelogs.platformapi.event.impl.PlayerJoinEvent
import com.codingcat.changelogs.platformapi.event.registerIdentified
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component

/**
 * Event handler displaying unread changelogs to players upon joining or entering the configuration phase.
 */
object ChangelogJoinListener {
    private val KEY: Key by lazy { ServerChangelogs.key("listener/changelog_display") }

    /**
     * Registers the join listener with [manager] according to configured packet phase.
     */
    fun registerEvents(manager: PlatformEventManager) {
        val config = ServerChangelogs.pluginConfig()
        when (config.dialogPacketPhase) {
            DialogPackets.PacketPhase.PLAY -> manager.registerIdentified<PlayerJoinEvent>(KEY) { e ->
                checkShowDialog(e.player, false)
            }

            DialogPackets.PacketPhase.CONFIGURATION -> manager.registerIdentified<PlayerEnterConfigurationPhaseEvent>(
                KEY,
            ) { e ->
                if (checkShowDialog(e.player, true)) {
                    ServerChangelogs.info("console.join.frozen", Component.text(e.player.uniqueId.toString()))
                    DialogSessionManager.waitForUnfreeze(e.player)
                }
            }
        }
    }

    fun unregisterEvents(manager: PlatformEventManager) {
        manager.unregisterIdentified(KEY)
    }

    fun checkShowDialog(player: PlatformPlayer, freeze: Boolean): Boolean {
        val storage = ServerChangelogs.changelogStorage
        val firstSeenAt = storage.recordFirstSeen(player.uniqueId, java.time.Instant.now())
        val unreadChangelogs = storage.listEntries().any { entry ->
            !entry.hasRead(player) && entry.recordedAt.isAfter(firstSeenAt)
        }
        if (!unreadChangelogs) return false
        val dialog = PluginDialog.Holder.getFromType<ChangelogDialog>()
        if (freeze) DialogSessionManager.freeze(player)
        dialog.showTo(player, DialogSessionManager, ServerChangelogs.pluginConfig().dialogPacketPhase)
        return true
    }
}
