package com.codingcat.changelogs.data;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ChangelogEntry(
        int uid,
        @NotNull List<Component> lines,
        @NotNull Instant recordedAt,
        @Nullable Component author,
        @NotNull Set<UUID> playersRead
) {
    public boolean hasRead(@NotNull Player player) {
        return this.playersRead().contains(player.getUniqueId());
    }
}
