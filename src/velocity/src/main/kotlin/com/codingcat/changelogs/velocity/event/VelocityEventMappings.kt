package com.codingcat.changelogs.velocity.event

import com.codingcat.changelogs.platformapi.event.impl.PlayerEnterConfigurationPhaseEvent
import com.codingcat.changelogs.platformapi.event.impl.PlayerJoinEvent
import com.codingcat.changelogs.platformapi.event.util.PlatformEventMappings
import com.codingcat.changelogs.velocity.player.VelocityPlayerManager
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.player.configuration.PlayerConfigurationEvent

class VelocityEventMappings(
    playerManager: VelocityPlayerManager,
) : PlatformEventMappings<Any>() {
    init {
        register(
            PlayerJoinEvent::class.java,
            ServerPostConnectEvent::class.java,
        ) { event ->
            PlayerJoinEvent(
                playerManager.fromNative(event.player),
                event.previousServer == null,
            )
        }
        register(
            PlayerEnterConfigurationPhaseEvent::class.java,
            PlayerConfigurationEvent::class.java,
        ) { event ->
            PlayerEnterConfigurationPhaseEvent(
                playerManager.fromNative(event.player()),
                event.server().previousServer.isEmpty,
            )
        }
    }
}
