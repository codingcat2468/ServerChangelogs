package com.codingcat.changelogs.paper.meta;

import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta;
import io.papermc.paper.plugin.configuration.PluginMeta;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public final class PaperPluginChangelogsMeta implements ChangelogsMeta {
    private final @NotNull PluginMeta pluginMeta;

    @Override
    public @NotNull String getName() {
        return this.pluginMeta.getName();
    }

    @Override
    public @NotNull String getVersion() {
        return this.pluginMeta.getVersion();
    }

    @Override
    public @Nullable String getDescription() {
        return this.pluginMeta.getDescription();
    }

    @Override
    public @Nullable Set<String> getAuthors() {
        if (this.pluginMeta.getAuthors().isEmpty()) return null;
        return new HashSet<>(this.pluginMeta.getAuthors());
    }
}
