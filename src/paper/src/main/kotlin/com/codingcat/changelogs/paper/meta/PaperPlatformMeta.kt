package com.codingcat.changelogs.paper.meta

import com.codingcat.changelogs.platformapi.meta.PlatformMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit

object PaperPlatformMeta : PlatformMeta {
    override val name: TextComponent
        get() {
            val serverName: String = Bukkit.getName()
            val color: TextColor = when {
                serverName.contains("purpur", true) -> NamedTextColor.LIGHT_PURPLE
                serverName.contains("folia", true) -> NamedTextColor.GREEN
                else -> NamedTextColor.AQUA
            }
            return Component.text(serverName, color)
        }

    override val version: String
        get() = Bukkit.getVersion()
}
