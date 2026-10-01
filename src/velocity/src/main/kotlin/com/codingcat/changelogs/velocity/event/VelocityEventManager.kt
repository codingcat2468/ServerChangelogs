package com.codingcat.changelogs.velocity.event

import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import com.codingcat.changelogs.platformapi.event.util.PlatformEventMappings
import com.codingcat.changelogs.velocity.VelocityChangelogsPlatform
import com.codingcat.changelogs.velocity.player.VelocityPlayerManager
import com.velocitypowered.api.event.AwaitingEventExecutor
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.EventTask

/**
 * Event manager implementation adapting Velocity proxy events to platform events.
 */
@Suppress("UNCHECKED_CAST")
object VelocityEventManager : PlatformEventManager {
    lateinit var eventManager: EventManager
    lateinit var platform: VelocityChangelogsPlatform

    private val handlerSet: MutableSet<PlatformMappingHandler<*>> = hashSetOf()
    private val eventMappings: VelocityEventMappings = VelocityEventMappings(VelocityPlayerManager)

    override fun <T : PlatformEvent> registerListener(eventCls: Class<T>, listener: (T) -> Unit) {
        val mapping = eventMappings.findForPlatformEvent(eventCls)
        val mappingHandler = PlatformMappingHandler(listener, mapping)

        val nativeClass = mapping.nativeEventCls as Class<Any>
        this.eventManager.register(this.platform, nativeClass, mappingHandler)
        this.handlerSet.add(mappingHandler)
    }

    override fun unregisterListener(listener: Any) {
        this.unregisterIf { it === listener || it == listener }
    }

    override fun unregisterIf(listenerPredicate: (Any) -> Boolean) {
        val iterator = this.handlerSet.iterator()
        while (iterator.hasNext()) {
            val handler = iterator.next()
            if (listenerPredicate(handler.listener)) {
                this.eventManager.unregister(this.platform, handler)
                iterator.remove()
            }
        }
    }

    override fun unregisterAll() {
        this.handlerSet.forEach { handler -> this.eventManager.unregister(platform, handler) }
        this.handlerSet.clear()
    }

    private class PlatformMappingHandler<T : PlatformEvent>(
        val listener: (T) -> Unit,
        private val mapping: PlatformEventMappings.Mapping<T, out Any>,
    ) : AwaitingEventExecutor<Any> {
        override fun execute(event: Any) {
            this.executeInternal(event)
        }

        override fun executeAsync(event: Any): EventTask {
            return EventTask.async {
                this@PlatformMappingHandler.executeInternal(event)
            }
        }

        private fun executeInternal(event: Any) {
            runCatching {
                val platformEvent: T = (mapping as PlatformEventMappings.Mapping<T, Any>).convertToPlatform(event)
                this.listener(platformEvent)
            }.getOrElse { e ->
                throw RuntimeException(
                    "Failed to handle event ${event} (platform ${mapping.platformEventCls})",
                    e,
                )
            }
        }
    }
}
