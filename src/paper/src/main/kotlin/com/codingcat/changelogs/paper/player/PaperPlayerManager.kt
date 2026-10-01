package com.codingcat.changelogs.paper.player

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * Player manager implementation providing Paper [Player] resolution and wrapping.
 */
object PaperPlayerManager : PlatformPlayerManager {
    override val onlinePlayers: ImmutableSet<PlatformPlayer>
        get() = Bukkit.getOnlinePlayers().map { this.fromNative(it) }.toImmutableSet()

    override fun getFromUUID(uuid: UUID): PlatformPlayer? {
        return Bukkit.getPlayer(uuid)?.let { this.fromNative(it) }
    }

    override fun getFromName(name: String): PlatformPlayer? {
        return Bukkit.getPlayer(name)?.let { this.fromNative(it) }
    }

    @Throws(IllegalArgumentException::class)
    override fun fromNative(nativePlayer: Any): PlatformPlayer {
        require(nativePlayer is Player) { "Expected bukkit Player but got ${nativePlayer}" }
        return PaperPlayerWrapper(nativePlayer)
    }
}
