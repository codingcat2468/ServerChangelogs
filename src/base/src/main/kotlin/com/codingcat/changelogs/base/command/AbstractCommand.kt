package com.codingcat.changelogs.base.command

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.command.argument.PlayerArgument
import com.codingcat.changelogs.base.lang.TranslationSource
import com.codingcat.changelogs.platformapi.command.PlatformCommandSource
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Base class for commands across platforms, providing standardized registration and helper utilities.
 *
 * Implements [CommandBuilder] and handles:
 * - Permission checking via Brigadier requirements.
 * - Feature flag validation to disable commands if inactive.
 * - Player-only execution validation via [executesPlayer].
 * - Fluent argument and literal builder extensions.
 *
 * @property name The literal name of the command.
 * @property description Brief description of the command.
 * @property usage Usage string for the command, defaulting to `"/${name}"`.
 * @property aliases Alternate labels for command invocation.
 * @property permission The required permission key (without namespace prefix if relative), or null if unrestricted.
 * @property permissionEnabledByDefault Whether the permission is granted by default when unset.
 * @property featureEnabled Whether the command's feature is globally enabled.
 *
 * @author GuavaDealer
 * @since Kotlin Migration
 */
abstract class AbstractCommand(
    val name: String,
    override val description: String,
    val usage: String = "/${name}",
    override val aliases: List<String> = emptyList(),
    val permission: String? = null,
    val permissionEnabledByDefault: Boolean = false,
    val featureEnabled: Boolean = true,
) : CommandBuilder {
    /**
     * Active plugin instance assigned during [build].
     */
    protected lateinit var plugin: ServerChangelogs
        private set

    override fun build(plugin: ServerChangelogs): LiteralCommandNode<Any> {
        this.plugin = plugin
        if (!isFeatureEnabled(plugin)) {
            return LiteralArgumentBuilder.literal<Any>(name)
                .executes { ctx ->
                    val source = plugin.platform.commandManager.adaptPlatformSource(ctx.source)
                    source.asAudience().sendMessage(
                        Component.text("This feature is disabled.", NamedTextColor.RED),
                    )
                    Command.SINGLE_SUCCESS
                }
                .build()
        }

        val builder = LiteralArgumentBuilder.literal<Any>(name)

        permission?.let { permStr ->
            val perm = permStr.removePrefix("${ServerChangelogs.NAMESPACE}.")
            val predicate = BrigadierCommandNode.requirePermission(
                perm,
                plugin,
                permissionEnabledByDefault,
            )
            builder.requires { predicate(it) }
        }

        buildCommand(builder)

        return builder.build()
    }

    /**
     * Determines whether this command is currently enabled for registration.
     *
     * @param plugin The active [ServerChangelogs] instance.
     * @return `true` if enabled, `false` otherwise.
     */
    open fun isFeatureEnabled(plugin: ServerChangelogs): Boolean = featureEnabled

    /**
     * Builds the command structure and execution handlers.
     *
     * @param builder The literal argument builder being configured.
     */
    abstract fun buildCommand(builder: LiteralArgumentBuilder<Any>)

    /**
     * Executes the given block only if the invoking source represents a player.
     *
     * @param block The block invoked with the executing player and command context.
     * @return The updated argument builder.
     */
    protected fun <T : ArgumentBuilder<Any, T>> T.executesPlayer(
        block: (PlatformPlayer, CommandContext<Any>) -> Unit,
    ): T {
        return this.executes { ctx ->
            val source = plugin.platform.commandManager.adaptPlatformSource(ctx.source)
            val player = source.executingPlayer ?: run {
                source.asAudience().sendMessage(TranslationSource.translatable("command.onlyplayer"))
                return@executes Command.SINGLE_SUCCESS
            }
            block(player, ctx)
            Command.SINGLE_SUCCESS
        }
    }

    /**
     * Executes the given block with the adapted [PlatformCommandSource] and command context.
     *
     * @param block The block invoked with the executing command source and command context.
     * @return The updated argument builder.
     */
    protected fun <T : ArgumentBuilder<Any, T>> T.executesSource(
        block: (PlatformCommandSource, CommandContext<Any>) -> Unit,
    ): T {
        return this.executes { ctx ->
            val source = plugin.platform.commandManager.adaptPlatformSource(ctx.source)
            block(source, ctx)
            Command.SINGLE_SUCCESS
        }
    }

    /**
     * Executes the given suspending block asynchronously only if the invoking source represents a player.
     *
     * @param block The suspending block invoked with the executing player and command context.
     * @return The updated argument builder.
     */
    protected fun <T : ArgumentBuilder<Any, T>> T.executesPlayerSuspend(
        block: suspend (PlatformPlayer, CommandContext<Any>) -> Unit,
    ): T {
        return this.executes { ctx ->
            val source = plugin.platform.commandManager.adaptPlatformSource(ctx.source)
            val player = source.executingPlayer ?: run {
                source.asAudience().sendMessage(TranslationSource.translatable("command.onlyplayer"))
                return@executes Command.SINGLE_SUCCESS
            }
            plugin.platform.coroutineScope.launch {
                block(player, ctx)
            }
            Command.SINGLE_SUCCESS
        }
    }

    /**
     * Executes the given suspending block asynchronously with the adapted [PlatformCommandSource] and command context.
     *
     * @param block The suspending block invoked with the executing command source and command context.
     * @return The updated argument builder.
     */
    protected fun <T : ArgumentBuilder<Any, T>> T.executesSourceSuspend(
        block: suspend (PlatformCommandSource, CommandContext<Any>) -> Unit,
    ): T {
        return this.executes { ctx ->
            val source = plugin.platform.commandManager.adaptPlatformSource(ctx.source)
            plugin.platform.coroutineScope.launch {
                block(source, ctx)
            }
            Command.SINGLE_SUCCESS
        }
    }

    /**
     * Attaches a permission requirement to this argument builder.
     *
     * @param permission The permission node to require.
     * @param trueIfUnset Whether permission is granted if unset.
     * @return The updated argument builder.
     */
    protected fun <T : ArgumentBuilder<Any, T>> T.requiresPermission(
        permission: String,
        trueIfUnset: Boolean = false,
    ): T {
        val perm = permission.removePrefix("${ServerChangelogs.NAMESPACE}.")
        val predicate = BrigadierCommandNode.requirePermission(perm, plugin, trueIfUnset)
        return this.requires { predicate(it) }
    }

    /**
     * Appends a child subcommand node built from a [BrigadierCommandNode].
     *
     * @param command The subcommand builder to register.
     * @return The updated argument builder.
     */
    protected fun <S : ArgumentBuilder<Any, S>> S.subcommand(
        command: BrigadierCommandNode,
    ): S {
        this.then(command.build(plugin))
        return this
    }

    /**
     * Appends a typed argument to this builder.
     *
     * @param name The argument name.
     * @param argument The argument type.
     * @param block Configuration block for the argument builder.
     * @return The updated argument builder.
     */
    protected fun <S : ArgumentBuilder<Any, S>, T> S.argument(
        name: String,
        argument: ArgumentType<T>,
        block: (@CommandDsl RequiredArgumentBuilder<Any, T>).() -> Unit = {},
    ): S {
        val argBuilder = RequiredArgumentBuilder.argument<Any, T>(name, argument)
        argBuilder.block()
        this.then(argBuilder)
        return this
    }

    /**
     * Appends a [PlatformPlayer] argument to this builder.
     *
     * @param name The argument name (defaults to `"player"`).
     * @param block Configuration block for the player argument builder.
     * @return The updated argument builder.
     */
    protected fun <S : ArgumentBuilder<Any, S>> S.player(
        name: String = "player",
        block: (@CommandDsl RequiredArgumentBuilder<Any, PlatformPlayer>).() -> Unit = {},
    ): S {
        return argument(name, PlayerArgument.player(plugin), block)
    }

    /**
     * Appends a literal subcommand branch to this builder.
     *
     * @param name The literal subcommand name.
     * @param block Configuration block for the literal argument builder.
     * @return The updated argument builder.
     */
    protected fun <S : ArgumentBuilder<Any, S>> S.literal(
        name: String,
        block: (@CommandDsl LiteralArgumentBuilder<Any>).() -> Unit = {},
    ): S {
        val literalBuilder = LiteralArgumentBuilder.literal<Any>(name)
        literalBuilder.block()
        this.then(literalBuilder)
        return this
    }

    /**
     * Resolves the [PlatformCommandSource] from a [CommandContext].
     */
    protected val CommandContext<Any>.platformSource: PlatformCommandSource
        get() = plugin.platform.commandManager.adaptPlatformSource(this.source)

    /**
     * Resolves the executing [PlatformPlayer] from a [CommandContext], if the sender is a player.
     */
    protected val CommandContext<Any>.executingPlayer: PlatformPlayer?
        get() = platformSource.executingPlayer

    /**
     * Resolves a parsed [PlatformPlayer] argument from a [CommandContext].
     */
    protected fun CommandContext<Any>.getPlayer(name: String = "player"): PlatformPlayer {
        return PlayerArgument.getPlayer(this, name)
    }
}
