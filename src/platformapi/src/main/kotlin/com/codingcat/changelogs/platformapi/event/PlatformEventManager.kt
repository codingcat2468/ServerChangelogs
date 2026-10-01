package com.codingcat.changelogs.platformapi.event

import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import com.codingcat.changelogs.platformapi.event.util.MethodDispatchingListener
import net.kyori.adventure.key.Key

/**
 * Manager handling subscription, identification, and lifecycle cleanup of platform-neutral event listeners.
 */
interface PlatformEventManager {
    /**
     * Binds all listener methods discovered on a [MethodDispatchingListener] instance to this manager.
     */
    fun registerMethodDispatcher(methodDispatcher: MethodDispatchingListener) {
        methodDispatcher.setupAll(this)
    }

    /**
     * Registers a listener tagged with a unique [key], enabling targeted unregistration later.
     */
    fun <T : PlatformEvent> registerIdentified(eventCls: Class<T>, key: Key, listener: (T) -> Unit) {
        this.registerListener(eventCls, IdentifiedListener.create(key, listener))
    }

    /**
     * Subscribes a listener to events of type [eventCls].
     */
    fun <T : PlatformEvent> registerListener(eventCls: Class<T>, listener: (T) -> Unit)

    /**
     * Unregisters the exact listener instance.
     */
    fun unregisterListener(listener: Any)

    /**
     * Removes all registered [IdentifiedListener] instances associated with [identifier].
     */
    fun unregisterIdentified(identifier: Key) {
        this.unregisterIf { e -> e is IdentifiedListener<*> && e.key() == identifier }
    }

    /**
     * Unregisters any active listeners matching [listenerPredicate].
     */
    fun unregisterIf(listenerPredicate: (Any) -> Boolean)

    /**
     * Removes all active event subscriptions.
     */
    fun unregisterAll()
}

/**
 * Reified helper for registering an anonymous listener for [T].
 */
inline fun <reified T : PlatformEvent> PlatformEventManager.registerListener(noinline listener: (T) -> Unit) {
    registerListener(T::class.java, listener)
}

/**
 * Reified helper for registering an identified listener tagged with [key] for [T].
 */
inline fun <reified T : PlatformEvent> PlatformEventManager.registerIdentified(
    key: Key,
    noinline listener: (T) -> Unit,
) {
    registerIdentified(T::class.java, key, listener)
}
