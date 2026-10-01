package com.codingcat.changelogs.platformapi.event.util

import com.codingcat.changelogs.platformapi.event.impl.PlatformEvent

/**
 * Registry holding bidirectional type mappings between platform-neutral [PlatformEvent]
 * types and platform-native events [E].
 */
@Suppress("UNCHECKED_CAST")
abstract class PlatformEventMappings<E : Any> {
    private val eventMappings = mutableSetOf<Mapping<*, out E>>()

    /**
     * Registers a mapping translating native event [nativeEventCls] into platform event
     * [platformEventCls] using [mapper].
     */
    protected fun <T : PlatformEvent, N : E> register(
        platformEventCls: Class<T>,
        nativeEventCls: Class<N>,
        mapper: (N) -> T,
    ) {
        eventMappings += Mapping(platformEventCls, nativeEventCls, mapper)
    }

    /**
     * Locates the registered mapping for [eventCls].
     *
     * @throws IllegalArgumentException if no mapping exists for the requested event class.
     */
    fun <T : PlatformEvent> findForPlatformEvent(eventCls: Class<T>): Mapping<T, out E> {
        val mapping = eventMappings.firstOrNull { it.platformEventCls == eventCls }
            ?: throw IllegalArgumentException("Unable to find matching native event for ${eventCls}")

        return mapping as Mapping<T, out E>
    }

    /**
     * Descriptor pairing a platform event class [PL] and native event class [PA] with a conversion function.
     */
    data class Mapping<PL : PlatformEvent, PA : Any>(
        val platformEventCls: Class<PL>,
        val nativeEventCls: Class<PA>,
        val mapper: (PA) -> PL,
    ) {
        /**
         * Transforms the supplied [nativeEvent] into its platform-neutral representation.
         */
        fun convertToPlatform(nativeEvent: PA): PL = mapper(nativeEvent)
    }
}
