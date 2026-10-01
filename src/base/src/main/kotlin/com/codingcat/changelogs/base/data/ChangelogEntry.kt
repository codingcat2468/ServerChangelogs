package com.codingcat.changelogs.base.data

import com.codingcat.changelogs.platformapi.player.PlatformPlayer
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.*

/**
 * Immutable record representing a single changelog post with its content, timestamp, author, and read receipts.
 */
@JvmRecord
data class ChangelogEntry(
    val uid: Int,
    val lines: List<Component>,
    val recordedAt: Instant,
    val author: Component?,
    val playersRead: MutableSet<UUID>,
) {
    /**
     * Checks whether [player] has already marked this entry as read.
     */
    fun hasRead(player: PlatformPlayer): Boolean {
        return hasRead(player.uniqueId)
    }

    /**
     * Checks whether player with [uuid] has already marked this entry as read.
     */
    fun hasRead(uuid: UUID): Boolean {
        return this.playersRead.contains(uuid)
    }
}
