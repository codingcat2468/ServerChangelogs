package com.codingcat.changelogs.base.command

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.collections.immutable.toPersistentSet

/**
 * Contract for building and registering Brigadier commands across platforms.
 *
 * @author GuavaDealer
 * @since Kotlin Migration
 */
interface CommandBuilder : BrigadierCommandNode {
    /**
     * Builds the Brigadier command node for registration.
     *
     * @param plugin The active [ServerChangelogs] instance.
     * @return The built [LiteralCommandNode] with platform-agnostic source type.
     */
    override fun build(plugin: ServerChangelogs): LiteralCommandNode<Any>

    /**
     * Command aliases for registration.
     */
    val aliases: List<String>
        get() = emptyList()

    /**
     * Brief description of the command.
     */
    val description: String
        get() = ""
}

/**
 * Convenience extension to build and register a [CommandBuilder] alongside its declared aliases.
 */
fun PlatformCommandManager.register(builder: CommandBuilder, plugin: ServerChangelogs) {
    this.register(builder.build(plugin), builder.aliases.toPersistentSet())
}
