package com.codingcat.changelogs.base.dialog

import com.codingcat.changelogs.base.TestPacketEvents
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.ConnectionState
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData
import com.github.retrooper.packetevents.protocol.dialog.DialogAction
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog
import com.github.retrooper.packetevents.protocol.dialog.NoticeDialog
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.resources.ResourceLocation
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerClearDialog
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerShowDialog
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.util.*
import kotlin.test.*

class DialogPacketsTest {
    @BeforeTest
    fun setUp() {
        TestPacketEvents.setup()
    }

    private class FakeUser(
        uuid: UUID = UUID.randomUUID(),
        name: String = "TestPlayer",
    ) : User(null, ConnectionState.PLAY, ClientVersion.V_1_21, UserProfile(uuid, name)) {
        val sentPackets = mutableListOf<PacketWrapper<*>>()
        var closedConnection = false

        override fun sendPacket(packet: PacketWrapper<*>) {
            sentPackets.add(packet)
        }

        override fun closeConnection() {
            closedConnection = true
        }
    }

    private class FakePlayer(
        val user: FakeUser,
        override val name: String = user.name ?: "TestPlayer",
        override val uniqueId: UUID = user.uuid,
    ) : PlatformPlayer {
        override val clientLocale: Locale = Locale.US
        override val isFirstJoin: Boolean = false
        override val isNativeAdmin: TriState = TriState.NOT_SET
        override fun asAudience(): Audience = Audience.empty()
        override fun asPermissionChecker(): PermissionChecker = PermissionChecker { TriState.TRUE }
        override fun kick(reason: Component?) {}
        override fun asPacketEventsUser(): Any = user
    }

    private fun createDummyDialog(): NoticeDialog {
        val common = CommonDialogData(
            Component.text("Title"),
            null,
            true,
            false,
            DialogAction.CLOSE,
            emptyList(),
            mutableListOf(),
        )
        return NoticeDialog(common, NoticeDialog.DEFAULT_ACTION)
    }

    @Test
    fun testShowDialogPlayAndConfig() {
        val user = FakeUser()
        val player = FakePlayer(user)
        val dialog = createDummyDialog()

        player.showDialog(dialog, DialogPackets.PacketPhase.PLAY)
        assertEquals(1, user.sentPackets.size)
        assertTrue(user.sentPackets[0] is WrapperPlayServerShowDialog)

        player.showDialog(dialog, DialogPackets.PacketPhase.CONFIGURATION)
        assertEquals(2, user.sentPackets.size)
        assertTrue(user.sentPackets[1] is WrapperConfigServerShowDialog)
    }

    @Test
    fun testClearDialogPlayAndConfig() {
        val user = FakeUser()
        val player = FakePlayer(user)

        player.clearDialog(DialogPackets.PacketPhase.PLAY)
        assertEquals(1, user.sentPackets.size)
        assertTrue(user.sentPackets[0] is WrapperPlayServerClearDialog)

        player.clearDialog(DialogPackets.PacketPhase.CONFIGURATION)
        assertEquals(2, user.sentPackets.size)
        assertTrue(user.sentPackets[1] is WrapperConfigServerClearDialog)
    }

    @Test
    fun testShowSimpleConfirm() {
        val user = FakeUser()
        val player = FakePlayer(user)

        val confirmAction = DynamicCustomAction(ResourceLocation(Key.key("test", "confirm")), null)
        val cancelAction = DynamicCustomAction(ResourceLocation(Key.key("test", "cancel")), null)

        DialogPackets.showSimpleConfirm(
            player,
            "dialog.popup.title",
            "dialog.popup.body",
            confirmAction,
            confirmDanger = false,
            cancelAction,
            DialogPackets.PacketPhase.PLAY,
        )

        assertEquals(1, user.sentPackets.size)
        val packet = user.sentPackets[0] as WrapperPlayServerShowDialog
        assertTrue(packet.dialog is MultiActionDialog)

        // Test with confirmDanger = true
        DialogPackets.showSimpleConfirm(
            player,
            "dialog.popup.title",
            "dialog.popup.body",
            confirmAction,
            confirmDanger = true,
            cancelAction,
            DialogPackets.PacketPhase.PLAY,
        )
        assertEquals(2, user.sentPackets.size)
    }

    @Test
    fun testShowSimpleNotice() {
        val user = FakeUser()
        val player = FakePlayer(user)

        // Notice with action
        val closeAction = DynamicCustomAction(ResourceLocation(Key.key("test", "close")), null)
        DialogPackets.showSimpleNotice(
            player,
            "dialog.popup.title",
            "dialog.popup.body",
            closeAction,
            DialogPackets.PacketPhase.PLAY,
        )
        assertEquals(1, user.sentPackets.size)
        val packet = user.sentPackets[0] as WrapperPlayServerShowDialog
        assertTrue(packet.dialog is NoticeDialog)

        // Notice without action (defaults)
        DialogPackets.showSimpleNotice(
            player,
            "dialog.popup.title",
            "dialog.popup.body",
        )
        assertEquals(2, user.sentPackets.size)
    }

    @Test
    fun testWrapPacketEventsUser() {
        val user = FakeUser(name = "WrappedUser")
        val wrapped = DialogPackets.wrapPacketEventsUser(user)

        assertEquals(user.uuid, wrapped.uniqueId)
        assertEquals("WrappedUser", wrapped.name)
        assertEquals(Locale.US, wrapped.clientLocale)
        assertFalse(wrapped.isFirstJoin)
        assertEquals(TriState.NOT_SET, wrapped.isNativeAdmin)
        assertEquals(user, wrapped.asPacketEventsUser())
        assertNotNull(wrapped.asAudience())
        assertEquals(TriState.NOT_SET, wrapped.asPermissionChecker().value("some.perm"))

        assertFalse(user.closedConnection)
        wrapped.kick(Component.text("Reason"))
        assertTrue(user.closedConnection)
    }
}
