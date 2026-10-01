package com.codingcat.changelogs.platformapi.command

import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * Abstraction responsible for registering Brigadier command nodes and adapting platform command sources.
 */
interface PlatformCommandManager {
    /**
     * Registers a root command node without aliases.
     */
    fun register(node: LiteralCommandNode<*>) {
        this.register(node, persistentSetOf())
    }

    /**
     * Registers a root command node alongside the provided set of alias labels.
     */
    fun register(node: LiteralCommandNode<*>, aliases: ImmutableSet<String>)

    /**
     * Adapts an underlying native command source (e.g., Paper `CommandSourceStack` or Velocity `CommandSource`)
     * to a platform-neutral [PlatformCommandSource].
     *
     * @throws IllegalArgumentException if the provided source instance is not of an expected native type.
     */
    fun adaptPlatformSource(platformCommandSource: Any): PlatformCommandSource
}
