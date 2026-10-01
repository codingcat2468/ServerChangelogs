package com.codingcat.changelogs.base.dialog

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.dialog.Dialog
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DialogSessionManagerTest {
    private class DummyDialog(override val id: String) : PluginDialog {
        override fun build(player: PlatformPlayer, sessionManager: DialogSessionManager): Dialog =
            throw UnsupportedOperationException()

        override fun onActionTriggered(
            action: String,
            data: NBTCompound?,
            source: PlatformPlayer,
            sessionManager: DialogSessionManager,
        ) {
        }
    }

    private class TestPlayer(override val uniqueId: UUID = UUID.randomUUID()) : PlatformPlayer {
        override val name: String = "TestPlayer"
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override val isNativeAdmin: TriState = TriState.NOT_SET
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = Unit
    }

    @Test
    fun testSessionLifecycle() {
        val player = TestPlayer()
        val dialog = DummyDialog("test_dialog")

        assertFalse(DialogSessionManager.isSessionActive(player, dialog))

        DialogSessionManager.startSession(dialog, player, "SessionData123")
        assertTrue(DialogSessionManager.isSessionActive(player, dialog))
        assertEquals("SessionData123", DialogSessionManager.getSessionData(player, String::class.java))

        DialogSessionManager.setSessionData(player, "UpdatedData")
        assertEquals("UpdatedData", DialogSessionManager.getSessionData(player, String::class.java))

        // startSessionIfNoneActive should be a no-op since session is already active
        DialogSessionManager.startSessionIfNoneActive(dialog, player) { "Ignored" }
        assertEquals("UpdatedData", DialogSessionManager.getSessionData(player, String::class.java))

        DialogSessionManager.endSession(player)
        assertFalse(DialogSessionManager.isSessionActive(player, dialog))
    }

    @Test
    fun testFreezeAndUnfreeze() {
        val player = TestPlayer()

        assertFalse(DialogSessionManager.isFrozen(player))

        DialogSessionManager.freeze(player)
        assertTrue(DialogSessionManager.isFrozen(player))

        DialogSessionManager.unfreeze(player)
        assertFalse(DialogSessionManager.isFrozen(player))
    }

    @Test
    fun testAwaitUnfreezeAsync() {
        runBlocking {
            val player = TestPlayer()
            DialogSessionManager.freeze(player)

            val job = async {
                DialogSessionManager.awaitUnfreeze(player, timeoutSeconds = 2)
            }

            DialogSessionManager.unfreeze(player)
            job.await()
            assertFalse(DialogSessionManager.isFrozen(player))
        }
    }

    @Test
    fun testWaitForUnfreezeBlocking() {
        val player = TestPlayer()
        DialogSessionManager.freeze(player)
        DialogSessionManager.unfreeze(player)
        DialogSessionManager.waitForUnfreeze(player, timeoutSeconds = 1)
        assertFalse(DialogSessionManager.isFrozen(player))
    }

    @Test
    fun testActionAndClickEventCreation() {
        val dialog = DummyDialog("sample")

        val staticClick = DialogSessionManager.createStaticClickEvent(dialog, "click")
        assertTrue(staticClick.payload().toString().contains("dialog/sample/click"))

        val sessionClick = DialogSessionManager.createSessionBasedClickEvent(dialog, "btn")
        assertTrue(sessionClick.payload().toString().contains("dialog/sample/btn"))
    }
}
