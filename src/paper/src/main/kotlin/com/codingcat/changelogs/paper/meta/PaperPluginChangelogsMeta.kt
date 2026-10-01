package com.codingcat.changelogs.paper.meta

import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta
import io.papermc.paper.plugin.configuration.PluginMeta

class PaperPluginChangelogsMeta(
    private val pluginMeta: PluginMeta,
) : ChangelogsMeta {
    override val name: String
        get() = pluginMeta.name

    override val version: String
        get() = pluginMeta.version

    override val description: String?
        get() = pluginMeta.description

    override val authors: Set<String>?
        get() = pluginMeta.authors.takeIf { it.isNotEmpty() }?.toSet()
}
