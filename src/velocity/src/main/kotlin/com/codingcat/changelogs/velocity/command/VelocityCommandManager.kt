package com.codingcat.changelogs.velocity.command

import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.codingcat.changelogs.velocity.VelocityChangelogsPlatform
import com.codingcat.changelogs.velocity.player.VelocityPlayerManager
import com.mojang.brigadier.tree.LiteralCommandNode
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.command.CommandMeta
import com.velocitypowered.api.command.CommandSource
import kotlinx.collections.immutable.ImmutableSet

/**
 * Command manager implementation adapting Brigadier commands to Velocity's [CommandManager].
 */
@Suppress("UNCHECKED_CAST")
object VelocityCommandManager : PlatformCommandManager {
    lateinit var commandManager: CommandManager
    lateinit var platform: VelocityChangelogsPlatform
    override fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>) {
        val command = BrigadierCommand(node as LiteralCommandNode<CommandSource>)
        val commandMeta: CommandMeta = this.commandManager.metaBuilder(command)
            .aliases(*aliases.toTypedArray())
            .plugin(this.platform)
            .build()
        this.commandManager.register(commandMeta, command)
    }

    @Throws(IllegalArgumentException::class)
    override fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource {
        require(platformCommandSource is CommandSource) {
            "Expected native velocity CommandSource but got ${platformCommandSource}"
        }
        return WrappedCommandSource(platformCommandSource, VelocityPlayerManager)
    }
}
