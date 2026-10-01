package com.codingcat.changelogs.paper.event

import com.codingcat.changelogs.paper.player.PaperConfigurationPhaseConnectionWrapper
import com.codingcat.changelogs.paper.player.PaperPlayerManager
import com.codingcat.changelogs.platformapi.event.impl.PlayerEnterConfigurationPhaseEvent
import com.codingcat.changelogs.platformapi.event.impl.PlayerJoinEvent
import com.codingcat.changelogs.platformapi.event.util.PlatformEventMappings
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import org.bukkit.event.Event

class PaperEventMappings(
    playerManager: PaperPlayerManager,
) : PlatformEventMappings<Event>() {
    init {
        register(
            PlayerJoinEvent::class.java,
            org.bukkit.event.player.PlayerJoinEvent::class.java,
        ) { ev ->
            PlayerJoinEvent(
                playerManager.fromNative(ev.player),
                true,
            )
        }
        register(
            PlayerEnterConfigurationPhaseEvent::class.java,
            AsyncPlayerConnectionConfigureEvent::class.java,
        ) { ev ->
            PlayerEnterConfigurationPhaseEvent(
                PaperConfigurationPhaseConnectionWrapper(ev.connection),
                true,
            )
        }
    }
}
