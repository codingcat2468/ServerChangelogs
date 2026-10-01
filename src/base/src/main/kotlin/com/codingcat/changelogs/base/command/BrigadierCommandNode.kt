package com.codingcat.changelogs.base.command

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.platformapi.command.PlatformCommandManager
import com.mojang.brigadier.tree.LiteralCommandNode

/**
 * Functional interface for building a Brigadier [LiteralCommandNode] within [ServerChangelogs].
 */
fun interface BrigadierCommandNode {
    /**
     * Constructs the literal command node for this command structure.
     *
     * @param plugin The active [ServerChangelogs] instance.
     * @return The constructed [LiteralCommandNode].
     */
    fun build(plugin: ServerChangelogs): LiteralCommandNode<Any>

    companion object {
        /**
         * Creates a permission predicate lambda for Brigadier commands.
         *
         * @param permission The sub-permission node to check.
         * @param plugin The active [ServerChangelogs] instance.
         * @param trueIfUnset Whether permission check succeeds if unset.
         * @return A predicate lambda returning `true` if the source has permission.
         */
        fun requirePermission(
            permission: String,
            plugin: ServerChangelogs,
            trueIfUnset: Boolean,
        ): (Any) -> Boolean {
            val commandManager: PlatformCommandManager = plugin.platform.commandManager
            if (plugin.pluginConfig().useNativeFallbackPermissions()) {
                return { source ->
                    val player = commandManager.adaptPlatformSource(source).executingPlayer
                    player?.let { it.isNativeAdmin.toBooleanOrElse(false) || trueIfUnset } ?: true
                }
            }
            val perm = "${ServerChangelogs.NAMESPACE}.${permission}"
            return { source ->
                commandManager.adaptPlatformSource(source).hasPermission(perm, trueIfUnset)
            }
        }
    }
}
