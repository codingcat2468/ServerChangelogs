package com.codingcat.changelogs.velocity.player

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Player manager implementation providing Velocity [Player] resolution and wrapping.
 */
object VelocityPlayerManager : PlatformPlayerManager {
    lateinit var server: ProxyServer
    override val onlinePlayers: ImmutableSet<PlatformPlayer>
        get() = this.server.allPlayers.map { this.fromNative(it) }.toImmutableSet()

    override fun getFromUUID(uuid: UUID): PlatformPlayer? {
        return this.server.getPlayer(uuid).getOrNull()?.let { this.fromNative(it) }
    }

    override fun getFromName(name: String): PlatformPlayer? {
        return this.server.getPlayer(name).getOrNull()?.let { this.fromNative(it) }
    }

    @Throws(IllegalArgumentException::class)
    override fun fromNative(nativePlayer: Any): PlatformPlayer {
        require(nativePlayer is Player) {
            "Expected native velocity player but got ${nativePlayer}"
        }
        return VelocityPlayerWrapper(nativePlayer)
    }
}
