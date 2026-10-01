package com.codingcat.changelogs.base.command.argument

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * Brigadier [ArgumentType] parsing online [PlatformPlayer] instances by their username.
 *
 * @property playerManager The platform player manager queried for active players.
 * @author GuavaDealer
 * @since Kotlin Migration
 */
class PlayerArgument(
    private val playerManager: PlatformPlayerManager,
) : ArgumentType<PlatformPlayer> {
    private val invalidNameException = DynamicCommandExceptionType { name ->
        LiteralMessage("Invalid player name: ${name}")
    }

    private val playerNotFoundException = DynamicCommandExceptionType { name ->
        LiteralMessage("Player not found: ${name}")
    }

    /**
     * Parses a player name from the reader and resolves the corresponding connected [PlatformPlayer].
     *
     * @param reader The string reader reading command input.
     * @return The resolved online player.
     * @throws com.mojang.brigadier.exceptions.CommandSyntaxException if the name is invalid or player not found.
     */
    @Throws(com.mojang.brigadier.exceptions.CommandSyntaxException::class)
    override fun parse(reader: StringReader): PlatformPlayer {
        val start = reader.cursor
        val argument = reader.readUnquotedString()

        if (!argument.matches(VALID_NAME_REGEX)) {
            reader.cursor = start
            throw invalidNameException.createWithContext(reader, argument)
        }

        return playerManager[argument] ?: run {
            reader.cursor = start
            throw playerNotFoundException.createWithContext(reader, argument)
        }
    }

    /**
     * Populates autocompletion suggestions matching the prefix of currently connected player names.
     *
     * @param context The command execution context.
     * @param builder The suggestions builder accumulating completion candidates.
     * @return A future completing with resolved suggestions.
     */
    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        playerManager.onlinePlayers.forEach { player ->
            if (player.name.lowercase().startsWith(remaining)) {
                builder.suggest(player.name)
            }
        }
        return builder.buildFuture()
    }

    companion object {
        private val VALID_NAME_REGEX = Regex("^[a-zA-Z0-9_]{3,16}$")

        /**
         * Creates a new [PlayerArgument] backed by the given [PlatformPlayerManager].
         */
        fun player(playerManager: PlatformPlayerManager): PlayerArgument = PlayerArgument(playerManager)

        /**
         * Creates a new [PlayerArgument] backed by the given [ServerChangelogs] plugin instance.
         */
        fun player(plugin: ServerChangelogs): PlayerArgument = PlayerArgument(plugin.platform.playerManager)

        /**
         * Resolves a parsed [PlatformPlayer] from a Brigadier [CommandContext].
         *
         * @param context The command context.
         * @param name The argument name.
         * @return The parsed [PlatformPlayer].
         */
        fun getPlayer(context: CommandContext<*>, name: String): PlatformPlayer {
            return context.getArgument(name, PlatformPlayer::class.java)
        }
    }
}
