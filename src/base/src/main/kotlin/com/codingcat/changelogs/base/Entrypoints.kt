package com.codingcat.changelogs.base

import com.codingcat.changelogs.platformapi.ChangelogsPlatform
import com.codingcat.changelogs.platformapi.Entrypoint

/**
 * Factory creating and binding the common [Entrypoints] singleton to the host [ChangelogsPlatform].
 */
object Entrypoints {
    /**
     * Binds [platform] to [ServerChangelogs] and returns the singleton entrypoint.
     */
    fun create(platform: ChangelogsPlatform): Entrypoint {
        ServerChangelogs.platform = platform
        return ServerChangelogs
    }
}
