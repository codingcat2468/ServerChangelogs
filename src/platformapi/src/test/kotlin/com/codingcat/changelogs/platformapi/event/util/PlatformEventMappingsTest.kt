package com.codingcat.changelogs.platformapi.event.util

import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlatformEventMappingsTest {
    private class NativeTestEvent(val text: String)

    private class PlatformTestEvent(val message: String) : PlatformEvent

    private class TestEventMappings : PlatformEventMappings<Any>() {
        init {
            register(
                PlatformTestEvent::class.java,
                NativeTestEvent::class.java,
            ) { native ->
                PlatformTestEvent("wrapped:${native.text}")
            }
        }
    }

    private class UnmappedEvent : PlatformEvent

    @Test
    fun testEventMappingAndConversion() {
        val mappings = TestEventMappings()
        val mapping = mappings.findForPlatformEvent(PlatformTestEvent::class.java)

        assertEquals(PlatformTestEvent::class.java, mapping.platformEventCls)
        assertEquals(NativeTestEvent::class.java, mapping.nativeEventCls)

        val nativeEvent = NativeTestEvent("hello")

        @Suppress("UNCHECKED_CAST")
        val converted = (mapping as PlatformEventMappings.Mapping<PlatformTestEvent, NativeTestEvent>)
            .convertToPlatform(nativeEvent)

        assertEquals("wrapped:hello", converted.message)
    }

    @Test
    fun testUnmappedEventThrows() {
        val mappings = TestEventMappings()
        assertFailsWith<IllegalArgumentException> {
            mappings.findForPlatformEvent(UnmappedEvent::class.java)
        }
    }
}
