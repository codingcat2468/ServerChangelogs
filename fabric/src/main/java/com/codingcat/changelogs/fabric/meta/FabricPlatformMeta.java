package com.codingcat.changelogs.fabric.meta;

import com.codingcat.changelogs.platformapi.meta.PlatformMeta;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

public final class FabricPlatformMeta implements PlatformMeta {
    private static final @NotNull TextColor COLOR = TextColor.color(0xDBD0B4);

    @Override
    public @NotNull Component getName() {
        return text("Fabric", COLOR);
    }

    @Override
    public @NotNull String getVersion() {
        FabricLoader loader = FabricLoader.getInstance();
        return version(loader, "fabricloader") + " (MC " + version(loader, "minecraft") + ")";
    }

    private static @NotNull String version(@NotNull FabricLoader loader, @NotNull String modId) {
        return loader.getModContainer(modId).map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }
}
