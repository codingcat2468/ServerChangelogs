package com.codingcat.changelogs.platformapi.event.util

import com.codingcat.changelogs.platformapi.event.PlatformEventManager
import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MethodDispatchingListenerTest {
    private class DummyEvent(val message: String) : PlatformEvent

    private class AnotherDummyEvent(val count: Int) : PlatformEvent

    private class SimpleTestListener : MethodDispatchingListener {
        val received = mutableListOf<String>()

        @MethodDispatchingListener.ListenerMethod
        fun onDummy(event: DummyEvent) {
            received.add(event.message)
        }

        @MethodDispatchingListener.ListenerMethod
        fun onAnother(event: AnotherDummyEvent) {
            received.add("count:${event.count}")
        }

        fun regularMethod() {
            // Ignored because no annotation
        }
    }

    private class InvalidParamsCountListener : MethodDispatchingListener {
        @MethodDispatchingListener.ListenerMethod
        fun invalidMethod(event: DummyEvent, extra: String) {
        }
    }

    private class InvalidParamTypeListener : MethodDispatchingListener {
        @MethodDispatchingListener.ListenerMethod
        fun invalidType(event: String) {
        }
    }

    private class ThrowingListener : MethodDispatchingListener {
        @MethodDispatchingListener.ListenerMethod
        fun onThrow(event: DummyEvent) {
            throw IllegalStateException("Failure in listener")
        }
    }

    private class MockPlatformEventManager : PlatformEventManager {
        val listeners = mutableListOf<Pair<Class<*>, (Any) -> Unit>>()

        @Suppress("UNCHECKED_CAST")
        override fun <T : PlatformEvent> registerListener(eventCls: Class<T>, listener: (T) -> Unit) {
            listeners.add(eventCls to (listener as (Any) -> Unit))
        }

        override fun unregisterListener(listener: Any) {
            listeners.removeAll { it.second == listener }
        }

        override fun unregisterIf(listenerPredicate: (Any) -> Boolean) {
            listeners.removeAll { listenerPredicate(it.second) }
        }

        override fun unregisterAll() {
            listeners.clear()
        }

        fun dispatch(event: PlatformEvent) {
            listeners.filter { it.first.isAssignableFrom(event.javaClass) }.forEach {
                it.second(event)
            }
        }
    }

    @Test
    fun testSetupAllAndDispatch() {
        val manager = MockPlatformEventManager()
        val listener = SimpleTestListener()

        listener.setupAll(manager)
        assertEquals(2, manager.listeners.size)

        manager.dispatch(DummyEvent("hello"))
        manager.dispatch(AnotherDummyEvent(42))

        assertEquals(listOf("hello", "count:42"), listener.received)

        listener.unregisterAll(manager)
        assertEquals(0, manager.listeners.size)
    }

    @Test
    fun testInvalidParameterCountThrows() {
        val manager = MockPlatformEventManager()
        val listener = InvalidParamsCountListener()
        assertFailsWith<IllegalArgumentException> {
            listener.setupAll(manager)
        }
    }

    @Test
    fun testInvalidParameterTypeThrows() {
        val manager = MockPlatformEventManager()
        val listener = InvalidParamTypeListener()
        assertFailsWith<IllegalArgumentException> {
            listener.setupAll(manager)
        }
    }

    @Test
    fun testThrowingListenerWrapsException() {
        val manager = MockPlatformEventManager()
        val listener = ThrowingListener()
        listener.setupAll(manager)

        val ex = assertFailsWith<RuntimeException> {
            manager.dispatch(DummyEvent("boom"))
        }
        assertTrue(ex.cause is IllegalStateException)
    }
}
