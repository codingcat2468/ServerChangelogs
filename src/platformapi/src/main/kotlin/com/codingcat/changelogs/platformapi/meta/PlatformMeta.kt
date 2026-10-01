package com.codingcat.changelogs.platformapi.meta

import net.kyori.adventure.text.Component

/**
 * Environmental details of the hosting platform.
 */
interface PlatformMeta {
    /**
     * Styled platform brand component (e.g. Paper, Folia, Purpur, Velocity).
     */
    val name: Component

    /**
     * Platform version string.
     */
    val version: String
}
