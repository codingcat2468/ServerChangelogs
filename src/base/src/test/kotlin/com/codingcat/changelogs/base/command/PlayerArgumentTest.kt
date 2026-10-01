package com.codingcat.changelogs.base.command.argument

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.codingcat.changelogs.platformapi.player.PlatformPlayerManager
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentSet
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlayerArgumentTest {
    private class TestPlayer(
        override val name: String,
        override val uniqueId: UUID = UUID.randomUUID(),
    ) : PlatformPlayer {
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override val isNativeAdmin: TriState = TriState.NOT_SET
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = Unit
    }

    private class TestPlayerManager(
        val players: List<PlatformPlayer>,
    ) : PlatformPlayerManager {
        override val onlinePlayers: ImmutableSet<PlatformPlayer> = players.toPersistentSet()
        override fun getFromName(name: String): PlatformPlayer? =
            players.find { it.name.equals(name, ignoreCase = true) }

        override fun getFromUUID(uuid: UUID): PlatformPlayer? = players.find { it.uniqueId == uuid }
        override fun fromNative(nativePlayer: Any): PlatformPlayer = throw UnsupportedOperationException()
    }

    @Test
    fun testParseValidPlayer() {
        val player = TestPlayer("Steve")
        val manager = TestPlayerManager(listOf(player))
        val argument = PlayerArgument(manager)

        val reader = StringReader("Steve")
        val parsed = argument.parse(reader)
        assertEquals("Steve", parsed.name)
    }

    @Test
    fun testParseInvalidNameThrows() {
        val manager = TestPlayerManager(emptyList())
        val argument = PlayerArgument(manager)

        val reader = StringReader("!!invalid!!")
        assertFailsWith<CommandSyntaxException> {
            argument.parse(reader)
        }
    }

    @Test
    fun testParsePlayerNotFoundThrows() {
        val manager = TestPlayerManager(emptyList())
        val argument = PlayerArgument(manager)

        val reader = StringReader("UnknownPlayer")
        assertFailsWith<CommandSyntaxException> {
            argument.parse(reader)
        }
    }

    @Test
    fun testListSuggestions() {
        val player1 = TestPlayer("Alice")
        val player2 = TestPlayer("Alex")
        val player3 = TestPlayer("Bob")
        val manager = TestPlayerManager(listOf(player1, player2, player3))
        val argument = PlayerArgument(manager)

        val builder = SuggestionsBuilder("al", 0)
        val dispatcher = com.mojang.brigadier.CommandDispatcher<Any>()
        val dummyContext = com.mojang.brigadier.context.CommandContextBuilder<Any>(
            dispatcher,
            Any(),
            dispatcher.root,
            0,
        ).build("al")
        val suggestions = argument.listSuggestions(dummyContext, builder).get()

        val matchedNames = suggestions.list.map { it.text }
        assertTrue(matchedNames.contains("Alice"))
        assertTrue(matchedNames.contains("Alex"))
        assertEquals(2, matchedNames.size)
    }
}
