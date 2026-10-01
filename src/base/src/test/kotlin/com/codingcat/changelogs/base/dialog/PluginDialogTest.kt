package com.codingcat.changelogs.base.dialog

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.dialog.Dialog
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PluginDialogTest {
    private class SimpleDialog(override val id: String) : PluginDialog {
        var destroyed = false
        var shouldReject = false

        override fun build(player: PlatformPlayer, sessionManager: DialogSessionManager): Dialog {
            throw UnsupportedOperationException()
        }

        override fun onActionTriggered(
            action: String,
            data: NBTCompound?,
            source: PlatformPlayer,
            sessionManager: DialogSessionManager,
        ) {
        }

        override fun attemptDestroy() {
            if (shouldReject) throw PluginDialog.DestroyRejectedException("busy")
            destroyed = true
        }
    }

    @Test
    fun testDestroyRejectedException() {
        val ex = PluginDialog.DestroyRejectedException("active_editor")
        assertEquals("active_editor", ex.key)
    }

    @Test
    fun testAttemptDestroyOnSimpleDialog() {
        val dialog = SimpleDialog("test")
        dialog.attemptDestroy()
        assertEquals(true, dialog.destroyed)

        dialog.shouldReject = true
        assertFailsWith<PluginDialog.DestroyRejectedException> {
            dialog.attemptDestroy()
        }
    }
}
