package com.codingcat.changelogs.platformapi

import com.github.retrooper.packetevents.PacketEventsAPI

/**
 * Common entrypoint contract for the core plugin lifecycle managed by a [ChangelogsPlatform].
 */
interface Entrypoint {
    /**
     * Active host platform providing services and metadata.
     */
    val platform: ChangelogsPlatform

    /**
     * PacketEvents API instance.
     */
    val packetEventsApi: PacketEventsAPI<out Any>

    /**
     * Initializes configurations, storage backends, and command/event registrations on startup.
     */
    suspend fun onStart()

    /**
     * Shuts down storage, flushes pending state, and releases registered resources.
     */
    suspend fun onShutdown()
}
