package com.codingcat.changelogs.velocity.player

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.velocity.VelocityChangelogsPlatform
import com.github.retrooper.packetevents.protocol.player.User
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.util.*

class VelocityPlayerWrapper(
    private val player: Player,
) : PlatformPlayer {
    override val uniqueId: UUID
        get() = player.uniqueId

    override val name: String
        get() = player.username

    override val clientLocale: Locale
        get() = player.playerSettings?.locale ?: Locale.getDefault()

    override val isFirstJoin: Boolean = false

    override val isNativeAdmin: TriState = TriState.NOT_SET

    override fun kick(reason: Component?) {
        player.disconnect(reason ?: Component.empty())
    }

    override fun asPacketEventsUser(): User {
        return VelocityChangelogsPlatform.packetEventsApi.playerManager.getUser(player)
            ?: error("PacketEvents player was null")
    }

    override fun asAudience(): Audience {
        return player
    }

    override fun asPermissionChecker(): PermissionChecker {
        return player.permissionChecker
    }
}
