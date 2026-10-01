package com.codingcat.changelogs.base.dialog

import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog
import com.codingcat.changelogs.base.lang.TranslationSource
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.dialog.*
import com.github.retrooper.packetevents.protocol.dialog.action.Action
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerClearDialog
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerShowDialog
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.permission.PermissionChecker
import net.kyori.adventure.text.Component
import net.kyori.adventure.util.TriState
import java.util.*

private const val CONFIRM_BUTTON_WIDTH: Int = 100
private const val CANCEL_BUTTON_WIDTH: Int = 100
private const val CLOSE_BUTTON_WIDTH: Int = 40
private const val CONFIRM_DIALOG_COLUMNS: Int = 2

object DialogPackets {
    fun showDialog(player: PlatformPlayer, dialog: Dialog, packetPhase: PacketPhase) {
        val wrapper = if (packetPhase == PacketPhase.CONFIGURATION) {
            WrapperConfigServerShowDialog(dialog)
        } else {
            WrapperPlayServerShowDialog(dialog)
        }
        packet(player, wrapper)
    }

    fun showSimpleConfirm(
        player: PlatformPlayer,
        titleTranslation: String,
        bodyTranslation: String,
        confirm: Action,
        confirmDanger: Boolean,
        cancel: Action,
        packetPhase: PacketPhase,
    ) {
        val common: CommonDialogData = createSimpleDialog(
            player, titleTranslation, bodyTranslation,
            closeAfter = false,
            canEscape = false,
        )
        val confirmSuffix = if (confirmDanger) "_danger" else ""
        val confirmButton = ActionButton(
            CommonButtonData(
                TranslationSource.translatableManual(
                    player,
                    "dialog.popup.confirm${confirmSuffix}",
                ),
                null,
                CONFIRM_BUTTON_WIDTH,
            ),
            confirm,
        )
        val cancelButton = ActionButton(
            CommonButtonData(
                TranslationSource.translatableManual(player, "dialog.popup.cancel"),
                null,
                CANCEL_BUTTON_WIDTH,
            ),
            cancel,
        )
        showDialog(
            player,
            MultiActionDialog(common, listOf(confirmButton, cancelButton), null, CONFIRM_DIALOG_COLUMNS),
            packetPhase,
        )
    }

    @JvmOverloads
    fun showSimpleNotice(
        player: PlatformPlayer,
        titleTranslation: String,
        bodyTranslation: String,
        action: Action? = null,
        packetPhase: PacketPhase = PacketPhase.PLAY,
    ) {
        val common: CommonDialogData =
            createSimpleDialog(player, titleTranslation, bodyTranslation, action == null, true)
        val closeButton = ActionButton(
            CommonButtonData(
                TranslationSource.translatableManual(player, "dialog.popup.close"),
                null,
                CLOSE_BUTTON_WIDTH,
            ),
            action,
        )
        showDialog(player, NoticeDialog(common, closeButton), packetPhase)
    }

    fun createSimpleDialog(
        player: PlatformPlayer,
        titleTranslation: String,
        bodyTranslation: String,
        closeAfter: Boolean,
        canEscape: Boolean,
    ): CommonDialogData {
        val body: DialogBody = PlainMessageDialogBody(
            PlainMessage(
                TranslationSource.translatableManual(player, bodyTranslation),
                ChangelogDialog.LINE_WIDTH,
            ),
        )
        return CommonDialogData(
            TranslationSource.translatableManual(player, titleTranslation),
            null,
            canEscape,
            false,
            if (closeAfter) DialogAction.CLOSE else DialogAction.NONE,
            listOf(body),
            mutableListOf(),
        )
    }

    fun clearDialog(player: PlatformPlayer, packetPhase: PacketPhase) {
        val wrapper = if (packetPhase == PacketPhase.CONFIGURATION) {
            WrapperConfigServerClearDialog()
        } else {
            WrapperPlayServerClearDialog()
        }
        packet(player, wrapper)
    }

    fun wrapPacketEventsUser(user: User): PlatformPlayer {
        return PacketEventsUserWrappedPlayer(user)
    }

    private fun packet(player: PlatformPlayer, wrapper: PacketWrapper<*>) {
        val user = player.asPacketEventsUser() as User
        user.sendPacket(wrapper)
    }

    enum class PacketPhase {
        CONFIGURATION,
        PLAY,
    }

    private class PacketEventsUserWrappedPlayer(
        private val user: User,
    ) : PlatformPlayer {
        override val uniqueId: UUID
            get() = this.user.uuid

        override val name: String
            get() = this.user.name ?: "Unknown"

        override val clientLocale: Locale
            get() = Locale.US

        override val isFirstJoin: Boolean
            get() = false

        override val isNativeAdmin: TriState
            get() = TriState.NOT_SET

        override fun kick(reason: Component?) {
            this.user.closeConnection()
        }

        override fun asPacketEventsUser(): Any {
            return this.user
        }

        override fun asAudience(): Audience {
            return Audience.empty()
        }

        override fun asPermissionChecker(): PermissionChecker {
            return PermissionChecker.always(TriState.NOT_SET)
        }

        companion object {
            private val EXCEPTION: RuntimeException =
                UnsupportedOperationException("Not supported on PacketEvents user wrapper")
        }
    }
}

fun PlatformPlayer.showDialog(
    dialog: Dialog,
    packetPhase: DialogPackets.PacketPhase = DialogPackets.PacketPhase.PLAY,
) {
    DialogPackets.showDialog(this, dialog, packetPhase)
}

fun PlatformPlayer.clearDialog(packetPhase: DialogPackets.PacketPhase = DialogPackets.PacketPhase.PLAY) {
    DialogPackets.clearDialog(this, packetPhase)
}
