package com.codingcat.changelogs.base.dialog.ui.editor

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.compat.PacketEventsFix
import com.codingcat.changelogs.base.data.ChangelogStorage
import com.codingcat.changelogs.base.dialog.DialogPackets
import com.codingcat.changelogs.base.dialog.DialogSessionManager
import com.codingcat.changelogs.base.dialog.PluginDialog
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog
import com.codingcat.changelogs.base.lang.TranslationSource
import com.codingcat.changelogs.base.lang.TranslationSource.Companion.translatableManual
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData
import com.github.retrooper.packetevents.protocol.dialog.Dialog
import com.github.retrooper.packetevents.protocol.dialog.DialogAction
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData
import com.github.retrooper.packetevents.protocol.dialog.input.Input
import com.github.retrooper.packetevents.protocol.dialog.input.TextInputControl
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.nbt.NBTByte
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTInt
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextDecoration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private const val ITEM_BODY_WIDTH: Int = 15
private const val ITEM_BODY_HEIGHT: Int = 15
private const val SUBTITLE_WIDTH: Int = 160
private const val RESTORED_SESSION_WIDTH: Int = 400
private const val LINE_INPUT_WIDTH: Int = 350
private const val LINE_INPUT_MAX_LENGTH: Int = 5000
private const val AUTHOR_INPUT_WIDTH: Int = 200
private const val AUTHOR_INPUT_MAX_LENGTH: Int = 200
private const val COMMIT_BUTTON_WIDTH: Int = 160
private const val LINE_ACTION_BUTTON_WIDTH: Int = 100
private const val CLOSE_BUTTON_WIDTH: Int = 100
private const val SAVE_SESSION_BUTTON_WIDTH: Int = 140
private const val DISCARD_SESSION_BUTTON_WIDTH: Int = 140
private const val CANCEL_BUTTON_WIDTH: Int = 100
private const val EDITOR_DIALOG_COLUMNS: Int = 3
private const val CONFIRM_CLOSE_COLUMNS: Int = 3

/**
 * Interactive dialog-based changelog editor enabling players with permissions to create and edit entries.
 */
class ChangelogEditorDialog(
    private val storage: ChangelogStorage,
    private val useFallbackPermissions: Boolean,
) : PluginDialog {
    private val savedSessions: MutableMap<UUID, EditorSession> = ConcurrentHashMap()

    override val id: String = "changelog_editor"

    override fun build(
        player: PlatformPlayer,
        sessionManager: DialogSessionManager,
    ): Dialog {
        sessionManager.startSessionIfNoneActive(this, player) {
            this@ChangelogEditorDialog.savedSessions.remove(player.uniqueId)?.takeIf { it.canBeSaved() }
                ?: EditorSession.Create(this@ChangelogEditorDialog.storage.nextUID())
        }
        val session: EditorSession = sessionManager.getSessionData(player, EditorSession::class.java)
        val sessionTranslation = "dialog.editor.${session.id}"
        val previewLines = session.deserializeLines()

        val lineMapper: (Component, Int) -> Component = { rawLine, idx ->
            var line = rawLine
            val isEditing = session.editingLineIndex == idx
            if (isEditing) line = line.decoration(TextDecoration.BOLD, true)
            val prefix = "dialog.editor.line_action."
            val payload = NBTCompound()
            payload.setTag("target_line", NBTInt(idx))
            var edit: Component? = null
            if (!isEditing) {
                edit = translatableManual(
                    player,
                    "${prefix}format",
                    TranslationSource.translatable("${prefix}edit"),
                )
                    .decoration(TextDecoration.BOLD, false)
                    .clickEvent(
                        sessionManager.createSessionBasedClickEvent(
                            this@ChangelogEditorDialog,
                            "start_edit_line",
                            payload,
                        ),
                    )
            }
            val remove: Component = translatableManual(
                player,
                "${prefix}format",
                TranslationSource.translatable("${prefix}remove"),
            )
                .decoration(TextDecoration.BOLD, false)
                .clickEvent(
                    sessionManager.createSessionBasedClickEvent(
                        this@ChangelogEditorDialog,
                        "remove_line",
                        payload,
                    ),
                )
            var finalCmp = line.appendSpace()
            if (!isEditing) edit?.let { finalCmp = finalCmp.append(it).appendSpace() }
            finalCmp.append(remove)
        }

        val itemBody = ItemDialogBody(
            ItemStack.builder().type(ItemTypes.WRITABLE_BOOK).build(),
            PlainMessage(
                translatableManual(player, "${sessionTranslation}.subtitle", text(session.entryUID + 1)),
                SUBTITLE_WIDTH,
            ),
            false,
            false,
            ITEM_BODY_WIDTH,
            ITEM_BODY_HEIGHT,
        )
        PacketEventsFix.fixItemBody(itemBody)

        val bodyList = mutableListOf(
            itemBody,
            PlainMessageDialogBody(
                PlainMessage(
                    translatableManual(player, "dialog.editor.hint"),
                    ChangelogDialog.LINE_WIDTH,
                ),
            ),
        )

        if (session.showRestoredMessage) {
            bodyList.add(
                PlainMessageDialogBody(
                    PlainMessage(
                        translatableManual(player, "dialog.editor.restored_session"),
                        RESTORED_SESSION_WIDTH,
                    ),
                ),
            )
        }

        bodyList.add(
            PlainMessageDialogBody(
                PlainMessage(
                    if (previewLines.isNotEmpty()) {
                        ChangelogDialog.createLinesComponent(player, lineMapper, previewLines)
                    } else {
                        translatableManual(player, "dialog.editor.empty_preview")
                    },
                    ChangelogDialog.LINE_WIDTH,
                ),
            ),
        )

        val inputs = listOf(
            Input(
                "line",
                TextInputControl(
                    LINE_INPUT_WIDTH,
                    translatableManual(player, "dialog.editor.input.contents"),
                    true,
                    session.currentLine,
                    LINE_INPUT_MAX_LENGTH,
                    null,
                ),
            ),
            Input(
                "author",
                TextInputControl(
                    AUTHOR_INPUT_WIDTH,
                    translatableManual(player, "dialog.editor.input.author"),
                    true,
                    session.author,
                    AUTHOR_INPUT_MAX_LENGTH,
                    null,
                ),
            ),
        )
        val common = CommonDialogData(
            translatableManual(player, titleKey(session)),
            null,
            true,
            false,
            DialogAction.NONE,
            bodyList,
            inputs,
        )
        val buttons = mutableListOf<ActionButton>()
        buttons.add(
            ActionButton(
                CommonButtonData(
                    translatableManual(player, "${sessionTranslation}.commit_button"),
                    null,
                    COMMIT_BUTTON_WIDTH,
                ),
                sessionManager.createSessionBasedAction(this, "commit", true),
            ),
        )
        val lineAction = if (session.editingLineIndex != -1) "edit_line" else "add_line"
        buttons.add(
            ActionButton(
                CommonButtonData(
                    translatableManual(player, "dialog.editor.button.${lineAction}"),
                    null,
                    LINE_ACTION_BUTTON_WIDTH,
                ),
                sessionManager.createSessionBasedAction(this, lineAction, true),
            ),
        )
        val closeBtn = ActionButton(
            CommonButtonData(
                translatableManual(player, "dialog.editor.button.close"),
                null,
                CLOSE_BUTTON_WIDTH,
            ),
            sessionManager.createSessionBasedAction(this, "try_close", true),
        )
        return MultiActionDialog(common, buttons, closeBtn, EDITOR_DIALOG_COLUMNS)
    }

    private fun buildConfirmCloseDialog(
        p: PlatformPlayer,
        sessionManager: DialogSessionManager,
    ): Dialog {
        val session: EditorSession = sessionManager.getSessionData(p, EditorSession::class.java)
        val prefix = "dialog.editor.confirm_close."
        val common: CommonDialogData = DialogPackets.createSimpleDialog(
            p,
            titleKey(session),
            "${prefix}content",
            closeAfter = false,
            canEscape = false,
        )

        fun closeAction(save: Boolean): com.github.retrooper.packetevents.protocol.dialog.action.Action {
            val payload = NBTCompound()
            payload.setTag("save_session", NBTByte(save))
            return sessionManager.createSessionBasedAction(this, "close", payload, false)
        }

        val saveSession = ActionButton(
            CommonButtonData(
                translatableManual(p, "${prefix}button.save_session"),
                null,
                SAVE_SESSION_BUTTON_WIDTH,
            ),
            closeAction(true),
        )
        val discardSession = ActionButton(
            CommonButtonData(
                translatableManual(p, "${prefix}button.discard_session"),
                null,
                DISCARD_SESSION_BUTTON_WIDTH,
            ),
            closeAction(false),
        )
        val cancel = ActionButton(
            CommonButtonData(
                translatableManual(p, "${prefix}button.cancel"),
                null,
                CANCEL_BUTTON_WIDTH,
            ),
            sessionManager.createSessionBasedAction(this, "reopen", false),
        )
        return MultiActionDialog(
            common,
            listOf(saveSession, discardSession, cancel),
            null,
            CONFIRM_CLOSE_COLUMNS,
        )
    }

    private fun actuallyClose(source: PlatformPlayer, sessionManager: DialogSessionManager) {
        sessionManager.endSession(source)
        DialogPackets.clearDialog(source, DialogPackets.PacketPhase.PLAY)
    }

    override fun onActionTriggered(
        action: String,
        data: NBTCompound?,
        source: PlatformPlayer,
        sessionManager: DialogSessionManager,
    ) {
        val session: EditorSession = sessionManager.getSessionData(source, EditorSession::class.java)
        session.showRestoredMessage = false
        if (data != null && data.contains("line")) {
            val rawLine: String = data.getStringTagValueOrThrow("line")
            val rawAuthor: String = data.getStringTagValueOrThrow("author")
            session.currentLine = rawLine
            session.author = rawAuthor
        }
        when (action) {
            "try_close" -> {
                if (session.canBeSaved()) {
                    val dialog = this.buildConfirmCloseDialog(source, sessionManager)
                    DialogPackets.showDialog(source, dialog, DialogPackets.PacketPhase.PLAY)
                    return
                }
                this.actuallyClose(source, sessionManager)
            }

            "close" -> {
                if (data == null) return
                val saveSession: Boolean = data.getBooleanOrThrow("save_session")
                if (saveSession) {
                    session.showRestoredMessage = true
                    this.savedSessions[source.uniqueId] = session
                }
                this.actuallyClose(source, sessionManager)
            }

            "reopen" -> this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY)

            "add_line", "edit_line" -> {
                if (action == "edit_line" && session.editingLineIndex == -1) return
                var removed = false
                if (session.currentLine.isBlank()) {
                    if (action == "add_line") {
                        this.showRetry(source, "add_empty_line", session, sessionManager)
                        return
                    } else {
                        session.rawLines.removeAt(session.editingLineIndex)
                        removed = true
                    }
                }
                if (action == "edit_line") {
                    if (!removed) session.rawLines[session.editingLineIndex] = session.currentLine
                    session.editingLineIndex = -1
                } else {
                    session.rawLines.add(session.currentLine)
                }
                session.currentLine = ""
                this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY)
            }

            "start_edit_line", "remove_line" -> {
                if (data == null) return
                val lineIdx = data.getNumberTagValueOrThrow("target_line").toInt()
                if (lineIdx < 0 || lineIdx >= session.rawLines.size) return
                if (action == "start_edit_line") {
                    val line = session.rawLines[lineIdx]
                    session.currentLine = line
                    session.editingLineIndex = lineIdx
                } else {
                    if (session.editingLineIndex == lineIdx) session.editingLineIndex = -1
                    session.rawLines.removeAt(lineIdx)
                    session.currentLine = ""
                }
                this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY)
            }

            "commit" -> {
                if (!permissionCheck(
                        session.permission,
                        source,
                        useFallbackPermissions,
                    )
                ) {
                    sessionManager.endSession(source)
                    DialogPackets.showSimpleNotice(
                        source,
                        titleKey(session),
                        "dialog.editor.error.no_permission",
                    )
                    return
                }
                if (data == null) return
                if (session.rawLines.isEmpty()) {
                    this.showRetry(source, "no_lines", session, sessionManager)
                    return
                }
                ServerChangelogs.platform.coroutineScope.launch {
                    try {
                        session.commitAsync(this@ChangelogEditorDialog.storage)
                    } catch (e: EditorSession.CommitException) {
                        this@ChangelogEditorDialog.showRetry(
                            source,
                            "${session.id}.${e.translationKeyPart}",
                            session,
                            sessionManager,
                        )
                        return@launch
                    }
                    sessionManager.endSession(source)
                    DialogPackets.showSimpleNotice(
                        source,
                        titleKey(session),
                        "dialog.editor.${session.id}.success",
                    )
                }
            }
        }
    }

    @Throws(PluginDialog.DestroyRejectedException::class)
    override fun attemptDestroy() {
        if (this.savedSessions.isNotEmpty()) throw PluginDialog.DestroyRejectedException("saved_sessions")
    }

    private fun showRetry(
        source: PlatformPlayer,
        errorPart: String,
        session: EditorSession,
        sessionManager: DialogSessionManager,
    ) {
        DialogPackets.showSimpleNotice(
            source,
            titleKey(session),
            "dialog.editor.error.${errorPart}",
            sessionManager.createSessionBasedAction(this, "reopen", false),
            DialogPackets.PacketPhase.PLAY,
        )
    }

    companion object {
        fun titleKey(session: EditorSession): String = "dialog.editor.${session.id}.title"

        fun permissionCheck(
            permission: String,
            source: PlatformPlayer,
            useFallbackPermissions: Boolean,
        ): Boolean {
            return if (useFallbackPermissions) {
                source.isNativeAdmin.toBooleanOrElse(false)
            } else {
                source.hasPermission("${ServerChangelogs.NAMESPACE}.${permission}")
            }
        }
    }
}
