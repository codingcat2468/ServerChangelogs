package com.codingcat.changelogs.paper.command

import com.codingcat.changelogs.paper.player.PaperPlayerManager
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import org.bukkit.entity.Player

class WrappedCommandSourceStack(
    private val sourceStack: CommandSourceStack,
    private val playerManager: PaperPlayerManager,
) : PlatformCommandSource {
    override val executingPlayer: PlatformPlayer?
        get() = when (val p = sourceStack.sender) {
            is Player -> playerManager.fromNative(p)
            else -> null
        }

    override fun asAudience(): Audience {
        return sourceStack.sender
    }

    override fun asPermissionChecker(): PermissionChecker {
        return PermissionChecker { permission: String -> sourceStack.sender.permissionValue(permission) }
    }
}
