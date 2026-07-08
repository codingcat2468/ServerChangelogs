package com.codingcat.changelogs.data;

import com.codingcat.changelogs.ServerChangelogs;
import com.codingcat.changelogs.data.storage.YamlChangelogStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ChangelogStorage {
    void init();

    void shutdown();

    void storeEntry(@NotNull ChangelogEntry entry);

    boolean removeEntry(int uid);

    @NotNull List<ChangelogEntry> listEntries();

    int allocateUID();

    @NotNull String getDisplayName();

    static @NotNull ChangelogStorage create(@NotNull String identifier, @NotNull ServerChangelogs plugin) throws IllegalArgumentException {
        if (!identifier.equals("yaml"))
            throw new IllegalArgumentException("Unknown changelog storage type \"" + identifier + "\"");
        return new YamlChangelogStorage(plugin.getDataPath().resolve("_data.yml"));
    }
}
