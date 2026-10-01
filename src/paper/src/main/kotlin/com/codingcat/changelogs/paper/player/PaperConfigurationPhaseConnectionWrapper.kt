@file:Suppress("UnstableApiUsage")

package com.codingcat.changelogs.paper.player

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.destroystokyo.paper.ClientOption
import com.github.retrooper.packetevents.protocol.player.User
import io.papermc.paper.connection.PlayerConfigurationConnection
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import org.bukkit.Bukkit
import java.util.*

class PaperConfigurationPhaseConnectionWrapper(
    private val connection: PlayerConfigurationConnection,
) : PlatformPlayer {
    override val uniqueId: UUID
        get() = connection.profile.id ?: error("Player UUID was null")

    override val name: String
        get() = connection.profile.name ?: "Unknown"

    override val clientLocale: Locale
        get() = Locale.of(connection.getClientOption(ClientOption.LOCALE))

    override val isFirstJoin: Boolean = false

    override val isNativeAdmin: TriState = TriState.NOT_SET

    override fun kick(reason: Component?) {
        connection.disconnect(reason ?: Component.empty())
    }

    override fun asPacketEventsUser(): User {
        val channel = ServerChangelogs.packetEventsApi.protocolManager.getChannel(uniqueId)
        return ServerChangelogs.packetEventsApi.protocolManager.getUser(channel)
            ?: error("Player channel was null")
    }

    override fun asAudience(): Audience {
        return connection.audience
    }

    override fun asPermissionChecker(): PermissionChecker {
        return Bukkit.getPlayer(uniqueId)?.let { player ->
            PermissionChecker { permission: String -> player.permissionValue(permission) }
        } ?: PermissionChecker.always(TriState.NOT_SET)
    }

    fun unwrapNative(): Any {
        return connection
    }
}
