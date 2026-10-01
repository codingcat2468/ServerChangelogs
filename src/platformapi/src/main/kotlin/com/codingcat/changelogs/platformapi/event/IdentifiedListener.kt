package com.codingcat.changelogs.platformapi.event

import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed

/**
 * Event listener tagged with an Adventure [Key] for grouped identification and lifecycle unregistration.
 */
interface IdentifiedListener<T> : (T) -> Unit, Keyed {
    companion object {
        /**
         * Wraps an existing [listener] with an identifying [key].
         */
        fun <T> create(key: Key, listener: (T) -> Unit): IdentifiedListener<T> {
            return object : IdentifiedListener<T> {
                override fun invoke(event: T) {
                    listener(event)
                }

                override fun key(): Key = key
            }
        }
    }
}
