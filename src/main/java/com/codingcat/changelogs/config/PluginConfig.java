package com.codingcat.changelogs.config;

import com.codingcat.changelogs.ServerChangelogs;
import com.codingcat.changelogs.data.ChangelogStorage;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

@RequiredArgsConstructor
public class PluginConfig extends YamlConfiguration {
    private final @NotNull ServerChangelogs plugin;
    private final @NotNull Path path;

    public void tryReload() {
        try {
            this.reload();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException("Failed to reload configuration", e);
        }
        String err = this.validate();
        if (err != null) throw new RuntimeException("Configuration invalid: " + err);
    }

    public void reload() throws IOException, InvalidConfigurationException {
        this.load(this.path.toFile());
    }

    public @Nullable String validate() {
        try {
            createChangelogStorage();
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public @NotNull ChangelogStorage createChangelogStorage() throws RuntimeException {
        return ChangelogStorage.create(getString("changelog_storage", "yaml"), this.plugin);
    }
}
