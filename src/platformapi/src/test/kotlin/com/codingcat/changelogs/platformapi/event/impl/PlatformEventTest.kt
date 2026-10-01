package com.codingcat.changelogs.platformapi.event.impl

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.util.*
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlatformEventTest {
    private class FakePlatformPlayer(
        override val uniqueId: UUID = UUID.randomUUID(),
        override val name: String = "TestPlayer",
    ) : PlatformPlayer {
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override val isNativeAdmin: TriState = TriState.TRUE

        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = "packetEventsUser"
    }

    @Test
    fun testPlayerJoinEvent() {
        val player = FakePlatformPlayer()
        val event = PlayerJoinEvent(player, justConnected = true)
        assertSame(player, event.player)
        assertTrue(event.justConnected)
    }

    @Test
    fun testPlayerEnterConfigurationPhaseEvent() {
        val player = FakePlatformPlayer()
        val event = PlayerEnterConfigurationPhaseEvent(player, initialConfiguration = true)
        assertSame(player, event.player)
        assertTrue(event.initialConfiguration)
    }
}
