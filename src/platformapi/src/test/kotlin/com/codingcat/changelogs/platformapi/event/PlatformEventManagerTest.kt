package com.codingcat.changelogs.platformapi.event

import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import com.codingcat.changelogs.platformapi.event.util.MethodDispatchingListener
import net.kyori.adventure.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformEventManagerTest {
    private class DummyEvent(val num: Int) : PlatformEvent

    private class SampleListener : MethodDispatchingListener {
        var callCount = 0

        @MethodDispatchingListener.ListenerMethod
        fun onEvent(event: DummyEvent) {
            callCount += event.num
        }
    }

    private class SimpleEventManager : PlatformEventManager {
        val registered = mutableListOf<Pair<Class<*>, (Any) -> Unit>>()

        @Suppress("UNCHECKED_CAST")
        override fun <T : PlatformEvent> registerListener(eventCls: Class<T>, listener: (T) -> Unit) {
            registered.add(eventCls to (listener as (Any) -> Unit))
        }

        override fun unregisterListener(listener: Any) {
            registered.removeAll { it.second == listener }
        }

        override fun unregisterIf(listenerPredicate: (Any) -> Boolean) {
            registered.removeAll { listenerPredicate(it.second) }
        }

        override fun unregisterAll() {
            registered.clear()
        }

        fun dispatch(event: PlatformEvent) {
            registered.filter { it.first.isAssignableFrom(event.javaClass) }.forEach {
                it.second(event)
            }
        }
    }

    @Test
    fun testIdentifiedListenerRegistrationAndUnregistration() {
        val manager = SimpleEventManager()
        val key1 = Key.key("test", "listener_one")
        val key2 = Key.key("test", "listener_two")

        var count1 = 0
        var count2 = 0

        manager.registerIdentified(DummyEvent::class.java, key1) { count1++ }
        manager.registerIdentified<DummyEvent>(key2) { count2++ }

        assertEquals(2, manager.registered.size)
        manager.dispatch(DummyEvent(1))
        assertEquals(1, count1)
        assertEquals(1, count2)

        manager.unregisterIdentified(key1)
        assertEquals(1, manager.registered.size)

        manager.dispatch(DummyEvent(1))
        assertEquals(1, count1)
        assertEquals(2, count2)

        manager.unregisterAll()
        assertEquals(0, manager.registered.size)
    }

    @Test
    fun testRegisterMethodDispatcher() {
        val manager = SimpleEventManager()
        val listener = SampleListener()

        manager.registerMethodDispatcher(listener)
        assertEquals(1, manager.registered.size)

        manager.dispatch(DummyEvent(5))
        assertEquals(5, listener.callCount)
    }
}
