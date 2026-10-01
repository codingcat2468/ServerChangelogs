package com.codingcat.changelogs.base.dialog

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.base.dialog.ui.ChangelogDialog
import com.codingcat.changelogs.base.dialog.ui.editor.ChangelogEditorDialog
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.protocol.dialog.Dialog
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound

/**
 * Contract representing an interactive dialog screen constructed and displayed to a [PlatformPlayer].
 */
interface PluginDialog {
    /**
     * Unique identifier string for this dialog type.
     */
    val id: String

    /**
     * Constructs a PacketEvents [Dialog] tailored for [player] within the given [sessionManager].
     */
    fun build(player: PlatformPlayer, sessionManager: DialogSessionManager): Dialog

    /**
     * Associates [session] data with [player] and displays the dialog during [packetPhase].
     */
    fun showTo(
        player: PlatformPlayer,
        sessionManager: DialogSessionManager,
        session: Any,
        packetPhase: DialogPackets.PacketPhase,
    ) {
        sessionManager.startSession(this, player, session)
        this.showTo(player, sessionManager, packetPhase)
    }

    /**
     * Renders and displays the dialog to [player] during [packetPhase].
     */
    fun showTo(player: PlatformPlayer, sessionManager: DialogSessionManager, packetPhase: DialogPackets.PacketPhase) {
        val dialog = this.build(player, sessionManager)
        player.showDialog(dialog, packetPhase)
    }

    /**
     * Callback invoked when the user interacts with an element triggering [action] with optional NBT [data].
     */
    fun onActionTriggered(
        action: String,
        data: NBTCompound?,
        source: PlatformPlayer,
        sessionManager: DialogSessionManager,
    )

    /**
     * Pre-check hook allowing active sessions to reject dialog destruction or plugin reloading.
     *
     * @throws DestroyRejectedException if active editing state prevents safe reload.
     */
    @Throws(DestroyRejectedException::class)
    fun attemptDestroy() {
    }

    object Holder {
        private val dialogMap: MutableMap<String, PluginDialog> = mutableMapOf()

        fun recreate() {
            this.dialogMap.clear()
            val editorDialog = ChangelogEditorDialog(
                ServerChangelogs.changelogStorage,
                ServerChangelogs.pluginConfig().useNativeFallbackPermissions(),
            )
            val changelogDialog = ChangelogDialog(
                ServerChangelogs.changelogStorage,
                this,
                ServerChangelogs.pluginConfig().dateFormatter,
                ServerChangelogs.pluginConfig().showChangelogHeader(),
                ServerChangelogs.pluginConfig().createChangelogHeaderStack(),
                ServerChangelogs.pluginConfig().useNativeFallbackPermissions(),
            )
            this.dialogMap[editorDialog.id] = editorDialog
            this.dialogMap[changelogDialog.id] = changelogDialog
        }

        @Throws(DestroyRejectedException::class)
        fun ensureCanReload() {
            this.dialogMap.values.forEach { it.attemptDestroy() }
        }

        fun getFromId(id: String): PluginDialog {
            return this.dialogMap[id] ?: throw NullPointerException("No dialog found matching ID \"${id}\"")
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : PluginDialog> getFromType(cls: Class<T>): T {
            val found = this.dialogMap.values.find { cls.isAssignableFrom(it.javaClass) }
                ?: throw IllegalArgumentException("No matching dialog found")
            return found as T
        }

        inline fun <reified T : PluginDialog> getFromType(): T =
            getFromType(T::class.java)
    }

    class DestroyRejectedException(val key: String) : Exception()
}
