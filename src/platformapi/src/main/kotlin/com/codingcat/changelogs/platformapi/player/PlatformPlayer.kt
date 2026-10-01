package com.codingcat.changelogs.platformapi.player

import com.codingcat.changelogs.platformapi.audience.PermissionAudienceWrapper
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.util.*

/**
 * Common platform abstraction representing a connected player across Paper and Velocity servers.
 */
interface PlatformPlayer : PermissionAudienceWrapper {
    /**
     * Unique identifier of this player.
     */
    val uniqueId: UUID

    /**
     * Current username of this player.
     */
    val name: String

    /**
     * Active client locale configured by the player's Minecraft client.
     */
    val clientLocale: Locale

    /**
     * Indicates whether this connection represents the player's initial join to the server.
     */
    val isFirstJoin: Boolean

    /**
     * Native administrator status (e.g. Bukkit `isOp()`), returning [TriState.NOT_SET] if unsupported by the platform.
     */
    val isNativeAdmin: TriState

    /**
     * Disconnects the player from the network with an optional disconnect reason component.
     */
    fun kick(reason: Component?)

    /**
     * Resolves the underlying PacketEvents `User` associated with this player's active network channel.
     */
    fun asPacketEventsUser(): Any
}
