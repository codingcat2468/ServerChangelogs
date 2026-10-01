package com.codingcat.changelogs.paper.event

import com.codingcat.changelogs.paper.PaperChangelogsPlatform
import com.codingcat.changelogs.paper.player.PaperPlayerManager
import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import com.codingcat.changelogs.platformapi.event.util.PlatformEventMappings
import org.bukkit.Bukkit
import org.bukkit.event.*
import org.bukkit.plugin.EventExecutor

/**
 * Event manager implementation adapting Bukkit/Paper event listeners to platform events.
 */
@Suppress("UNCHECKED_CAST")
object PaperEventManager : PlatformEventManager {
    private val listenerSet: MutableSet<PlatformMappingListener<*>> = hashSetOf()
    private val eventMappings: PaperEventMappings = PaperEventMappings(PaperPlayerManager)

    override fun <T : PlatformEvent> registerListener(eventCls: Class<T>, listener: (T) -> Unit) {
        val mapping = eventMappings.findForPlatformEvent(eventCls)
        val mappingListener = PlatformMappingListener(listener, mapping)
        Bukkit.getPluginManager().registerEvent(
            mapping.nativeEventCls,
            mappingListener,
            EventPriority.NORMAL,
            mappingListener,
            PaperChangelogsPlatform.instance,
        )
        this.listenerSet.add(mappingListener)
    }

    override fun unregisterListener(listener: Any) {
        this.unregisterIf { it === listener || it == listener }
    }

    override fun unregisterIf(listenerPredicate: (Any) -> Boolean) {
        val iterator = this.listenerSet.iterator()
        while (iterator.hasNext()) {
            val mappingListener = iterator.next()
            if (listenerPredicate(mappingListener.listener)) {
                HandlerList.unregisterAll(mappingListener)
                iterator.remove()
            }
        }
    }

    override fun unregisterAll() {
        this.listenerSet.forEach { HandlerList.unregisterAll(it) }
        this.listenerSet.clear()
    }

    private class PlatformMappingListener<T : PlatformEvent>(
        val listener: (T) -> Unit,
        private val mapping: PlatformEventMappings.Mapping<T, out Event>,
    ) : Listener, EventExecutor {
        @Throws(EventException::class)
        override fun execute(listener: Listener, event: Event) {
            runCatching {
                val platformEvent: T = (mapping as PlatformEventMappings.Mapping<T, Event>)
                    .convertToPlatform(event)
                this.listener(platformEvent)
            }.getOrElse { e ->
                throw EventException(
                    e,
                    "Failed to handle event ${event} (platform ${mapping.platformEventCls})",
                )
            }
        }
    }
}
