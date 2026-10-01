package com.codingcat.changelogs.platformapi.command

import com.codingcat.changelogs.platformapi.audience.PermissionAudienceWrapper
import com.codingcat.changelogs.platformapi.player.PlatformPlayer

/**
 * Abstraction wrapping an invocation source capable of receiving messages,
 * checking permissions, and identifying an optional executing player.
 */
interface PlatformCommandSource : PermissionAudienceWrapper {
    /**
     * The player executing this command, or `null` if the execution originated from console or automated source.
     */
    val executingPlayer: PlatformPlayer?
}
