package com.codingcat.changelogs.fabric.meta;

import com.codingcat.changelogs.fabric.FabricChangelogsPlatform;
import com.codingcat.changelogs.platformapi.meta.ChangelogsMeta;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

public final class FabricChangelogsMeta implements ChangelogsMeta {
    private final @NotNull ModMetadata metadata = FabricLoader.getInstance()
            .getModContainer(FabricChangelogsPlatform.MOD_ID).orElseThrow().getMetadata();

    @Override
    public @NotNull String getName() {
        return this.metadata.getName();
    }

    @Override
    public @NotNull String getVersion() {
        return this.metadata.getVersion().getFriendlyString();
    }

    @Override
    public @Nullable String getDescription() {
        return this.metadata.getDescription();
    }

    @Override
    public @Nullable Set<String> getAuthors() {
        Set<String> authors = this.metadata.getAuthors().stream().map(Person::getName).collect(Collectors.toSet());
        return authors.isEmpty() ? null : authors;
    }
}
