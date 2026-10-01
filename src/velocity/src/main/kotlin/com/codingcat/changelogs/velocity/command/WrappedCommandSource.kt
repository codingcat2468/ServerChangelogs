package com.codingcat.changelogs.velocity.command

import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.velocity.player.VelocityPlayerManager
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker

class WrappedCommandSource(
    private val commandSource: CommandSource,
    private val playerManager: VelocityPlayerManager,
) : PlatformCommandSource {
    override val executingPlayer: PlatformPlayer?
        get() = (commandSource as? Player)?.let { playerManager.fromNative(it) }

    override fun asAudience(): Audience {
        return commandSource
    }

    override fun asPermissionChecker(): PermissionChecker {
        return commandSource.permissionChecker
    }
}
