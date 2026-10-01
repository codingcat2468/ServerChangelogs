package com.codingcat.changelogs.platformapi.event.impl

import com.codingcat.changelogs.platformapi.player.PlatformPlayer

/**
 * Dispatched when a player enters the network configuration phase.
 *
 * Platform implementations should handle this event asynchronously; blocking the handler thread
 * causes the connection to remain waiting in the configuration phase.
 */
class PlayerEnterConfigurationPhaseEvent(
    val player: PlatformPlayer,
    val initialConfiguration: Boolean,
) : PlatformEvent
