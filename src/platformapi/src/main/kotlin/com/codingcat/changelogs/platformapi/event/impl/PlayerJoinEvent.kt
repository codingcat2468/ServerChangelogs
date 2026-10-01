package com.codingcat.changelogs.platformapi.event.impl

import com.codingcat.changelogs.platformapi.player.PlatformPlayer

/**
 * Dispatched when a player connects or transitions into the active play phase.
 */
class PlayerJoinEvent(
    val player: PlatformPlayer,
    val justConnected: Boolean,
) : PlatformEvent
