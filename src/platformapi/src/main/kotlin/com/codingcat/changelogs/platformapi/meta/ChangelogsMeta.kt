package com.codingcat.changelogs.platformapi.meta

/**
 * Metadata defining the ServerChangelogs plugin distribution.
 */
interface ChangelogsMeta {
    /**
     * Plugin display name.
     */
    val name: String

    /**
     * Semantic version string of the plugin.
     */
    val version: String

    /**
     * Brief summary describing the plugin purpose.
     */
    val description: String?

    /**
     * Set of author names credited with developing the plugin.
     */
    val authors: Set<String>?
}
