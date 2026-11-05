package com.codingcat.changelogs.paper.meta;

import com.codingcat.changelogs.platformapi.meta.PlatformMeta;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public final class PaperPlatformMeta implements PlatformMeta {
    private final @NotNull Server server;

    @Override
    public @NotNull Component getName() {
        TextColor color = NamedTextColor.AQUA;
        String lowerName = server.getName().toLowerCase(Locale.ENGLISH);
        if (lowerName.contains("purpur"))
            color = NamedTextColor.LIGHT_PURPLE;
        else if (lowerName.contains("folia"))
            color = NamedTextColor.GREEN;
        return text(this.server.getName(), color);
    }

    @Override
    public @NotNull String getVersion() {
        return this.server.getVersion();
    }
}
