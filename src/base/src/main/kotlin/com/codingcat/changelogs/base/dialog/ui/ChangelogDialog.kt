package com.codingcat.changelogs.base.dialog.ui

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.compat.PacketEventsFix
import com.codingcat.changelogs.base.data.ChangelogEntry
import com.codingcat.changelogs.base.data.ChangelogStorage
import com.codingcat.changelogs.base.dialog.DialogPackets
import com.codingcat.changelogs.base.dialog.DialogSessionManager
import com.codingcat.changelogs.base.dialog.PluginDialog
import com.codingcat.changelogs.base.dialog.ui.editor.ChangelogEditorDialog
import com.codingcat.changelogs.base.dialog.ui.editor.EditorSession
import com.codingcat.changelogs.base.lang.TranslationSource
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData
import com.github.retrooper.packetevents.protocol.dialog.Dialog
import com.github.retrooper.packetevents.protocol.dialog.DialogAction
import com.github.retrooper.packetevents.protocol.dialog.NoticeDialog
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody
import com.github.retrooper.packetevents.protocol.dialog.body.ItemDialogBody
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.nbt.NBTInt
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import java.time.format.DateTimeFormatter

private const val EMPTY_DIALOG_WIDTH: Int = 350
private const val HEADER_MESSAGE_WIDTH: Int = 260
private const val HEADER_ITEM_WIDTH: Int = 17
private const val HEADER_ITEM_HEIGHT: Int = 17
private const val CLOSE_BUTTON_WIDTH: Int = 60

/**
 * Dialog UI presenting published server changelogs to players.
 */
open class ChangelogDialog(
    private val storage: ChangelogStorage,
    private val holder: PluginDialog.Holder,
    private val dateFormatter: DateTimeFormatter,
    private val addHeader: Boolean,
    private val headerItem: ItemStack?,
    private val useFallbackPermissions: Boolean,
) : PluginDialog {
    override val id: String = "changelog_view"

    /**
     * Shows the changelog view dialog to the specified player during the given packet phase.
     */
    open fun showDialogView(
        player: PlatformPlayer,
        sessionManager: DialogSessionManager,
        packetPhase: DialogPackets.PacketPhase,
    ) {
        showTo(player, sessionManager, packetPhase)
    }

    override fun build(
        player: PlatformPlayer,
        sessionManager: DialogSessionManager,
    ): Dialog {
        val canManage = this.canManage(player, sessionManager)
        if (canManage) sessionManager.startSessionIfNoneActive(this, player) { Any() }
        val bodyList = mutableListOf<DialogBody>()

        if (this.addHeader) {
            val headerMessage = PlainMessage(
                TranslationSource.translatableManual(player, "dialog.changelog.header"),
                HEADER_MESSAGE_WIDTH,
            )
            val body = headerItem?.let { item ->
                ItemDialogBody(
                    item,
                    headerMessage,
                    false,
                    false,
                    HEADER_ITEM_WIDTH,
                    HEADER_ITEM_HEIGHT,
                ).also(PacketEventsFix::fixItemBody)
            } ?: PlainMessageDialogBody(headerMessage)
            bodyList.add(body)
        }

        val entries = this.storage.listEntries().asReversed()
        if (entries.isEmpty()) {
            bodyList.add(
                PlainMessageDialogBody(
                    PlainMessage(
                        TranslationSource.translatableManual(player, "dialog.changelog.empty"),
                        EMPTY_DIALOG_WIDTH,
                    ),
                ),
            )
        } else {
            entries.forEach { entry ->
                val formatted = this@ChangelogDialog.formatEntry(entry, player, sessionManager, canManage)
                bodyList.add(
                    PlainMessageDialogBody(
                        PlainMessage(formatted, LINE_WIDTH),
                    ),
                )
            }
        }

        val common = CommonDialogData(
            TranslationSource.translatableManual(player, "dialog.changelog.title"),
            null,
            true,
            false,
            if (canManage) DialogAction.NONE else DialogAction.CLOSE,
            bodyList,
            mutableListOf(),
        )
        val button = ActionButton(
            CommonButtonData(
                TranslationSource.translatableManual(player, "dialog.changelog.button.close"),
                null,
                CLOSE_BUTTON_WIDTH,
            ),
            sessionManager.createStaticAction(this, "confirm_read"),
        )
        return NoticeDialog(common, button)
    }

    private fun formatEntry(
        entry: ChangelogEntry,
        player: PlatformPlayer,
        sessionManager: DialogSessionManager,
        canManage: Boolean,
    ): Component {
        val linesComponent: Component = createLinesComponent(player, null, entry.lines)
        val unreadSuffix = if (this.storage.isUnreadFor(entry, player.uniqueId)) "_unread" else ""
        var entryComponent: Component = TranslationSource.translatableManual(
            player,
            "dialog.changelog.entry${unreadSuffix}",
            Component.text(dateFormatter.format(entry.recordedAt)),
            linesComponent,
            entry.author ?: TranslationSource.translatableManual(player, "dialog.changelog.unspecified_author"),
        )
        if (canManage) {
            val prefix = "dialog.changelog.manage.changelog_action."
            val payload = NBTCompound()
            payload.setTag("uid", NBTInt(entry.uid))
            val edit: Component = TranslationSource.translatableManual(
                player,
                "${prefix}format",
                TranslationSource.translatable("${prefix}edit"),
            ).clickEvent(sessionManager.createSessionBasedClickEvent(this, "start_editing", payload))

            val delete: Component = TranslationSource.translatableManual(
                player,
                "${prefix}format",
                TranslationSource.translatable("${prefix}delete"),
            ).clickEvent(sessionManager.createSessionBasedClickEvent(this, "request_delete", payload))

            entryComponent = entryComponent.appendSpace().append(edit).appendSpace().append(delete)
        }
        return entryComponent
    }

    override fun onActionTriggered(
        action: String,
        data: NBTCompound?,
        source: PlatformPlayer,
        sessionManager: DialogSessionManager,
    ) {
        try {
            val canManage = this.canManage(source, sessionManager)
            when (action) {
                "confirm_read" -> {
                    val unreadEntries = this.storage.listEntries().filter {
                        this.storage.isUnreadFor(it, source.uniqueId)
                    }
                    if (unreadEntries.isNotEmpty()) {
                        val uids = unreadEntries.map { it.uid }
                        ServerChangelogs.platform.coroutineScope.launch {
                            this@ChangelogDialog.storage.markAllAsReadAsync(uids, source.uniqueId)
                        }
                    }
                    if (canManage) {
                        sessionManager.endSession(source)
                        DialogPackets.clearDialog(source, DialogPackets.PacketPhase.PLAY)
                    }
                    if (unreadEntries.isNotEmpty()) {
                        source.asAudience().sendMessage(
                            TranslationSource.translatable(
                                "dialog.changelog.read",
                                Component.text(unreadEntries.size),
                            ),
                        )
                    }
                }

                "reopen" -> {
                    if (canManage) this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY)
                }

                "start_editing" -> {
                    if (data == null || !canManage) return
                    val uid = data.getNumberTagValueOrThrow("uid").toInt()
                    val entry: ChangelogEntry = this.storage.getByUID(uid) ?: return
                    sessionManager.endSession(source)
                    val session = EditorSession.Edit(entry)
                    this.holder.getFromType<ChangelogEditorDialog>()
                        .showTo(source, sessionManager, session, DialogPackets.PacketPhase.PLAY)
                }

                "request_delete" -> {
                    if (data == null || !canManage) return
                    val uid = data.getNumberTagValueOrThrow("uid").toInt()
                    val payload = NBTCompound()
                    payload.setTag("uid", NBTInt(uid))
                    val prefix = "dialog.changelog.manage.confirm_delete."
                    DialogPackets.showSimpleConfirm(
                        source,
                        "${prefix}title",
                        "${prefix}content",
                        sessionManager.createSessionBasedAction(this, "confirm_delete", payload, false),
                        true,
                        sessionManager.createSessionBasedAction(this, "reopen", false),
                        DialogPackets.PacketPhase.PLAY,
                    )
                }

                "confirm_delete" -> {
                    if (data == null || !canManage) return
                    val uid = data.getNumberTagValueOrThrow("uid").toInt()
                    ServerChangelogs.platform.coroutineScope.launch {
                        this@ChangelogDialog.storage.removeEntryAsync(uid)
                    }
                    this.showTo(source, sessionManager, DialogPackets.PacketPhase.PLAY)
                }
            }
        } finally {
            // Ensure the player is able to continue gameplay even if any exceptions occur
            if (action == "confirm_read") sessionManager.unfreeze(source)
        }
    }

    private fun canManage(player: PlatformPlayer, sessionManager: DialogSessionManager): Boolean {
        return ChangelogEditorDialog.permissionCheck(
            "manage",
            player,
            useFallbackPermissions,
        ) && !sessionManager.isFrozen(player)
    }

    companion object {
        const val LINE_WIDTH: Int = 440

        fun createLinesComponent(
            player: PlatformPlayer,
            mapper: ((Component, Int) -> Component)?,
            lines: List<Component>,
        ): Component {
            val lineMapper = mapper ?: { c, _ -> c }
            var component: Component = Component.empty()
            lines.forEachIndexed { i, rawLine ->
                var line = TranslationSource.translatableManual(player, "dialog.changelog.entry_line", rawLine)
                line = lineMapper(line, i)
                component = component.append(line)
                if (i < lines.size - 1) component = component.appendNewline()
            }
            return component
        }
    }
}
