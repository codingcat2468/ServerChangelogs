package com.codingcat.changelogs.base.data

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.time.Instant
import java.util.*
import kotlin.test.*

class ChangelogEntryTest {
    private class TestPlayer(
        override val uniqueId: UUID,
        override val name: String = "TestPlayer",
    ) : PlatformPlayer {
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override val isNativeAdmin: TriState = TriState.NOT_SET
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = Unit
    }

    @Test
    fun testChangelogEntryPropertiesAndHasRead() {
        val playerUuid1 = UUID.randomUUID()
        val playerUuid2 = UUID.randomUUID()
        val player1 = TestPlayer(playerUuid1)
        val player2 = TestPlayer(playerUuid2)

        val now = Instant.now()
        val entry = ChangelogEntry(
            uid = 1,
            lines = listOf(Component.text("Line 1"), Component.text("Line 2")),
            recordedAt = now,
            author = Component.text("Author"),
            playersRead = mutableSetOf(playerUuid1),
        )

        assertEquals(1, entry.uid)
        assertEquals(2, entry.lines.size)
        assertEquals(now, entry.recordedAt)
        assertNotNull(entry.author)
        assertTrue(entry.hasRead(player1))
        assertFalse(entry.hasRead(player2))

        val copy = entry.copy(uid = 2)
        assertEquals(2, copy.uid)
        assertEquals(entry.recordedAt, copy.recordedAt)
        assertEquals(entry.hashCode(), entry.hashCode())
        assertTrue(entry.toString().contains("1"))
    }
}
