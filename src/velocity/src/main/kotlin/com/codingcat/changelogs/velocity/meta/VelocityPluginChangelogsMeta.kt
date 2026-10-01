package com.codingcat.changelogs.velocity.meta

import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import com.velocitypowered.api.plugin.PluginDescription
import kotlin.jvm.optionals.getOrNull

class VelocityPluginChangelogsMeta(
    private val pluginDescription: PluginDescription,
) : ChangelogsMeta {
    override val name: String
        get() = pluginDescription.name.orElseGet { pluginDescription.id }

    override val version: String
        get() = pluginDescription.version.getOrNull() ?: "Unknown"

    override val description: String?
        get() = pluginDescription.description.getOrNull()

    override val authors: Set<String>?
        get() = pluginDescription.authors.takeIf { it.isNotEmpty() }?.toSet()
}
