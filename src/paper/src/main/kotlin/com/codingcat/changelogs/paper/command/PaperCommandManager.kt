package com.codingcat.changelogs.paper.command

import com.codingcat.changelogs.paper.player.PaperPlayerManager
import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.collections.immutable.ImmutableSet
import org.bukkit.plugin.Plugin

/**
 * Command manager implementation for Paper platform using Brigadier lifecycle events.
 */
@Suppress("UNCHECKED_CAST")
object PaperCommandManager : PlatformCommandManager {
    private val registrarActions: MutableSet<(Commands) -> Unit> = hashSetOf()

    override fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>) {
        val commandNode = node as LiteralCommandNode<CommandSourceStack>
        this.registrarActions.add { commands: Commands -> commands.register(commandNode, aliases) }
    }

    @Throws(IllegalArgumentException::class)
    override fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource {
        require(platformCommandSource is CommandSourceStack) {
            "Expected native paper CommandSourceStack but got ${platformCommandSource}"
        }
        return WrappedCommandSourceStack(platformCommandSource, PaperPlayerManager)
    }

    /**
     * Binds queued command registration actions to Paper's lifecycle event manager.
     */
    fun registerEvents(lifecycleEventManager: LifecycleEventManager<Plugin>) {
        lifecycleEventManager.registerEventHandler(
            LifecycleEvents.COMMANDS,
        ) { commands ->
            this.registrarActions.forEach { action ->
                action(commands.registrar())
            }
        }
    }
}
