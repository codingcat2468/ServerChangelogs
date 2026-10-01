package com.codingcat.changelogs.base.dialog

import com.codingcat.changelogs.base.ServerChangelogs
import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketListenerCommon
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.chat.clickevent.CustomClickEvent
import com.github.retrooper.packetevents.protocol.dialog.action.Action
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction
import com.github.retrooper.packetevents.protocol.dialog.action.StaticAction
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.util.NbtCodecException
import com.github.retrooper.packetevents.resources.ResourceLocation
import com.github.retrooper.packetevents.util.adventure.NbtTagHolder
import com.github.retrooper.packetevents.wrapper.common.client.WrapperCommonClientCustomClickAction
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientCustomClickAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.event.ClickEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * PacketEvents listener coordinating dialog click actions, session state, and player freeze deferreds.
 */
object DialogSessionManager : PacketListener {
    private val staticActions: MutableSet<Key> = ConcurrentHashMap.newKeySet()
    private val activeSessions: MutableMap<UUID, String> = ConcurrentHashMap()
    private val sessionData: MutableMap<UUID, Any> = ConcurrentHashMap()
    private val frozenViewers: MutableMap<UUID, CompletableDeferred<Unit>> = ConcurrentHashMap()
    private var selfListener: PacketListenerCommon? = null

    fun handleCustomClick(packetWrapper: WrapperCommonClientCustomClickAction<*>, player: PlatformPlayer) {
        val key: Key = packetWrapper.id.key()
        val fSIdx: Int = key.value().indexOf('/')
        val lSIdx: Int = key.value().lastIndexOf('/')
        if (key.namespace() != ServerChangelogs.NAMESPACE ||
            fSIdx == -1 ||
            !key.value().startsWith("dialog/") ||
            (fSIdx + 1) >= lSIdx ||
            lSIdx == (key.value().length - 1)
        ) {
            return
        }
        val dialogId = key.value().substring(fSIdx + 1, lSIdx)
        val actionId = key.value().substring(lSIdx + 1)
        if (dialogId != activeSessions[player.uniqueId] && !staticActions.contains(key)) return
        val dialog: PluginDialog = runCatching {
            ServerChangelogs.dialogHolder.getFromId(dialogId)
        }.getOrNull() ?: return

        runCatching {
            dialog.onActionTriggered(actionId, packetWrapper.payload as? NBTCompound, player, this)
        }.onFailure { e ->
            if (e !is NbtCodecException) {
                throw RuntimeException(
                    "Failed to run dialog action handler for \"${dialogId}\", action \"${actionId}\"",
                    e,
                )
            }
        }
    }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType === PacketType.Play.Client.CUSTOM_CLICK_ACTION) {
            handleCustomClick(
                WrapperPlayClientCustomClickAction(event),
                playerFromPacket(event),
            )
        }
        if (event.packetType === PacketType.Configuration.Client.CUSTOM_CLICK_ACTION) {
            handleCustomClick(
                WrapperConfigClientCustomClickAction(event),
                playerFromPacket(event),
            )
        }
    }

    private fun playerFromPacket(event: PacketReceiveEvent): PlatformPlayer {
        return runCatching {
            ServerChangelogs.platform.playerManager.fromNative(event.getPlayer())
        }.getOrElse {
            // If obtaining the native PlatformPlayer implementation fails (e.g. during the configuration phase,
            // where event.getPlayer() can return null), fall back to a wrapped PE User implementation.
            DialogPackets.wrapPacketEventsUser(event.user)
        }
    }

    fun registerEvents() {
        this.selfListener =
            ServerChangelogs.packetEventsApi.eventManager.registerListener(this, PacketListenerPriority.NORMAL)
    }

    fun unregisterEvents() {
        ServerChangelogs.packetEventsApi.eventManager.unregisterListener(this.selfListener)
        this.selfListener = null
    }

    fun createStaticAction(dialog: PluginDialog, id: String): Action {
        val key = this.createStaticKey(dialog, id)
        return StaticAction(CustomClickEvent(ResourceLocation(key), null))
    }

    fun createStaticClickEvent(dialog: PluginDialog, id: String): ClickEvent<*> {
        return ClickEvent.custom(this.createStaticKey(dialog, id), NONE_TAG_HOLDER)
    }

    fun createSessionBasedAction(dialog: PluginDialog, id: String, includeInputs: Boolean): Action {
        return this.createSessionBasedAction(dialog, id, null, includeInputs)
    }

    fun createSessionBasedAction(
        dialog: PluginDialog,
        id: String,
        data: NBTCompound?,
        includeInputs: Boolean,
    ): Action {
        val key = this.createSessionBasedKey(dialog, id)
        val clickEvent = CustomClickEvent(ResourceLocation(key), data)
        return if (includeInputs) {
            DynamicCustomAction(clickEvent.id, data)
        } else {
            StaticAction(clickEvent)
        }
    }

    fun createSessionBasedClickEvent(dialog: PluginDialog, id: String): ClickEvent<*> {
        return ClickEvent.custom(this.createSessionBasedKey(dialog, id), null)
    }

    fun createSessionBasedClickEvent(dialog: PluginDialog, id: String, data: NBTCompound?): ClickEvent<*> {
        return ClickEvent.custom(
            this.createSessionBasedKey(dialog, id),
            data?.let(::NbtTagHolder) ?: NONE_TAG_HOLDER,
        )
    }

    private fun createSessionBasedKey(dialog: PluginDialog, id: String): Key {
        return ServerChangelogs.key("dialog/${dialog.id}/${id}")
    }

    private fun createStaticKey(dialog: PluginDialog, id: String): Key {
        val key = this.createSessionBasedKey(dialog, id)
        this.staticActions.add(key)
        return key
    }

    fun startSessionIfNoneActive(
        dialog: PluginDialog,
        player: PlatformPlayer,
        initialDataSupplier: () -> Any,
    ) {
        if (this.isSessionActive(player, dialog)) return
        this.startSession(dialog, player, initialDataSupplier())
    }

    fun startSession(dialog: PluginDialog, player: PlatformPlayer, initialData: Any) {
        this.setSessionData(player, initialData)
        this.activeSessions[player.uniqueId] = dialog.id
    }

    fun endSession(player: PlatformPlayer) {
        this.activeSessions.remove(player.uniqueId)
        this.sessionData.remove(player.uniqueId)
    }

    fun isSessionActive(player: PlatformPlayer, dialog: PluginDialog): Boolean {
        return dialog.id == this.activeSessions[player.uniqueId]
    }

    fun setSessionData(player: PlatformPlayer, data: Any) {
        this.sessionData[player.uniqueId] = data
    }

    @Throws(ClassCastException::class)
    fun <T> getSessionData(player: PlatformPlayer, dataType: Class<T>): T {
        return dataType.cast(this.sessionData[player.uniqueId])
    }

    fun isFrozen(player: PlatformPlayer): Boolean {
        val deferred = this.frozenViewers[player.uniqueId]
        return deferred != null && !deferred.isCompleted
    }

    fun unfreeze(player: PlatformPlayer) {
        val deferred = this.frozenViewers.remove(player.uniqueId)
        deferred?.complete(Unit)
    }

    fun freeze(player: PlatformPlayer) {
        this.frozenViewers.computeIfAbsent(player.uniqueId) { CompletableDeferred() }
    }

    suspend fun awaitUnfreeze(player: PlatformPlayer, timeoutSeconds: Long = 45) {
        val deferred = this.frozenViewers[player.uniqueId] ?: return
        runCatching {
            withTimeout(timeoutSeconds * 1000) {
                deferred.await()
            }
        }.onFailure {
            // Deferred timed out, was cancelled, or completed exceptionally
        }
        this.frozenViewers.remove(player.uniqueId)
    }

    fun waitForUnfreeze(player: PlatformPlayer, timeoutSeconds: Long = 45) {
        val deferred = this.frozenViewers[player.uniqueId] ?: return
        runBlocking {
            runCatching {
                withTimeout((timeoutSeconds * 1000).milliseconds) {
                    deferred.await()
                }
            }.onFailure {
                // Deferred timed out, was cancelled, or completed exceptionally
            }
        }
        this.frozenViewers.remove(player.uniqueId)
    }

    val NONE_TAG_HOLDER: BinaryTagHolder = BinaryTagHolder.binaryTagHolder("")
}
