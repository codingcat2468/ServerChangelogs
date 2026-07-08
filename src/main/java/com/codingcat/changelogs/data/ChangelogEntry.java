package com.codingcat.changelogs.data;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public record ChangelogEntry(
        int uid,
        @NotNull List<Component> lines,
        @NotNull Instant recordedAt,
        @Nullable Component author
) {
}
