package com.codingcat.changelogs.velocity.meta

import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import com.velocitypowered.api.util.ProxyVersion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor

class VelocityPlatformMeta(
    private val proxyVersion: ProxyVersion,
) : PlatformMeta {
    override val name: Component
        get() = text(proxyVersion.name, NamedTextColor.RED)

    override val version: String
        get() = proxyVersion.version
}
