package com.codingcat.changelogs.paper.player

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.player.User
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerKickEvent
import java.util.*

class PaperPlayerWrapper(
    private val player: Player,
) : PlatformPlayer {
    override val uniqueId: UUID = player.uniqueId
    override val name: String get() = player.name
    override val clientLocale: Locale = player.locale()
    override val isFirstJoin: Boolean = !player.hasPlayedBefore()
    override val isNativeAdmin: TriState = TriState.byBoolean(player.isOp)

    override fun kick(reason: Component?) {
        player.kick(reason, PlayerKickEvent.Cause.PLUGIN)
    }

    override fun asPacketEventsUser(): User {
        return ServerChangelogs.packetEventsApi.playerManager.getUser(this.player)
            ?: error("PacketEvents player was null")
    }

    override fun asAudience(): Audience {
        return this.player
    }

    override fun asPermissionChecker(): PermissionChecker {
        return PermissionChecker { permission: String -> this.player.permissionValue(permission) }
    }
}
