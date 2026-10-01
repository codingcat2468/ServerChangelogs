package com.codingcat.changelogs.platformapi.player

import kotlinx.collections.immutable.ImmutableSet
import java.util.*

/**
 * Manager responsible for querying online players and adapting platform-native player representations.
 */
interface PlatformPlayerManager {
    /**
     * Immutable snapshot of currently connected players.
     */
    val onlinePlayers: ImmutableSet<PlatformPlayer>

    /**
     * Finds a connected player by their unique [uuid], returning `null` if not found.
     */
    fun getFromUUID(uuid: UUID): PlatformPlayer?

    /**
     * Finds a connected player by their username [name], returning `null` if not found.
     */
    fun getFromName(name: String): PlatformPlayer?

    /**
     * Operator shorthand for lookup by [uuid].
     */
    operator fun get(uuid: UUID): PlatformPlayer? = getFromUUID(uuid)

    /**
     * Operator shorthand for lookup by [name].
     */
    operator fun get(name: String): PlatformPlayer? = getFromName(name)

    /**
     * Wraps a platform-native player instance (e.g. Bukkit `Player` or Velocity `Player`) into [PlatformPlayer].
     *
     * @throws IllegalArgumentException if the provided native instance cannot be handled.
     */
    @Throws(IllegalArgumentException::class)
    fun fromNative(nativePlayer: Any): PlatformPlayer
}
